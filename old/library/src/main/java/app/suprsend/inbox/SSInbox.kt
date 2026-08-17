package app.suprsend.inbox

import app.suprsend.SSApiInternal
import app.suprsend.base.Logger
import app.suprsend.base.PeriodicJob
import app.suprsend.base.inboxExecutorService
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
private constructor(
    subscriberId: String,
    distinctId: String,
    tenantId: String? = null,
    pageSize: Int? = null,
    notificationStoreConfigs: List<NotificationStoreConfig> = listOf()
) {

    private var ssInboxInternal = SSInboxInternal()
    private var periodicJob: PeriodicJob? = null
    private var socketConnectionState: ConnectionState = ConnectionState.CLOSED

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
                periodInSec = 20,
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
        inboxExecutorService.execute {
            ssInboxInternal.load(storeId)
        }
    }

    fun reload(storeId: String? = null) {
        inboxExecutorService.execute {
            ssInboxInternal.reload(storeId)
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
        inboxExecutorService.execute {
            ssInboxInternal.markBellClicked()
        }
    }

    fun markAllNotificationRead() {
        inboxExecutorService.execute {
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
        inboxExecutorService.execute {
            ssInboxInternal
                .markNotificationRead(
                    storeId = storeId,
                    notificationId = notificationId
                )
        }
    }

    fun markNotificationArchive(storeId: String, notificationId: String) {
        inboxExecutorService.execute {
            ssInboxInternal
                .markNotificationArchive(
                    storeId = storeId,
                    notificationId = notificationId
                )
        }
    }

    fun markNotificationUnRead(storeId: String, notificationId: String) {
        inboxExecutorService
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

    fun findStore(storeId: String? = null): NotificationStore? {
        return getData().findStore(storeId)
    }

    fun getData(): InboxData {
        return ssInboxInternal.inboxData
    }

    fun connect() {
        inboxExecutorService.execute {
            Logger.i(LOGGING_TAG, "Socket Connect Requested")
            if (socketConnectionState == ConnectionState.FAILURE || socketConnectionState == ConnectionState.CLOSED) {
                socket.connect()
                if (!periodicJob?.isScheduled.isTrue()) {
                    periodicJob?.start()
                }
                ssInboxInternal.reload()
            } else {
                Logger.i(LOGGING_TAG, "Socket connect ignored")
            }
        }
    }

    fun disconnect() {
        inboxExecutorService.execute {
            Logger.i(LOGGING_TAG, "Socket Disconnect Requested")
            if (socket.connected())
                socket.disconnect()
            if (periodicJob?.isScheduled.isTrue())
                periodicJob?.stop()
        }
    }

    private fun subscribeSocketListeners() {
        socketConnectionState = ConnectionState.CONNECTING
        subscribeNewNotification()
        subscribeUpdatedNotification()
        subscribeUpdateBadge()
        subscribeMarkAllRead()
        socket.on(Socket.EVENT_CONNECT) {
            Logger.i(LOGGING_TAG, "Socket connected")
            socketConnectionState = ConnectionState.OPENED
            ssInboxInternal.notifySocketStatus(socketConnectionState)
        }
        socket.on(Socket.EVENT_DISCONNECT) {
            socketConnectionState = ConnectionState.CLOSED
            Logger.i(LOGGING_TAG, "Socket disconnected")
            ssInboxInternal.notifySocketStatus(socketConnectionState)
        }
        socket.on(Socket.EVENT_CONNECT_ERROR) {
            socketConnectionState = ConnectionState.FAILURE
            Logger.i(LOGGING_TAG, "Socket connect error")
            ssInboxInternal.notifySocketStatus(socketConnectionState)
        }
        socket.connect()
    }

    private fun subscribeMarkAllRead() {
        socket.on("mark_all_read") {
            inboxExecutorService.execute {
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
            inboxExecutorService.execute {
                Logger.i(LOGGING_TAG, "notification_updated")
                ssInboxInternal.onSocketNotificationUpdate(data)
            }
        }
    }

    private fun subscribeNewNotification() {
        socket.on("new_notification") { data ->
            inboxExecutorService.execute {
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
        private var instance: SSInbox? = null

        fun initialize(
            subscriberId: String,
            distinctId: String,
            tenantId: String? = null,
            pageSize: Int? = null,
            notificationStoreConfigs: List<NotificationStoreConfig> = listOf()
        ): SSInbox {
            var safeInstance = instance
            if (safeInstance == null) {
                safeInstance = SSInbox(subscriberId, distinctId, tenantId, pageSize, notificationStoreConfigs)
                instance = safeInstance
            }
            return safeInstance
        }

        fun getInstance(): SSInbox? {
            return instance
        }

        fun clear() {
            instance = null
        }
    }


}