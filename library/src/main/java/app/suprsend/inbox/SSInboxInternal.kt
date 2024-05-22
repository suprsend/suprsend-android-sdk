package app.suprsend.inbox

import androidx.core.content.ContextCompat
import app.suprsend.base.Logger
import app.suprsend.base.SdkAndroidCreator
import app.suprsend.base.safeString
import app.suprsend.inbox.model.InboxData
import app.suprsend.inbox.model.InboxStoreListener
import app.suprsend.inbox.model.NotificationModel
import app.suprsend.inbox.model.NotificationStore
import app.suprsend.inbox.model.hasMultipleStore
import org.json.JSONObject

internal class SSInboxInternal {

    private val inboxRepository = InboxRepository()
    var inboxData = InboxData()
    private val inboxStoreListeners: MutableList<InboxStoreListener> = mutableListOf()

    fun load(storeId: String?) {
        val sid = storeId ?: SSInbox.DEFAULT_STORE_ID
        val notificationStore =
            inboxData.notificationStores.find { it.notificationStoreConfig.storeId == sid }
        if (notificationStore == null) {
            Logger.i(SSInbox.LOGGING_TAG, "Notification Store not found storeId:$sid")
            return
        }
        if (notificationStore.hasInitialFetchTime()) {
            loadNextPage(
                notificationStore
            )
        } else {
            loadInitial(
                notificationStore
            )
        }
    }

    fun reload(storeId: String? = null) {
        if (storeId == null) {
            inboxData.notificationStores.forEach { store ->
                store.reset()
                loadInitial(store, true)
            }
        } else {
            val store =
                inboxData.notificationStores.find { it.notificationStoreConfig.storeId == storeId }
                    ?: return
            store.reset()
            loadInitial(store, true)
        }
    }

    private fun loadInitial(
        notificationStore: NotificationStore,
        notifyAfterCompletion: Boolean = false
    ) {
        try {
            if (notificationStore.hasInitialFetchTime()) {
                Logger.i(
                    SSInbox.LOGGING_TAG,
                    "Initial page for store:${notificationStore.notificationStoreConfig.storeId} is already loaded"
                )
                return
            }

            inboxRepository.fetchAndUpdateNotificationCount(
                inboxData = inboxData
            )

            loadNextPage(notificationStore, notifyAfterCompletion)

        } catch (e: Exception) {
            Logger.e(SSInbox.LOGGING_TAG, e)
        }
    }

    private fun loadNextPage(
        notificationStore: NotificationStore,
        notifyAfterCompletion: Boolean = false
    ) {
        try {
            if (notificationStore.hasInitialFetchTime() && notificationStore.currentPageNumber >= notificationStore.totalPages) {
                return
            }
            if (notificationStore.isLoading) {
                Logger.i(
                    SSInbox.LOGGING_TAG,
                    "Loading for store id ${notificationStore.notificationStoreConfig.storeId} is already in-progress ignoring this duplicate call on same store"
                )
                return
            }
            notificationStore.isLoading = true
            if (!notifyAfterCompletion)
                notifyLoading(
                    notificationStore.notificationStoreConfig.storeId,
                    notificationStore.isLoading
                )
            val fetchTime = if (notificationStore.hasInitialFetchTime())
                notificationStore.initialFetchTime else {
                Logger.i(SSInbox.LOGGING_TAG, "Using new fetch time")
                System.currentTimeMillis()
            }

            val notificationListModel = inboxRepository.fetchNotificationListAndConvertToModels(
                subscriberId = inboxData.subscriberId,
                distinctId = inboxData.distinctId,
                tenantId = inboxData.tenantId,
                pageNumber = notificationStore.currentPageNumber + 1,
                pageSize = inboxData.pageSize,
                before = fetchTime,
                notificationStore = notificationStore
            )
            if (notificationListModel != null) {
                val meta = notificationListModel.meta
                val newMessages = notificationListModel.results
                //To clear out socket notifications
                if (!notificationStore.hasInitialFetchTime()) {
                    notificationStore.notifications.clear()
                    notificationStore.initialFetchTime = fetchTime
                }
                if (newMessages.isNotEmpty()) {
                    notificationStore.notifications.addAll(newMessages)
                    notificationStore.totalPages = meta.totalPages
                    notificationStore.currentPageNumber = meta.currentPage
                }

                notificationStore.isLoading = false
                notifyLoading(
                    storeId = notificationStore.notificationStoreConfig.storeId,
                    isLoading = notificationStore.isLoading
                )
                notifyListeners(
                    storeId = notificationStore.notificationStoreConfig.storeId,
                    allNotifications = notificationStore.notifications
                )
            } else {
                notificationStore.isLoading = false
                notifyLoading(notificationStore.notificationStoreConfig.storeId, false)
                notifyError(
                    notificationStore.notificationStoreConfig.storeId,
                    IllegalStateException("Failed to fetch ")
                )
            }
        } catch (e: Exception) {
            Logger.e(SSInbox.LOGGING_TAG, e)
        }
    }

    fun markBellClicked() {
        val success = inboxRepository.hitApi(
            requestURI = "/bell-clicked/",
            distinctId = inboxData.distinctId,
            subscriberId = inboxData.subscriberId,
            tenantId = inboxData.tenantId
        )
        if (success) {
            inboxData.bellCount = 0
            notifyBellCount(inboxData.bellCount)
        }
    }

    fun markAllNotificationRead() {
        val success = inboxRepository.hitApi(
            requestURI = "/mark-all-read/",
            distinctId = inboxData.distinctId,
            subscriberId = inboxData.subscriberId,
            tenantId = inboxData.tenantId
        )
        val clickedOn = System.currentTimeMillis()
        if (success) {
            inboxData
                .notificationStores
                .forEach { store ->
                    store.notifications.forEach { notification ->
                        if (notification.seenOn == null) {
                            notification.seenOn = clickedOn
                        }
                    }
                    store.unseenCount = 0
                }
        }
    }

    fun markNotificationClicked(storeId: String, notificationId: String) {
        val notificationStore = inboxData.notificationStores.find {
            it.notificationStoreConfig.storeId == inboxData.safeStoreId(storeId)
        }
        if (notificationStore == null) {
            Logger.i(SSInbox.LOGGING_TAG, "Notification store not found storeId:$storeId")
            return
        }
        val notification = notificationStore.notifications.find { it.id == notificationId }
        if (notification == null) {
            Logger.i(SSInbox.LOGGING_TAG, "Notification not found notificationId:$notificationId")
            return
        }

        if (notification.interactedOn == null || notification.seenOn == null) {
            if (notification.interactedOn == null) {
                inboxRepository.markNotificationClicked(
                    notificationId = notificationId
                )
            } else {
                inboxRepository.markNotificationReadStatus(
                    notificationId = notificationId,
                    distinctId = inboxData.distinctId,
                    subscriberId = inboxData.subscriberId,
                    read = true
                )
            }
            val clickedOn = System.currentTimeMillis()
            notification.interactedOn = clickedOn
            notification.seenOn = clickedOn
            if (notificationStore.unseenCount > 0)
                notificationStore.unseenCount -= 1
        }

    }

    fun markNotificationRead(storeId: String, notificationId: String) {
        val notificationStore = inboxData.notificationStores.find {
            it.notificationStoreConfig.storeId == inboxData.safeStoreId(storeId)
        }
        if (notificationStore == null) {
            Logger.i(SSInbox.LOGGING_TAG, "Notification store not found storeId:$storeId")
            return
        }
        val notification = notificationStore.notifications.find { it.id == notificationId }
        if (notification == null) {
            Logger.i(SSInbox.LOGGING_TAG, "Notification not found notificationId:$notificationId")
            return
        }
        if (notification.seenOn != null) {
            return
        }
        val success = inboxRepository.markNotificationReadStatus(
            notificationId = notificationId,
            distinctId = inboxData.distinctId,
            subscriberId = inboxData.subscriberId,
            read = true
        )
        if (success) {
            notification.seenOn = System.currentTimeMillis()
            if (notificationStore.unseenCount > 0)
                notificationStore.unseenCount -= 1
            notifyListeners(
                notificationStore.notificationStoreConfig.storeId,
                notificationStore.notifications
            )
        }
    }

    fun markNotificationArchive(storeId: String, notificationId: String) {
        val notificationStore = inboxData.notificationStores.find {
            it.notificationStoreConfig.storeId == inboxData.safeStoreId(storeId)
        }
        if (notificationStore == null) {
            Logger.i(SSInbox.LOGGING_TAG, "Notification store not found storeId:$storeId")
            return
        }
        val notification = notificationStore.notifications.find { it.id == notificationId }
        if (notification == null) {
            Logger.i(SSInbox.LOGGING_TAG, "Notification not found notificationId:$notificationId")
            return
        }

        inboxRepository.markNotificationArchive(
            notificationId = notificationId,
            distinctId = inboxData.distinctId,
            subscriberId = inboxData.subscriberId
        )

    }

    fun markNotificationUnRead(storeId: String, notificationId: String) {
        val notificationStore = inboxData.notificationStores.find {
            it.notificationStoreConfig.storeId == inboxData.safeStoreId(storeId)
        }
        if (notificationStore == null) {
            Logger.i(SSInbox.LOGGING_TAG, "Notification store not found storeId:$storeId")
            return
        }
        val notification = notificationStore.notifications.find { it.id == notificationId }
        if (notification == null) {
            Logger.i(SSInbox.LOGGING_TAG, "Notification not found notificationId:$notificationId")
            return
        }
        if (notification.seenOn == null) {
            return
        }
        val success = inboxRepository.markNotificationReadStatus(
            notificationId = notificationId,
            distinctId = inboxData.distinctId,
            subscriberId = inboxData.subscriberId,
            read = false
        )
        if (success) {
            notification.seenOn = null
            notificationStore.unseenCount += 1
            notifyListeners(
                notificationStore.notificationStoreConfig.storeId,
                notificationStore.notifications
            )
        }
    }

    fun addListener(inboxStoreListener: InboxStoreListener) {
        if (!inboxStoreListeners.contains(inboxStoreListener)) {
            inboxStoreListeners.add(inboxStoreListener)
        }
    }

//    fun getNotificationItems(storeId: String): List<NotificationModel>? {
//        val notificationStore = inboxData.notificationStores.find { it.notificationStoreConfig.storeId == storeId }
//        if (notificationStore == null) {
//            Logger.i(SSInbox.LOGGING_TAG, "Store not found storeId:$storeId")
//            return null
//        }
//        return notificationStore.notifications
//    }


    private fun addNewNotification(
        newNotification: NotificationModel,
        oldNotifications: List<NotificationModel>
    ): MutableList<NotificationModel> {
        val notifications = mutableListOf<NotificationModel>()

        if (newNotification.isPinned) {
            notifications.add(newNotification)
            notifications.addAll(oldNotifications)
        } else {
            var addedNotification = false
            for (notification in oldNotifications) {
                if (notification.isPinned) {
                    notifications.add(notification)
                } else {
                    if (!addedNotification) {
                        notifications.add(newNotification)
                        addedNotification = true
                    }
                    notifications.add(notification)
                }
            }
            if (!addedNotification) {
                notifications.add(newNotification)
            }
        }
        return notifications
    }

    private fun notificationBelongToStore(
        notificationModel: NotificationModel,
        store: NotificationStore
    ): Boolean {
        val notificationRead = notificationModel.seenOn != null
        val notificationTags: List<String> = notificationModel.tags ?: emptyList()
        val notificationCategory: String = notificationModel.category
        val notificationArchive: Boolean = notificationModel.archived

        val config = store.notificationStoreConfig
        val storeRead = config.query.read
        val storeTags = config.query.tags
        val storeCategories = config.query.categories
        val storeArchived = config.query.archived

        val sameRead = storeRead == null || notificationRead == storeRead
        val sameArchived = storeArchived == null || (notificationArchive == storeArchived)
        var sameTags = false
        var sameCategory = false

        if (storeTags.isNullOrEmpty()) {
            sameTags = true
        } else if (storeTags.isNotEmpty()) {
            for (tag in storeTags) {
                if (notificationTags.contains(tag)) {
                    sameTags = true
                    break
                }
            }
        }

        if (storeCategories.isNullOrEmpty()) {
            sameCategory = true
        } else if (storeCategories.isNotEmpty()) {
            sameCategory = storeCategories.contains(notificationCategory)
        }

        return sameRead && sameTags && sameCategory && sameArchived
    }

    private fun notifyError(storeId: String, e: Exception) {
        ContextCompat.getMainExecutor(SdkAndroidCreator.context).execute {
            inboxStoreListeners.forEach {
                it.onError(storeId, e)
            }
        }
    }

    private fun notifyBellCount(bellCount: Int) {
        ContextCompat.getMainExecutor(SdkAndroidCreator.context).execute {
            inboxStoreListeners.forEach {
                it.bellCount(bellCount)
            }
        }
    }

    private fun notifyNewNotification(notificationModel: NotificationModel) {
        ContextCompat.getMainExecutor(SdkAndroidCreator.context).execute {
            inboxStoreListeners.forEach {
                it.newNotification(notificationModel)
            }
        }
    }

    private fun notifyLoading(storeId: String, isLoading: Boolean) {
        ContextCompat.getMainExecutor(SdkAndroidCreator.context).execute {
            inboxStoreListeners.forEach {
                it.loading(storeId, isLoading)
            }
        }
    }

    fun notifySocketStatus(connectionState: ConnectionState) {
        ContextCompat.getMainExecutor(SdkAndroidCreator.context).execute {
            inboxStoreListeners.forEach {
                it.socket(connectionState)
            }
        }
    }

    private fun notifyListeners(
        storeId: String,
        allNotifications: List<NotificationModel>
    ) {
        ContextCompat.getMainExecutor(SdkAndroidCreator.context).execute {
            inboxStoreListeners.forEach {
                it.onUpdate(
                    storeId = storeId,
                    allNotifications = allNotifications
                )
            }
        }
    }

    fun onSocketNotificationUpdate(data: Array<Any>?) {
        val payloadJO = data?.getOrNull(0) as JSONObject? ?: JSONObject()
        if (payloadJO.getString("n_id").isNullOrEmpty()) return
        Thread.sleep(1000)
        val newNotificationModel = inboxRepository.getNotificationDetails(
            notificationId = payloadJO.safeString("n_id") ?: "",
            subscriberId = inboxData.subscriberId,
            distinctId = inboxData.distinctId,
            tenantId = inboxData.tenantId
        ) ?: return

        inboxData.notificationStores.forEach { store ->
            val oldNotification = store.notifications.find { it.id == newNotificationModel.id }
            val index = store.notifications.indexOf(oldNotification)
            val nowItBelongsToCurrentStore = notificationBelongToStore(newNotificationModel, store)
            if (oldNotification == null && nowItBelongsToCurrentStore) {
                val newNotifications = addNewNotification(
                    newNotificationModel,
                    store.notifications
                )
                store.notifications.clear()
                store.notifications.addAll(newNotifications)
                notifyListeners(
                    store.notificationStoreConfig.storeId,
                    store.notifications
                )
                Logger.i(
                    SSInbox.LOGGING_TAG,
                    "Updating store - ${store.notificationStoreConfig.storeId}\n" +
                            "Now notification belong this store"
                )
            } else if (oldNotification != null) {
                //Due to change in see_on it may be possible now notification does not belong to current store
                if (!nowItBelongsToCurrentStore) {
                    Logger.i(
                        SSInbox.LOGGING_TAG,
                        "Updating store - ${store.notificationStoreConfig.storeId}\n" +
                                "Now notification do not belong this store"
                    )
                    store.notifications.remove(oldNotification)
                    if (store.notifications.isEmpty()) {
                        store.reset()
                    }
                } else {
                    Logger.i(
                        SSInbox.LOGGING_TAG, "" +
                                "Updating store - ${store.notificationStoreConfig.storeId}\n" +
                                "index - $index\n" +
                                "id - ${newNotificationModel.id}\n" +
                                "old - seenon ${oldNotification.seenOn}\n" +
                                "new - seenon ${newNotificationModel.seenOn}"+
                                "old - archived ${oldNotification.archived}\n" +
                                "new - archived ${newNotificationModel.archived}"
                    )
                    store.notifications.remove(oldNotification)
                    store.notifications.add(index, newNotificationModel.copy())
                }
                notifyListeners(
                    store.notificationStoreConfig.storeId,
                    store.notifications
                )
            }
        }
        inboxRepository.fetchAndUpdateNotificationCount(
            inboxData
        )
        notifyBellCount(inboxData.bellCount)
    }

    fun onSocketNewNotification(data: Array<Any>?) {
        val payloadJO = data?.getOrNull(0) as JSONObject? ?: JSONObject()
        if (payloadJO.getString("n_id").isNullOrEmpty()) return
        Thread.sleep(1000)
        val notificationModel = inboxRepository.getNotificationDetails(
            notificationId = payloadJO.safeString("n_id") ?: "",
            subscriberId = inboxData.subscriberId,
            distinctId = inboxData.distinctId,
            tenantId = inboxData.tenantId
        )
        if (notificationModel != null) {
            var updateOverallCount = false
            inboxData
                .notificationStores
                .forEach { store ->
                    if (notificationBelongToStore(notificationModel, store)) {

                        val newNotifications = addNewNotification(
                            notificationModel,
                            store.notifications
                        )
                        store.notifications.clear()
                        store.notifications.addAll(newNotifications)

                        if (!updateOverallCount) {
                            inboxData.bellCount += 1
                            updateOverallCount = true
                        }
                        notifyListeners(
                            store.notificationStoreConfig.storeId,
                            store.notifications
                        )
                    }
                }

        }
        inboxRepository.fetchAndUpdateNotificationCount(inboxData)
        notifyBellCount(inboxData.bellCount)
        if (notificationModel != null) {
            notifyNewNotification(notificationModel)
        }
    }

    fun onSocketUpdateBadge() {
        inboxData.bellCount = 0
        notifyBellCount(inboxData.bellCount)
    }

    fun onSocketMarkAllRead() {
        val clickedOn = System.currentTimeMillis()
        inboxData
            .notificationStores.forEach { store ->
                store.notifications.forEach { notification ->
                    notification.seenOn = clickedOn
                }
                store.unseenCount = 0
                notifyListeners(store.notificationStoreConfig.storeId, store.notifications)
            }
    }

    fun checkExpiredMessages() {
        Logger.i(SSInbox.LOGGING_TAG, "Checking expiry messages")
        val now = System.currentTimeMillis()
        val expiredIds = arrayListOf<String>()
        inboxData
            .notificationStores
            .forEach { store ->
                var hasExpired = false
                val validNotifications = store.notifications.filter { notification ->
                    val expiry = notification.expiry
                    if (expiry != null && now > expiry) {
                        expiredIds.add(notification.id)
                        hasExpired = true
                        false
                    } else true
                }
                if (hasExpired) {
                    store.notifications.clear()
                    store.notifications.addAll(validNotifications)
                    notifyListeners(
                        store.notificationStoreConfig.storeId,
                        store.notifications
                    )
                }
            }
        if (expiredIds.isNotEmpty()) {
            Logger.i(SSInbox.LOGGING_TAG, "Expired message ids : $expiredIds")
            inboxRepository.fetchAndUpdateNotificationCount(inboxData)
            notifyBellCount(inboxData.bellCount)
        }
    }

    private fun InboxData.safeStoreId(storeId: String): String {
        return if (notificationStores.hasMultipleStore())
            storeId
        else
            SSInbox.DEFAULT_STORE_ID
    }


}