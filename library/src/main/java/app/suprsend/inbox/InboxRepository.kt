package app.suprsend.inbox

import app.suprsend.BuildConfig
import app.suprsend.SSApiInternal
import app.suprsend.base.Logger
import app.suprsend.base.SSConstants
import app.suprsend.base.getDate
import app.suprsend.base.httpCall
import app.suprsend.base.urlEncode
import app.suprsend.base.uuid
import app.suprsend.config.ConfigHelper
import app.suprsend.inbox.model.InboxData
import app.suprsend.inbox.model.NotificationListModel
import app.suprsend.inbox.model.NotificationModel
import app.suprsend.inbox.model.NotificationStore
import app.suprsend.inbox.model.hasMultipleStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal class InboxRepository {

    fun fetchNotificationListAndConvertToModels(
        subscriberId: String,
        distinctId: String,
        tenantId: String,
        pageNumber: Int,
        pageSize: Int,
        before: Long,
        notificationStore: NotificationStore? = null
    ): NotificationListModel? {
        val notificationListJson = try {
            fetchNotificationsList(
                subscriberId = subscriberId,
                distinctId = distinctId,
                tenantId = tenantId,
                pageNumber = pageNumber,
                pageSize = pageSize,
                before = before,
                notificationStore = notificationStore
            )
        } catch (e: Exception) {
            ""
        }
        return NotificationListModel.convertToModels(notificationListJson)
    }

    fun fetchAndUpdateNotificationCount(
        inboxData: InboxData
    ) {
        try {
            val urlBuilder = StringBuilder("/notification_count/")
            urlBuilder.append("?subscriber_id=").append(inboxData.subscriberId)
            urlBuilder.append("&distinct_id=").append(inboxData.distinctId)
            urlBuilder.append("&tenant_id=").append(urlEncode(inboxData.tenantId))
            val configsJA = JSONArray()
            val notificationStores = inboxData.notificationStores
            if (notificationStores.hasMultipleStore()) {
                notificationStores.forEach { store ->
                    val configJO = store.notificationStoreConfig.toJson()
                    configsJA.put(configJO)
                }
                urlBuilder.append("&stores=").append(urlEncode(configsJA.toString()))
            }

            val requestURI = urlBuilder.toString()

            val date = getDate()
            val requestMethod = "GET"

            val workspaceKey = ConfigHelper.get(SSConstants.CONFIG_API_KEY) ?: ""
            val authorization = "$workspaceKey:${UUID.randomUUID()}"

            val baseUrl = SSApiInternal.getInboxBaseUrl()
            val url = "$baseUrl$requestURI"
            val httpResponse = httpCall(
                urL = url,
                authorization = authorization,
                date = date,
                requestMethod = requestMethod
            )
            if (httpResponse.ok()) {
                val json = JSONObject(httpResponse.response ?: "")
                inboxData.bellCount = json.getInt("ss_bell_count")
                notificationStores
                    .forEach { store ->
                        if (json.has(store.notificationStoreConfig.storeId)) {
                            store.unseenCount = json.getInt(store.notificationStoreConfig.storeId)
                        }
                    }
            }
        } catch (e: Exception) {
            Logger.e(SSInbox.LOGGING_TAG, "Fetch Notification Count", e)
        }
    }

    fun markNotificationClicked(
        notificationId: String
    ): Boolean {
        val requestURI = "/notification/${notificationId}/action"
        val bodyJo = JSONObject()
        bodyJo.put(SSConstants.EVENT, SSConstants.S_EVENT_NOTIFICATION_CLICKED)
        bodyJo.put(SSConstants.ENV, SSApiInternal.getCachedApiKey())
        bodyJo.put(SSConstants.INSERT_ID, uuid())
        bodyJo.put(SSConstants.TIME, System.currentTimeMillis())
        bodyJo.put(SSConstants.PROPERTIES, JSONObject().apply {
            put("id", notificationId)
        })
        val date = getDate()
        val requestMethod = "POST"

        val workspaceKey = ConfigHelper.get(SSConstants.CONFIG_API_KEY) ?: ""
        val authorization = "$workspaceKey:"

        val baseUrl = SSApiInternal.getBaseUrl()
        val url = "$baseUrl$requestURI"
        val httpResponse = httpCall(
            urL = url,
            authorization = authorization,
            date = date,
            requestMethod = requestMethod,
            requestJson = bodyJo.toString()
        )
        return httpResponse.ok()
    }

    fun markNotificationReadStatus(
        notificationId: String,
        distinctId: String,
        subscriberId: String,
        read: Boolean
    ): Boolean {
        val requestURI = "/notification/${notificationId}/action"
        val bodyJo = JSONObject()

        bodyJo.put("action", if (read) "read" else "unread")
        bodyJo.put("distinct_id", distinctId)
        bodyJo.put("subscriber_id", subscriberId)
        val date = getDate()
        val requestMethod = "POST"

        val workspaceKey = ConfigHelper.get(SSConstants.CONFIG_API_KEY) ?: ""
        val authorization = "$workspaceKey:${UUID.randomUUID()}"

        val baseUrl = SSApiInternal.getInboxBaseUrl()
        val url = "$baseUrl$requestURI"
        val httpResponse = httpCall(
            urL = url,
            authorization = authorization,
            date = date,
            requestMethod = requestMethod,
            requestJson = bodyJo.toString()
        )
        return httpResponse.ok()
    }

    fun markNotificationArchive(
        notificationId: String,
        distinctId: String,
        subscriberId: String
    ): Boolean {
        val requestURI = "/notification/${notificationId}/action"
        val bodyJo = JSONObject()

        bodyJo.put("action", "archive")
        bodyJo.put("distinct_id", distinctId)
        bodyJo.put("subscriber_id", subscriberId)
        val date = getDate()
        val requestMethod = "POST"

        val workspaceKey = ConfigHelper.get(SSConstants.CONFIG_API_KEY) ?: ""
        val authorization = "$workspaceKey:${UUID.randomUUID()}"

        val baseUrl = SSApiInternal.getInboxBaseUrl()
        val url = "$baseUrl$requestURI"
        val httpResponse = httpCall(
            urL = url,
            authorization = authorization,
            date = date,
            requestMethod = requestMethod,
            requestJson = bodyJo.toString()
        )
        return httpResponse.ok()
    }

    fun getNotificationDetails(
        notificationId: String,
        subscriberId: String,
        distinctId: String,
        tenantId: String
    ): NotificationModel? {
        try {
            val requestURI = "/notification/${notificationId}/?subscriber_id=${subscriberId}&distinct_id=${distinctId}&tenant_id=${tenantId}"

            val date = getDate()
            val requestMethod = "GET"

            val workspaceKey = ConfigHelper.get(SSConstants.CONFIG_API_KEY) ?: ""
            val authorization = "$workspaceKey:${UUID.randomUUID()}"

            val baseUrl = SSApiInternal.getInboxBaseUrl()
            val url = "$baseUrl$requestURI"
            val httpResponse = httpCall(
                urL = url,
                authorization = authorization,
                date = date,
                requestMethod = requestMethod
            )
            val responseJO = JSONObject(httpResponse.response ?: "")
            return NotificationListModel.convertToModel(
                responseJO.getJSONObject("data")
            )
        } catch (e: Exception) {
            //Do noting user can be in offline mode
        }

        return null
    }

    fun hitApi(
        requestURI: String,
        distinctId: String,
        subscriberId: String,
        tenantId: String
    ): Boolean {
        val bodyJo = JSONObject()
        bodyJo.put("time", System.currentTimeMillis())
        bodyJo.put(SSConstants.DISTINCT_ID, distinctId)
        bodyJo.put(SSConstants.SUBSCRIBER_ID, subscriberId)
        bodyJo.put(SSConstants.TENANT_ID, tenantId)
        val date = getDate()
        val requestMethod = "POST"

        val workspaceKey = ConfigHelper.get(SSConstants.CONFIG_API_KEY) ?: ""
        val authorization = "$workspaceKey:${UUID.randomUUID()}"

        val baseUrl = SSApiInternal.getInboxBaseUrl()
        val url = "$baseUrl$requestURI"
        val httpResponse = httpCall(
            urL = url,
            authorization = authorization,
            date = date,
            requestMethod = requestMethod,
            requestJson = bodyJo.toString()
        )
        return httpResponse.ok()
    }

    private fun fetchNotificationsList(
        subscriberId: String,
        distinctId: String,
        tenantId: String,
        pageNumber: Int,
        pageSize: Int,
        before: Long,
        notificationStore: NotificationStore?
    ): String {
        val urlBuilder = StringBuilder("/notifications/")
        urlBuilder.append("?subscriber_id=").append(subscriberId)
        urlBuilder.append("&distinct_id=").append(distinctId)
        urlBuilder.append("&tenant_id=").append(urlEncode(tenantId))
        urlBuilder.append("&page_no=").append(pageNumber)
        urlBuilder.append("&page_size=").append(pageSize)
        urlBuilder.append("&before=").append(before)
        if (notificationStore != null) {
            val storedFilter = notificationStore.notificationStoreConfig.toJson()
            if (storedFilter != null)
                urlBuilder.append("&store=").append(urlEncode(storedFilter.toString()))
        }

        val requestURI = urlBuilder.toString()

        val date = getDate()
        val requestMethod = "GET"

        val workspaceKey = ConfigHelper.get(SSConstants.CONFIG_API_KEY) ?: ""
        val authorization = "$workspaceKey:${UUID.randomUUID()}"

        val baseUrl = SSApiInternal.getInboxBaseUrl()
        val url = "$baseUrl$requestURI"
        val httpResponse = httpCall(
            urL = url,
            authorization = authorization,
            date = date,
            requestMethod = requestMethod
        )
        return if (httpResponse.ok()) httpResponse.response ?: "" else ""
    }

}