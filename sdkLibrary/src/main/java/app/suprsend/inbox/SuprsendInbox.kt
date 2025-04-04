package app.suprsend.inbox

import app.suprsend.base.Response
import app.suprsend.base.inboxExecutorService
import app.suprsend.inbox.InboxStore.Companion.DEFAULT_STORE
import app.suprsend.inbox.socket.ConnectionState
import app.suprsend.inbox.socket.SSInboxSocket

class SuprsendInbox
private constructor() {

    fun getBellCount(): Int {
        return SSInboxInternal.getBellCount()
    }

    fun fetchBellCount(): Response<Int> {
        return SSInboxInternal.fetchAndNotifyNotificationsCount()
    }

    fun fetchBellCountAsync(callBack: ((response: Response<Int>) -> Unit)? = null) {
        inboxExecutorService.execute {
            val response = fetchBellCount()
            callBack?.invoke(response)
        }
    }

    fun resetBellCount(): Response<Boolean> {
        return SSInboxInternal.resetBellCount()
    }

    fun resetBellCountAsync(callBack: ((response: Response<Boolean>) -> Unit)? = null) {
        inboxExecutorService.execute {
            val response = resetBellCount()
            callBack?.invoke(response)
        }
    }

    fun getStore(storeId: String? = null): InboxStore {
        return SSInboxInternal.getStore(storeId)
    }

    fun getStores(): List<InboxStore> {
        return SSInboxInternal.getStores()
    }

    fun getNotificationDetails(notificationId: String, storeId: String? = null): Response<InboxNotification> {
        val notification = SSInboxInternal.getNotificationDetails(notificationId, if (storeId == DEFAULT_STORE) null else getStore(storeId).toJSONObject())
        return notification
    }

    fun getStoreCount(): Int {
        return SSInboxInternal.getStoreCount()
    }

    fun markAllRead(): Response<Boolean> {
        return SSInboxInternal.markAllRead()
    }

    fun markAllReadAsync(callBack: ((response: Response<Boolean>) -> Unit)? = null) {
        inboxExecutorService.execute {
            val response = SSInboxInternal.markAllRead()
            callBack?.invoke(response)
        }
    }

    fun markAsInteracted(notificationId: String): Response<Boolean> {
        return SSInboxInternal.markAsInteracted(notificationId)
    }

    fun markAsInteractedAsync(notificationId: String, callBack: ((response: Response<Boolean>) -> Unit)? = null) {
        inboxExecutorService.execute {
            val response = SSInboxInternal.markAsInteracted(notificationId)
            callBack?.invoke(response)
        }
    }

    fun markAsUnread(notificationId: String): Response<Boolean> {
        return SSInboxInternal.markAsUnread(notificationId)
    }

    fun markAsUnreadAsync(notificationId: String, callBack: ((response: Response<Boolean>) -> Unit)? = null) {
        inboxExecutorService.execute {
            val response = SSInboxInternal.markAsUnread(notificationId)
            callBack?.invoke(response)
        }
    }

    fun markAsRead(notificationId: String): Response<Boolean> {
        return SSInboxInternal.markAsRead(notificationId)
    }

    fun markAsReadAsync(notificationId: String, callBack: ((response: Response<Boolean>) -> Unit)? = null) {
        inboxExecutorService.execute {
            val response = SSInboxInternal.markAsRead(notificationId)
            callBack?.invoke(response)
        }
    }

    fun markAsArchived(notificationId: String): Response<Boolean> {
        return SSInboxInternal.markAsArchived(notificationId)
    }

    fun markAsArchivedAsync(notificationId: String, callBack: ((response: Response<Boolean>) -> Unit)? = null) {
        inboxExecutorService.execute {
            val response = SSInboxInternal.markAsArchived(notificationId)
            callBack?.invoke(response)
        }
    }

    fun markAsSeen(notificationId: String): Response<Boolean> {
        return markAsSeen(listOf(notificationId))
    }

    fun markAsSeenAsync(notificationId: String, callBack: ((response: Response<Boolean>) -> Unit)? = null) {
        inboxExecutorService.execute {
            val response = markAsSeen(listOf(notificationId))
            callBack?.invoke(response)
        }
    }

    fun markAsSeen(notificationIds: List<String>): Response<Boolean> {
        return SSInboxInternal.markAsSeen(notificationIds = notificationIds)
    }

    fun markAsSeenAsync(notificationIds: List<String>, callBack: ((response: Response<Boolean>) -> Unit)? = null) {
        inboxExecutorService.execute {
            val response = SSInboxInternal.markAsSeen(notificationIds = notificationIds)
            callBack?.invoke(response)
        }
    }

    fun getSocketConnectionState(): ConnectionState {
        return SSInboxSocket.getSocketConnectionState()
    }

    fun openConnection() {
        SSInboxSocket.connect()
        SSInboxExpiredMessages.start()
    }

    fun closeConnection() {
        SSInboxInternal.reset()
    }

    fun registerCallback(inboxStoreListener: InboxStoreListener) {
        SSInboxInternal.registerCallback(inboxStoreListener)
    }

    fun unRegisterCallback(inboxStoreListener: InboxStoreListener) {
        SSInboxInternal.unRegisterCallback(inboxStoreListener)
    }

    companion object {
        fun setBaseUrl(baseUrl: String) {
            SSInboxInternal.setBaseUrl(baseUrl)
        }

        fun setInboxSocketUrl(socketBaseUrl: String) {
            SSInboxInternal.setInboxSocketUrl(socketBaseUrl)
        }

        fun setSubscriberId(subscriberId: String) {
            SSInboxInternal.setSubscriberId(subscriberId)
        }

        fun setTenantId(tenantId: String?) {
            SSInboxInternal.setTenantId(tenantId)
        }

        @Volatile
        private var instance: SuprsendInbox? = null
        fun getInstance(): SuprsendInbox {
            if (SSInboxInternal.inboxData.subscriberId.isNullOrBlank()) {
                throw IllegalStateException("Subscriber id is missing. Please set using SuprsendInbox.setSubscriberId()")
            }
            if (SSInboxInternal.inboxData.baseUrl.isBlank()) {
                throw IllegalStateException("Inbox feature is not initialized. Please use SuprsendInbox.init() to initialize.")
            }
            return instance ?: synchronized(this) {
                instance ?: SuprsendInbox().also { instance = it }
            }
        }

        fun setInboxStores(inboxStoreList: List<InboxStore>?) {
            SSInboxInternal.setInboxStores(inboxStoreList)
        }

//        fun setInboxThemeConfig(inboxThemeConfig: InboxThemeConfig) {
//            SSInboxInternal.setInboxThemeConfig(inboxThemeConfig)
//        }

    }
}