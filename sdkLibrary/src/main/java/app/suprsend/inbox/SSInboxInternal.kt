package app.suprsend.inbox

import androidx.core.content.ContextCompat
import app.suprsend.SSInternal
import app.suprsend.SuprSend
import app.suprsend.base.NetworkClient
import app.suprsend.base.NetworkInfo
import app.suprsend.base.Response
import app.suprsend.base.SSConstants
import app.suprsend.base.sdkExecutorService
import app.suprsend.exception.NoInternetException
import app.suprsend.inbox.socket.ConnectionState
import app.suprsend.inbox.socket.SSInboxSocket
import app.suprsend.log.Logger
import app.suprsend.utils.addTenantIdIfPresent
import app.suprsend.utils.convertToList
import app.suprsend.utils.safeString
import app.suprsend.utils.urlEncode
import org.json.JSONArray
import org.json.JSONObject

internal object SSInboxInternal {

    var networkClient = NetworkClient()

    var inboxData: InboxData = InboxData()

    private val inboxStoreListeners: MutableList<InboxStoreListener> = mutableListOf()

    fun setBaseUrl(baseUrl: String) {
        inboxData.baseUrl = baseUrl
    }

    fun setInboxSocketUrl(socketBaseUrl: String) {
        inboxData.socketUrl = socketBaseUrl
    }

    fun setSubscriberId(subscriberId: String) {
        inboxData.subscriberId = subscriberId
    }

    fun setTenantId(tenantId: String?) {
        inboxData.tenantId = tenantId
    }

    fun setInboxStores(inboxStoreList: List<InboxStore>?) {
        inboxData.storesMap = if (inboxStoreList.isNullOrEmpty()) mapOf(InboxStore.DEFAULT_STORE to InboxStore(storeId = InboxStore.DEFAULT_STORE)) else inboxStoreList.associateBy { it.storeId }
    }

    fun getBellCount(): Int {
        return inboxData.bellCount
    }

    /**
     * This fetches
     * - Bell count shown on notification badge
     * - If stores are present then also fetches unseen count of each store
     */
    fun fetchAndNotifyNotificationsCount(): Response<Int> {
        Thread.sleep(1000)
        if (!NetworkInfo.isConnected()) {
            return Response.Error(
                ex = NoInternetException(),
                message = "Internet connection is not available"
            )
        }

        val distinctId = urlEncode(SuprSend.getInstance().getDistinctId() ?: "")

        val operationStatus = SSInternal.refreshTokenIfRequired(distinctId = distinctId)

        if (!operationStatus.isSuccess()) {
            Logger.e(SSConstants.TAG_SUPRSEND, operationStatus.message ?: "No response", operationStatus.exception)
            return Response.Error(
                operationStatus.exception ?: IllegalStateException("Failed to refresh token while getting bell count"),
                operationStatus.message ?: "Failed to refresh token while getting bell count"
            )
        }

        val inboxBaseUrl = inboxData.baseUrl

        var url = "$inboxBaseUrl/v1/feed/notifications_count?distinct_id=$distinctId"
        url = url.addTenantIdIfPresent()
        val storeList = inboxData.storesMap
        if (!storeList.containsKey(InboxStore.DEFAULT_STORE)) {
            val jsonArray = JSONArray()
            storeList.forEach {
                jsonArray.put(it.value.toJSONObject())
            }
            url += "&stores=${urlEncode(jsonArray.toString())}"
        }

        val httpResponse = networkClient.httpCall(
            requestMethod = "GET",
            url = url,
            authorization = SSInternal.suprSendData.publicApiKey ?: "",
            headers = SSInternal.addSSSignature()
        )
        return if (httpResponse.isSuccess()) {
            val oldBelCountSignature = inboxData.getCountHash()
            val responseJo = JSONObject(httpResponse.body ?: "{}")
            val bellCount = responseJo.optInt("badge")

            //Updating store unseen count
            inboxData.storesMap.forEach { (id, store) ->
                if (responseJo.has(id)) {
                    val count = responseJo.getInt(id)
                    store.setUnseenCount(count)
                }
            }
            //Updating badge count
            inboxData.bellCount = bellCount

            if (oldBelCountSignature != inboxData.getCountHash()) {
                // Notifying the listeners
                notifyBellCount(bellCount)
            }
            Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Inbox : Received bell count : $bellCount")
            Response.Success(model = bellCount)
        } else {
            Response.Error(
                ex = IllegalStateException("Failed to get notifications count : ${httpResponse.statusCode}")
            )
        }
    }

    fun resetBellCount(): Response<Boolean> {
        if (!NetworkInfo.isConnected()) {
            return Response.Error(
                ex = NoInternetException(),
                message = "Internet connection is not available"
            )
        }

        val distinctId = urlEncode(SuprSend.getInstance().getDistinctId() ?: "")

        val operationStatus = SSInternal.refreshTokenIfRequired(distinctId = distinctId)

        if (!operationStatus.isSuccess()) {
            Logger.e(SSConstants.TAG_SUPRSEND, operationStatus.message ?: "No response", operationStatus.exception)
            return Response.Error(
                operationStatus.exception ?: IllegalStateException("Failed to refresh token while resetting bell count"),
                operationStatus.message ?: "Failed to refresh token while resetting bell count"
            )
        }

        val inboxBaseUrl = inboxData.baseUrl

        val url = "$inboxBaseUrl/v1/feed/reset_bell_count?distinct_id=$distinctId".addTenantIdIfPresent()

        val httpResponse = networkClient.httpCall(
            requestMethod = "PATCH",
            url = url,
            authorization = SSInternal.suprSendData.publicApiKey ?: "",
            headers = SSInternal.addSSSignature(
                mutableMapOf(
                    "content-type" to "application/json"
                )
            ),
            requestJson = "{}"
        )

        return if (httpResponse.isSuccess()) {
            Response.Success(true)
        } else {
            Response.Error(
                ex = IllegalStateException("Failed to reset bell count : ${httpResponse.statusCode}"),
                message = "Reset bell count failed ${httpResponse.statusCode}"
            )
        }
    }

    private fun getNotificationsRemote(
        before: Long,
        store: JSONObject? = null,
        pageSize: Int = 20,
        pageNo: Int = 1
    ): Response<InboxNotificationsResponse> {
        if (!NetworkInfo.isConnected()) {
            return Response.Error(
                ex = NoInternetException(),
                message = "Internet connection is not available"
            )
        }

        val encodedDistinctId = urlEncode(SuprSend.getInstance().getDistinctId() ?: "")

        val operationStatus = SSInternal.refreshTokenIfRequired(distinctId = encodedDistinctId)

        if (!operationStatus.isSuccess()) {
            Logger.e(SSConstants.TAG_SUPRSEND, operationStatus.message ?: "No response", operationStatus.exception)
            return Response.Error(
                operationStatus.exception ?: IllegalStateException("Failed to refresh token while getting notifications"),
                operationStatus.message ?: "Failed to refresh token while getting notifications"
            )
        }

        val inboxBaseUrl = inboxData.baseUrl

        var url = "$inboxBaseUrl/v1/feed/notifications?" +
                "distinct_id=$encodedDistinctId" +
                "&page_size=$pageSize" +
                "&page_no=$pageNo" +
                "&before=$before"

        if (store != null) {
            url += "&store=${urlEncode(store.toString())}"
        }

        url = url.addTenantIdIfPresent()

        val httpResponse = networkClient.httpCall(
            requestMethod = "GET",
            url = url,
            authorization = SSInternal.suprSendData.publicApiKey ?: "",
            headers = SSInternal.addSSSignature(
                mutableMapOf(
                    "content-type" to "application/json"
                )
            )
        )

        return if (httpResponse.isSuccess()) {
            val inboxNotificationsResponse = InboxNotificationsResponse.fromJson(httpResponse.body ?: "{}")
            Response.Success(inboxNotificationsResponse)
        } else {
            Response.Error(
                ex = IllegalStateException("Failed to get notifications : ${httpResponse.statusCode}"),
                body = httpResponse.body
            )
        }
    }

    fun getNotificationDetails(
        notificationId: String,
        store: JSONObject? = null
    ): Response<InboxNotification> {
        if (!NetworkInfo.isConnected()) {
            return Response.Error(
                ex = NoInternetException(),
                message = "Internet connection is not available"
            )
        }

        val encodedDistinctId = urlEncode(SuprSend.getInstance().getDistinctId() ?: "")

        val operationStatus = SSInternal.refreshTokenIfRequired(distinctId = encodedDistinctId)

        if (!operationStatus.isSuccess()) {
            Logger.e(SSConstants.TAG_SUPRSEND, operationStatus.message ?: "No response", operationStatus.exception)
            return Response.Error(
                operationStatus.exception ?: IllegalStateException("Failed to refresh token while getting notifications"),
                operationStatus.message ?: "Failed to refresh token while getting notifications"
            )
        }

        val inboxBaseUrl = inboxData.baseUrl

        var url = "$inboxBaseUrl/v1/feed/notifications/$notificationId?distinct_id=$encodedDistinctId"

        if (store != null) {
            url += "&store=${urlEncode(store.toString())}"
        }

        url = url.addTenantIdIfPresent()

        val httpResponse = networkClient.httpCall(
            requestMethod = "GET",
            url = url,
            authorization = SSInternal.suprSendData.publicApiKey ?: "",
            headers = SSInternal.addSSSignature(
                mutableMapOf(
                    "content-type" to "application/json"
                )
            )
        )

        return if (httpResponse.isSuccess()) {
            val inboxNotificationsResponse = InboxNotification.fromJson(JSONObject(httpResponse.body ?: "{}"))
            Response.Success(inboxNotificationsResponse)
        } else {
//            SSInternal.checkAndRemoveLocalToken(httpResponse.getErrorBody())
            Response.Error(
                ex = IllegalStateException("Failed to get notification detail : ${httpResponse.statusCode}")
            )
        }
    }

    fun markAllRead(): Response<Boolean> {
        inboxData.storesMap.forEach { (_, store) ->
            store.inboxMessagesList.forEach {
                it.seenOn = System.currentTimeMillis()
                handleNotificationPropertiesChanges(it)
            }
        }
        val distinctId = urlEncode(SuprSend.getInstance().getDistinctId() ?: "")
        val inboxBaseUrl = inboxData.baseUrl
        val isSuccess = callAPI(
            url = "$inboxBaseUrl/v1/feed/mark_all_read?distinct_id=$distinctId".addTenantIdIfPresent(),
            requestMethod = "PATCH"
        ).isSuccess()
        return Response.Success(isSuccess)
    }

    fun markAsInteracted(notificationId: String): Response<Boolean> {
        inboxData.storesMap.forEach { (_, store) ->
            store.inboxMessagesList.find { it.id == notificationId }?.let {
                if (it.interactedOn == null) {
                    it.interactedOn = System.currentTimeMillis()
                }
                if (it.readOn == null) {
                    it.readOn = System.currentTimeMillis()
                }
                handleNotificationPropertiesChanges(it)
            }
        }
        val distinctId = urlEncode(SuprSend.getInstance().getDistinctId() ?: "")
        val inboxBaseUrl = inboxData.baseUrl
        val response = callAPI(
            url = "$inboxBaseUrl/v1/feed/notifications/${notificationId}/interacted?distinct_id=$distinctId".addTenantIdIfPresent(),
            requestMethod = "PATCH"
        )
        if (!response.isSuccess()) {
            notifyError(id = notificationId, errorType = InBoxErrorType.NOTIFICATION, message = "Mark as interacted has failed", e = response.getException())
        }
        return response
    }

    fun markAsUnread(notificationId: String): Response<Boolean> {
        inboxData.storesMap.forEach { (_, store) ->
            store.inboxMessagesList.find { it.id == notificationId }?.let { notification ->
                if (notification.readOn != null) {
                    notification.readOn = null
                    handleNotificationPropertiesChanges(notification)
                    Logger.v(SSConstants.TAG_SUPRSEND, "${notification.message.header} marked as read")
                }
            }
        }
        val distinctId = urlEncode(SuprSend.getInstance().getDistinctId() ?: "")
        val inboxBaseUrl = inboxData.baseUrl
        val response = callAPI(
            url = "$inboxBaseUrl/v1/feed/notifications/${notificationId}/unread?distinct_id=$distinctId".addTenantIdIfPresent(),
            requestMethod = "PATCH"
        )
        if (!response.isSuccess()) {
            notifyError(id = notificationId, errorType = InBoxErrorType.NOTIFICATION, message = "Mark as unread has failed", e = response.getException())
        }
        return response
    }

    fun markAsRead(notificationId: String): Response<Boolean> {
        inboxData.storesMap.forEach { (_, store) ->
            store.inboxMessagesList.find { it.id == notificationId }?.let { notification ->
                if (notification.readOn == null || notification.readOn == 0L) {
                    notification.readOn = System.currentTimeMillis()
                    notification.seenOn = System.currentTimeMillis()
                    handleNotificationPropertiesChanges(notification)
                    Logger.v(SSConstants.TAG_SUPRSEND, "${notification.message.header} marked as read")
                }
            }
        }

        val distinctId = urlEncode(SuprSend.getInstance().getDistinctId() ?: "")
        val inboxBaseUrl = inboxData.baseUrl
        val response = callAPI(
            url = "$inboxBaseUrl/v1/feed/notifications/${notificationId}/read?distinct_id=$distinctId".addTenantIdIfPresent(),
            requestMethod = "PATCH"
        )
        if (!response.isSuccess()) {
            notifyError(id = notificationId, errorType = InBoxErrorType.NOTIFICATION, message = "Mark as read has failed", e = response.getException())
        }
        return response
    }

    fun markAsArchived(notificationId: String): Response<Boolean> {
        inboxData.storesMap.forEach { (_, store) ->
            store.inboxMessagesList.find { it.id == notificationId }?.let {
                it.archived = true
                handleNotificationPropertiesChanges(it)
            }
        }

        val distinctId = urlEncode(SuprSend.getInstance().getDistinctId() ?: "")
        val inboxBaseUrl = inboxData.baseUrl
        var url = "$inboxBaseUrl/v1/feed/notifications/${notificationId}/archive?distinct_id=$distinctId"
        url = url.addTenantIdIfPresent()
        val response = callAPI(
            url = url,
            requestMethod = "PATCH"
        )

        if (!response.isSuccess()) {
            notifyError(id = notificationId, errorType = InBoxErrorType.NOTIFICATION, message = "Mark as archive has failed", e = response.getException())
        }
        return response


    }

    fun markAsSeen(notificationIds: List<String>): Response<Boolean> {
        if (!NetworkInfo.isConnected()) {
            return Response.Error(
                ex = NoInternetException(),
                message = "Internet connection is not available"
            )
        }

        inboxData.storesMap.forEach { (_, store) ->
            store.inboxMessagesList.filter { notificationIds.contains(it.id) }.forEach {
                it.seenOn = System.currentTimeMillis()
                handleNotificationPropertiesChanges(it)
            }
        }

        val distinctId = urlEncode(SuprSend.getInstance().getDistinctId() ?: "")

        val operationStatus = SSInternal.refreshTokenIfRequired(distinctId = distinctId)

        if (!operationStatus.isSuccess()) {
            Logger.e(SSConstants.TAG_SUPRSEND, operationStatus.message ?: "No response", operationStatus.exception)
            return Response.Error(
                operationStatus.exception ?: IllegalStateException("Failed to refresh token while marking notification seen"),
                operationStatus.message ?: "Failed to refresh token while marking notification seen"
            )
        }

        val inboxBaseUrl = inboxData.baseUrl

        var url = "$inboxBaseUrl/v1/feed/bulk/notifications/seen?distinct_id=$distinctId"
        url = url.addTenantIdIfPresent()

        val requestJson = JSONObject().apply {
            put("notification_ids", JSONArray(notificationIds))
        }.toString()

        val httpResponse = networkClient.httpCall(
            requestMethod = "POST",
            url = url,
            authorization = SSInternal.suprSendData.publicApiKey ?: "",
            headers = SSInternal.addSSSignature(
                mutableMapOf(
                    "content-type" to "application/json"
                )
            ),
            requestJson = requestJson
        )

        return if (httpResponse.isSuccess()) {
            Response.Success(true)
        } else {
            Response.Error(
                ex = IllegalStateException("Failed to mark as seen : ${httpResponse.statusCode}")
            )
        }
    }

    fun sync() {
        try {
            sdkExecutorService.execute {
//                if (inboxData.isLoading) {
//                    Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Inbox : Sync already in progress skipping this request")
//                    return@execute
//                }

//                inboxData.isLoading = true

                Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Inbox : Fetching bell count")
                fetchAndNotifyNotificationsCount()

//                Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Inbox : Fetching inbox notifications")
//                val currentTime = System.currentTimeMillis()
//                var currentPageNo = 1
//                val inboxMessagesList = arrayListOf<InboxNotification>()
//                inboxMessagesList.clear()
//                var totalPages = 0
//                do {
//                    Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Bell : fetching page $currentPageNo")
//                    val response = SuprsendInbox.getInstance().getNotifications(
//                        before = currentTime,
//                        pageNo = currentPageNo
//                    )
//                    if (response.isSuccess()) {
//                        Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Bell : response success for page $currentPageNo")
//                        inboxMessagesList.addAll(response.getData()?.results ?: listOf())
//                        totalPages = response.getData()?.meta?.totalPages ?: 0
//                    } else {
//                        Logger.e(SSConstants.TAG_SUPRSEND_INBOX, "Bell : response failure for page $currentPageNo")
//                    }
//                    if (currentPageNo == totalPages) {
//                        break
//                    }
//                    currentPageNo++
//                } while (currentPageNo <= totalPages)
//                if (totalPages > 0) {
//                    inboxData.inboxMessagesList.clear()
//                    inboxData.inboxMessagesList.addAll(inboxMessagesList)
//                }
            }
        } catch (e: Exception) {
            Logger.e(SSConstants.TAG_SUPRSEND_INBOX, "Inbox : Sync failed", e)
        } finally {
//            inboxData.isLoading = false
        }
    }

    fun getStores(): List<InboxStore> {
        return inboxData.storesMap.map { it.value }
    }

    fun getStore(storeId: String?): InboxStore {
        val safeStoreID = storeId ?: InboxStore.DEFAULT_STORE
//        if (inboxData.storesMap.size > 1 && (safeStoreID == InboxStore.DEFAULT_STORE || safeStoreID.isBlank())) {
//            throw IllegalStateException("Invalid storeId : $storeId. Please use valid storeId")
//        }
        if (!inboxData.storesMap.containsKey(safeStoreID)) {
            throw IllegalStateException("Invalid storeId : $storeId. Please use valid storeId")
        }
        return inboxData.storesMap[safeStoreID]!!
    }

    fun getStoreCount(): Int = inboxData.storesMap.size

    fun onSocketNewNotification(data: Array<Any>?) {
        val payloadJO = data?.getOrNull(0) as JSONObject? ?: JSONObject()
        if (payloadJO.getString("n_id").isNullOrEmpty()) return
        Thread.sleep(1000)
        val notificationModelResponse = getNotificationDetails(
            notificationId = payloadJO.safeString("n_id") ?: ""
        )
        if (notificationModelResponse.isSuccess()) {
            val inboxNotification = notificationModelResponse.getData()!!
            var updateOverallCount = false
            inboxData
                .storesMap
                .forEach { (id, inboxStore) ->
                    if (notificationBelongToStore(inboxNotification, inboxStore)) {
                        val newNotifications = addNewNotification(
                            inboxNotification,
                            inboxStore.inboxMessagesList
                        )
                        inboxStore.inboxMessagesList.clear()
                        inboxStore.inboxMessagesList.addAll(newNotifications)

                        if (!updateOverallCount) {
                            inboxData.bellCount += 1
                            notifyBellCount(inboxData.bellCount)
                            updateOverallCount = true
                        }
                        notifyListeners(inboxStore)
                    }
                }
            notifyNewNotification(inboxNotification)
            fetchAndNotifyNotificationsCount()
        }

    }

    fun onSocketNotificationUpdate(data: Array<Any>?) {
        val payloadJO = data?.getOrNull(0) as JSONObject? ?: JSONObject()
        if (payloadJO.getString("n_id").isNullOrEmpty()) return
        Thread.sleep(1000)
        val newNotificationModelResponse = getNotificationDetails(notificationId = payloadJO.safeString("n_id") ?: "")
        if (!newNotificationModelResponse.isSuccess()) {
            return
        }

        val newNotificationModel = newNotificationModelResponse.getData()!!

        handleNotificationPropertiesChanges(newNotificationModel)
        fetchAndNotifyNotificationsCount()
    }

    private fun handleNotificationPropertiesChanges(newNotificationModel: InboxNotification) {
        inboxData.storesMap.forEach { (id, store) ->
            val oldNotification = store.inboxMessagesList.find { it.id == newNotificationModel.id }
            val index = store.inboxMessagesList.indexOf(oldNotification)
            val nowItBelongsToCurrentStore = notificationBelongToStore(newNotificationModel, store)
            // Due to change in properties new notification may not belong to current store
            if (oldNotification == null && nowItBelongsToCurrentStore) {
                val newNotifications = addNewNotification(
                    newNotification = newNotificationModel,
                    oldNotifications = store.inboxMessagesList
                )
                store.inboxMessagesList.clear()
                store.inboxMessagesList.addAll(newNotifications)
                Logger.v(
                    tag = SSConstants.TAG_SUPRSEND_INBOX,
                    message = "Updating store - ${store.storeId}\n" +
                            "Now notification belong this store"
                )
            } else if (oldNotification != null) {
                //Due to change in properties(see_on,etc) it may be possible now notification does not belong to current store
                if (!nowItBelongsToCurrentStore) {
                    Logger.v(
                        SSConstants.TAG_SUPRSEND_INBOX,
                        "Updating store - ${store.storeId}\n" +
                                "Now notification do not belong this store"
                    )
                    val removeNotification = store.inboxMessagesList.find { it.id == oldNotification.id }
                    store.inboxMessagesList.remove(removeNotification)
                    if (store.inboxMessagesList.isEmpty()) {
                        store.reset()
                    }
                } else {
                    Logger.v(
                        SSConstants.TAG_SUPRSEND_INBOX, "" +
                                "Updating store - ${store.storeId}\n" +
                                "index - $index\n" +
                                "id - ${newNotificationModel.id}\n" +
                                "old - seenon ${oldNotification.seenOn}\n" +
                                "new - seenon ${newNotificationModel.seenOn}" +
                                "old - archived ${oldNotification.archived}\n" +
                                "new - archived ${newNotificationModel.archived}"
                    )
                    val removeNotification = store.inboxMessagesList.find { it.id == oldNotification.id }
                    store.inboxMessagesList.remove(removeNotification)
                    store.inboxMessagesList.add(index, newNotificationModel.copy())
                }
            }
            notifyListeners(store)
        }
    }

    fun onSocketBulkNotificationUpdate(data: Array<Any>?) {
        val payloadJO = data?.getOrNull(0) as JSONObject? ?: JSONObject()
        val action = payloadJO.safeString("action")
        val notificationStringOrList = payloadJO.get("notification_ids")
        var notificationIds: List<String> = listOf()
        if (notificationStringOrList is String) {
            notificationIds = listOf("all")
        } else if (notificationStringOrList is JSONArray) {
            notificationIds = notificationStringOrList.convertToList()
        }
        if (action == "read" && notificationIds.contains("all")) {
            inboxData.bellCount = 0
            inboxData.storesMap.forEach { (id, store) ->
                store.setUnseenCount(0)
                store.inboxMessagesList.forEach { notification ->
                    notification.readOn = System.currentTimeMillis()
                }
                notifyListeners(store)
            }
        }
        if (action == "seen" && notificationIds.isNotEmpty()) {
            fetchAndNotifyNotificationsCount()
            inboxData.storesMap.forEach { (id, store) ->
                var isChanged = false
                store.inboxMessagesList.forEach { notification ->
                    if (notificationIds.contains(notification.id)) {
                        notification.seenOn = System.currentTimeMillis()
                        isChanged = true
                    }
                }
                if (isChanged)
                    notifyListeners(store)
            }
        }
    }

    //TODO - write tests of all socket callbacks
    fun onSocketResetBadge(data: Array<Any>?) {
        inboxData.bellCount = 0
        inboxData.storesMap.forEach { (_, store) ->
            store.setUnseenCount(0)
            notifyBellCount(inboxData.bellCount)
            notifyListeners(store)
        }
    }

    private fun addNewNotification(
        newNotification: InboxNotification,
        oldNotifications: List<InboxNotification>
    ): MutableList<InboxNotification> {
        val notifications = mutableListOf<InboxNotification>()

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
        notificationModel: InboxNotification,
        store: InboxStore
    ): Boolean {
        val notificationRead = notificationModel.readOn != null && notificationModel.readOn != 0L
        val notificationArchive: Boolean = notificationModel.archived
        val notificationTags: List<String> = notificationModel.tags ?: emptyList()
        val notificationCategory: String = notificationModel.category

        val query = store.query
        val storeRead = query?.read
        val storeArchived = query?.archived

        val storeTags = query?.tags
        val storeCategories = query?.categories

        val sameRead = storeRead == null || notificationRead == storeRead
        val sameArchived = if (storeArchived == null) !notificationArchive else notificationArchive == storeArchived
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

    private fun callAPI(
        url: String,
        requestMethod: String = "POST"
    ): Response<Boolean> {
        if (!NetworkInfo.isConnected()) {
            return Response.Error(
                ex = NoInternetException(),
                message = "Internet connection is not available"
            )
        }

        val distinctId = urlEncode(SuprSend.getInstance().getDistinctId() ?: "")

        val operationStatus = SSInternal.refreshTokenIfRequired(distinctId = distinctId)

        if (!operationStatus.isSuccess()) {
            Logger.e(SSConstants.TAG_SUPRSEND, operationStatus.message ?: "No response", operationStatus.exception)
            return Response.Error(
                operationStatus.exception ?: IllegalStateException("Failed to refresh token while marking notification seen"),
                operationStatus.message ?: "Failed to refresh token while marking notification seen"
            )
        }

        val tenantId = SSInboxInternal.inboxData.tenantId
        val finalUrl = if (!tenantId.isNullOrBlank()) {
            "$url&tenant_id=${urlEncode(tenantId)}"
        } else {
            url
        }

        val requestJson = JSONObject().toString()

        val httpResponse = networkClient.httpCall(
            requestMethod = requestMethod,
            url = finalUrl,
            authorization = SSInternal.suprSendData.publicApiKey ?: "",
            headers = SSInternal.addSSSignature(
                mutableMapOf(
                    "content-type" to "application/json"
                )
            ),
            requestJson = requestJson
        )

        val response = if (httpResponse.isSuccess()) {
            Response.Success(true)
        } else {
            Logger.e(SSConstants.TAG_SUPRSEND_INBOX, "API call failed : $finalUrl ")
            Response.Error(
                ex = IllegalStateException("Failed: ${httpResponse.statusCode} : $url "),
                body = httpResponse.body
            )
        }
        if (!response.isSuccess()) {
            SSInternal.checkStatusCodeAndRemoveLocalToken(response.getErrorBody())
        }

        return response
    }

    fun notifyError(id: String, errorType: InBoxErrorType, message: String, e: Exception? = null) {
        ContextCompat.getMainExecutor(SSInternal.context).execute {
            inboxStoreListeners.forEach {
                it.onError(id, errorType, message, e)
            }
        }
    }

    fun notifyBellCount(bellCount: Int) {
        ContextCompat.getMainExecutor(SSInternal.context).execute {
            inboxStoreListeners.forEach {
                it.bellCount(bellCount)
            }
        }
    }

    private fun notifyNewNotification(notificationModel: InboxNotification) {
        ContextCompat.getMainExecutor(SSInternal.context).execute {
            inboxStoreListeners.forEach {
                it.newNotification(notificationModel)
            }
        }
    }

    fun notifyLoading(storeId: String, isLoading: Boolean) {
        ContextCompat.getMainExecutor(SSInternal.context).execute {
            inboxStoreListeners.forEach {
                it.loading(storeId, isLoading)
            }
        }
    }

    fun notifySocketStatus(connectionState: ConnectionState) {
        ContextCompat.getMainExecutor(SSInternal.context).execute {
            inboxStoreListeners.forEach {
                it.socket(connectionState)
            }
        }
    }

    fun notifyListeners(
        inboxStore: InboxStore
    ) {
        ContextCompat.getMainExecutor(SSInternal.context).execute {
            inboxStoreListeners.forEach {
                it.onUpdate(inboxStore)
            }
        }
    }

    fun registerCallback(inboxNotificationUpdateCallback: InboxStoreListener) {
        if (!inboxStoreListeners.contains(inboxNotificationUpdateCallback)) {
            inboxStoreListeners.add(inboxNotificationUpdateCallback)
        }
    }

    fun unRegisterCallback(inboxNotificationUpdateCallback: InboxStoreListener) {
        if (inboxStoreListeners.contains(inboxNotificationUpdateCallback)) {
            inboxStoreListeners.remove(inboxNotificationUpdateCallback)
        }
    }

    fun fetchNotificationsAndNotify(
        inboxStore: InboxStore
    ): Response<InboxStore> {
        val returnResponse: Response<InboxStore> = try {
            if (inboxStore.isLoading) {
                val message = "Inbox : Store (${inboxStore.storeId}) sync already in progress skipping this request"
                Logger.i(SSConstants.TAG_SUPRSEND_INBOX, message)
                return Response.Error(IllegalStateException(message))
            }
            if (!inboxStore.hasNextPage() && inboxStore.initialFetchTime != -1L) {
                val message = "Inbox : Store (${inboxStore.storeId}) sync already completed skipping this request"
                Logger.e(SSConstants.TAG_SUPRSEND_INBOX, message)
                return Response.Error(IllegalStateException(message))
            }

            inboxStore.isLoading = true
            notifyLoading(inboxStore.storeId, inboxStore.isLoading)

            var tempLoadedAtTimeStamp = inboxStore.initialFetchTime
            if (tempLoadedAtTimeStamp == -1L) {
                tempLoadedAtTimeStamp = System.currentTimeMillis()
            }

            val response = getNotificationsRemote(
                before = tempLoadedAtTimeStamp,
                store = if (inboxStore.storeId == InboxStore.DEFAULT_STORE) null else inboxStore.toJSONObject(),
                pageNo = inboxStore.inStoreMeta.currentPageNo + 1
            )
            if (response.isSuccess()) {
                inboxStore.initialFetchTime = tempLoadedAtTimeStamp
                val inboxNotificationsResponse = response.getData()
                inboxStore.inStoreMeta = inboxNotificationsResponse?.inStoreMeta!!
                if (inboxStore.inStoreMeta.currentPageNo == 1) {
                    inboxStore.inboxMessagesList.clear()
                }
                inboxStore.inboxMessagesList.addAll(inboxNotificationsResponse.results)
                notifyListeners(inboxStore)
                Response.Success(inboxStore)
            } else {
                SSInternal.checkStatusCodeAndRemoveLocalToken(response.getErrorBody())
                val message = "${inboxStore.storeId} : Store sync has failed"
                val exception = response.getException() ?: IllegalStateException("Store (${inboxStore.storeId}) sync failed")
                notifyError(inboxStore.storeId, InBoxErrorType.STORE, message = message, e = exception)
                Logger.e(SSConstants.TAG_SUPRSEND_INBOX, message, exception)
                Response.Error(ex = exception)
            }
        } catch (e: Exception) {
            Logger.e(SSConstants.TAG_SUPRSEND_INBOX, "Inbox : Store sync failed : ${inboxStore.storeId}", e)
            Response.Error(ex = IllegalStateException("Store (${inboxStore.storeId}) sync failed"))
        } finally {
            inboxStore.isLoading = false
            notifyLoading(inboxStore.storeId, inboxStore.isLoading)
        }
        return returnResponse
    }

    fun reset() {
        inboxData = InboxData()
        SSInboxSocket.disconnect()
        SSInboxExpiredMessages.stop()
    }

}