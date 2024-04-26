package app.suprsend.inbox

import app.suprsend.SSApiInternal
import app.suprsend.base.Logger
import app.suprsend.base.PeriodicJob
import app.suprsend.base.executorService
import app.suprsend.base.isTrue
import app.suprsend.inbox.model.InboxData
import app.suprsend.inbox.model.InboxStoreListener
import app.suprsend.inbox.model.NotificationStore
import app.suprsend.inbox.model.NotificationStoreConfig
import app.suprsend.inbox.model.NotificationStoreQuery
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import java.util.UUID


class SSInbox
constructor(
    subscriberId: String,
    distinctId: String,
    tenantId: String? = null,
    pageSize: Int? = null,
    notificationStoreConfigs: List<NotificationStoreConfig> = listOf()
) {

    private var ssInboxInternal = SSInboxInternal()
    private var periodicJob: PeriodicJob? = null

    private val socket: Socket by lazy {
        val options = IO.Options().apply {
            transports = arrayOf(WebSocket.NAME)
            auth = mapOf(
                "subscriber_id" to ssInboxInternal.inboxData.subscriberId,
                "distinct_id" to ssInboxInternal.inboxData.distinctId,
                "authorization" to "${SSApiInternal.getCachedApiKey()}:${UUID.randomUUID()}",
                "tenant_id" to ssInboxInternal.inboxData.tenantId
            )
            reconnectionAttempts = 25
            reconnectionDelay = 5000
            reconnectionDelayMax = 10000
        }
        IO.socket(SSApiInternal.getInboxSocketUrl(), options)
    }

    init {
        try {
            ssInboxInternal.inboxData.subscriberId = subscriberId
            ssInboxInternal.inboxData.distinctId = distinctId
            ssInboxInternal.inboxData.pageSize = pageSize ?: DEFAULT_PAGINATION_LIMIT
            validateTenantId(tenantId)
            validateStore(notificationStoreConfigs)
            subscribeSocketListeners()
            periodicJob = PeriodicJob(
                periodInSec = 30,
                jobName = "InboxExpiryMessages"
            ) {
                ssInboxInternal.checkExpiredMessages()
            }
            periodicJob?.start()
        } catch (e: Exception) {
            Logger.e(LOGGING_TAG, e)
        }
    }


    fun load(storeId: String? = null) {
        executorService.execute {
            val sid = storeId ?: DEFAULT_STORE_ID
            val notificationStore = ssInboxInternal.inboxData.notificationStores.find { it.notificationStoreConfig.storeId == sid }
            if (notificationStore == null) {
                Logger.i(LOGGING_TAG, "Notification Store not found storeId:$sid")
                return@execute
            }
            if (notificationStore.hasInitialFetchTime()) {
                ssInboxInternal.loadNextPage(
                    notificationStore
                )
            } else {
                ssInboxInternal.loadInitial(
                    notificationStore
                )
            }
        }
    }

//    /**
//     * If store id is not passed it will by default load the first store notifications
//     */
//    private fun loadInitial(
//        notificationStore: NotificationStore
//    ) {
//
//    }

//    private fun loadNextPage(
//        notificationStore: NotificationStore
//    ) {
//
//    }

    fun markBellClicked() {
        executorService.execute {
            ssInboxInternal.markBellClicked()
        }
    }

    fun markAllNotificationRead() {
        executorService.execute {
            ssInboxInternal.markAllNotificationRead()
        }
    }

    fun markNotificationClicked(storeId: String, notificationId: String) {
        ssInboxInternal.markNotificationClicked(
            storeId,
            notificationId
        )
    }

    fun markNotificationRead(storeId: String, notificationId: String) {
        executorService.execute {
            ssInboxInternal
                .markNotificationRead(
                    storeId = storeId,
                    notificationId = notificationId
                )
        }
    }

    fun markNotificationUnRead(storeId: String, notificationId: String) {
        executorService
            .execute {
                ssInboxInternal.markNotificationUnRead(
                    storeId = storeId,
                    notificationId = notificationId
                )
            }
    }

    fun addListener(inboxStoreListener: InboxStoreListener) {
        ssInboxInternal.addListener(inboxStoreListener)
    }

//    fun getNotificationItems(storeId: String): List<NotificationModel>? {
//        return ssInboxInternal.getNotificationItems(storeId)
//    }

    fun getData(): InboxData {
        return ssInboxInternal.inboxData
    }

    fun isSocketConnected(): Boolean {
        return socket.connected()
    }

    fun connect() {
        Logger.i(LOGGING_TAG,"Socket Connect Requested")
        if (!socket.connected())
            socket.connect()

        if (!periodicJob?.isScheduled.isTrue()) {
            periodicJob?.start()
        }
    }

    fun disconnect() {
        Logger.i(LOGGING_TAG,"Socket Disconnect Requested")
        if (socket.connected())
            socket.disconnect()
        if (periodicJob?.isScheduled.isTrue())
            periodicJob?.stop()
    }

    private fun subscribeSocketListeners() {
        subscribeNewNotification()
        subscribeUpdatedNotification()
        subscribeUpdateBadge()
        subscribeMarkAllRead()
        socket.on(Socket.EVENT_CONNECT) {
            Logger.i(LOGGING_TAG,"Socket connected")
            ssInboxInternal.notifySocketStatus(true)
        }
        socket.on(Socket.EVENT_DISCONNECT) {
            Logger.i(LOGGING_TAG,"Socket disconnected")
            ssInboxInternal.notifySocketStatus(false)
        }
        socket.connect()
    }

    private fun subscribeMarkAllRead() {
        socket.on("mark_all_read") {
            executorService.execute {
                Logger.i(LOGGING_TAG, "mark_all_read")
                ssInboxInternal.onSocketMarkAllRead()
            }
        }
    }

    private fun subscribeUpdateBadge() {
        socket.on("update_badge") {
            Logger.i(LOGGING_TAG, "update_badge")
            ssInboxInternal.onSocketUpdateBadge()
        }
    }

    private fun subscribeUpdatedNotification() {
        socket.on("notification_updated") { data ->
            executorService.execute {
                Logger.i(LOGGING_TAG, "notification_updated")
                ssInboxInternal.onSocketNotificationUpdate(data)
            }
        }
    }

    private fun subscribeNewNotification() {
        socket.on("new_notification") { data ->
            executorService.execute {
                Logger.i(LOGGING_TAG, "new_notification")
                ssInboxInternal.onSocketNewNotification(data)
            }
        }
    }

    private fun validateTenantId(tenantId: String?) {
        var finalTenantId = tenantId ?: ""
        if (finalTenantId.isBlank())
            finalTenantId = DEFAULT_TENANT_ID
        ssInboxInternal.inboxData.tenantId = finalTenantId
    }

    private fun validateStore(notificationStoreConfigs: List<NotificationStoreConfig>) {
        if (notificationStoreConfigs.isEmpty()) {
            ssInboxInternal.inboxData.notificationStores.add(
                NotificationStore(
                    notificationStoreConfig = NotificationStoreConfig(
                        storeId = DEFAULT_STORE_ID,
                        query = NotificationStoreQuery()
                    )
                )
            )
        } else {
            notificationStoreConfigs.forEach { config ->
                ssInboxInternal.inboxData.notificationStores.add(
                    NotificationStore(
                        notificationStoreConfig = config
                    )
                )
            }
        }
    }

    companion object {
        const val DEFAULT_PAGINATION_LIMIT = 20
        const val DEFAULT_MAX_PAGINATION_LIMIT = 100
        const val DEFAULT_MIN_PAGINATION_LIMIT = 1
        const val DEFAULT_TENANT_ID = "default"
        const val DEFAULT_STORE_ID = "default_store"
        const val LOGGING_TAG = "inbox"
    }
}