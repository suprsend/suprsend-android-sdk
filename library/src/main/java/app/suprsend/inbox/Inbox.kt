package app.suprsend.inbox

import app.suprsend.SSApiInternal
import app.suprsend.base.Logger
import app.suprsend.base.SSConstants
import app.suprsend.base.getDate
import app.suprsend.base.httpCall
import app.suprsend.base.safeBoolean
import app.suprsend.base.safeLong
import app.suprsend.base.safeString
import app.suprsend.base.urlEncode
import app.suprsend.inbox.model.NotificationListMeta
import app.suprsend.inbox.model.NotificationListModel
import app.suprsend.inbox.model.NotificationMessage
import app.suprsend.inbox.model.NotificationModel
import app.suprsend.inbox.model.NotificationStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Inbox
constructor(
    private var workspaceKey: String,
    private val subscriberId: String,
    private val distinctId: String,
    private val tenantId: String? = null
) {
    private var _tenantID: String

    init {
        _tenantID = tenantId ?: ""
        if (_tenantID.isBlank())
            _tenantID = "default"
    }

    fun getNotificationsList(
        pageNumber: Int,
        pageSize: Int,
        before: Long,
        notificationStore: NotificationStore? = null
    ): NotificationListModel? {
        val notificationListJson = fetchNotificationsList(
            subscriberId = subscriberId,
            distinctId = distinctId,
            tenantId = _tenantID,
            pageNumber = pageNumber,
            pageSize = pageSize,
            before = before,
            notificationStore = notificationStore
        )
        return convertToModels(notificationListJson)
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
            val storedFilter = getNotificationStoreQueryString(notificationStore)
            if (storedFilter != null)
                urlBuilder.append("&store=").append(urlEncode(storedFilter))
        }

        val requestURI = urlBuilder.toString()

        val date = getDate()
        val requestMethod = "GET"

        val authorization = "$workspaceKey:${UUID.randomUUID()}"

        val baseUrl = SSApiInternal.getInboxBaseUrl()
        val url = "$baseUrl$requestURI"
        Logger.i(SSConstants.TAG_SUPRSEND, "Requesting : $url")
        val httpResponse = httpCall(
            urL = url,
            authorization = authorization,
            date = date,
            requestMethod = requestMethod
        )
        Logger.i(SSConstants.TAG_SUPRSEND, "Response Received : $url \nCode : ${httpResponse.statusCode}")
        return httpResponse.response ?: ""
    }

    private fun getNotificationStoreQueryString(notificationStore: NotificationStore): String? {
        val query = notificationStore.query ?: return null

        val tags = query.tags
        val categories = query.categories
        val read = query.read

        val jsonObject = JSONObject()
        jsonObject.put("store_id", notificationStore.storeId)

        val queryJo = JSONObject()
        if (read != null)
            queryJo.put("read", read)
        if (tags != null)
            queryJo.put("tags", queryOrOperator(tags))
        if (categories != null)
            queryJo.put("categories", queryOrOperator(categories))
        jsonObject.put("query", queryJo)
        return jsonObject.toString(0)
    }

    private fun queryOrOperator(values: List<String>): JSONObject {
        val ja = JSONArray()
        values.forEach {
            ja.put(it)
        }
        return JSONObject().apply {
            put("or", ja)
        }
    }

    private fun convertToModels(notificationListJson: String): NotificationListModel? {

        val jsonObject = try {
            JSONObject(notificationListJson)
        } catch (e: Exception) {
            return null
        }

        val metaJson = jsonObject.getJSONObject("meta")
        val total = jsonObject.optInt("total")
        val unseen = jsonObject.optInt("unseen")
        val resultsJsonArray = jsonObject.optJSONArray("results") ?: JSONArray()

        val meta = NotificationListMeta(
            currentPage = metaJson.optInt("current_page"),
            totalPages = metaJson.optInt("total_pages")
        )

        val results = mutableListOf<NotificationModel>()
        for (i in 0 until resultsJsonArray.length()) {
            val resultJson = resultsJsonArray.optJSONObject(i)
            val messageJson = resultJson.optJSONObject("message")
            val notificationModel = NotificationModel(
                tenantId = resultJson.safeString("tenant_id") ?: "",
                isExpiryVisible = resultJson.safeBoolean("is_expiry_visible") ?: false,
                importance = resultJson.safeString("importance") ?: "",
                message = NotificationMessage(
                    schema = messageJson?.safeString("schema") ?: "",
                    header = messageJson?.safeString("header") ?: "",
                    text = messageJson?.safeString("text") ?: ""
                ),
                isPinned = resultJson.safeBoolean("is_pinned") ?: false,
                archived = resultJson.safeBoolean("archived") ?: false,
                createdOn = resultJson.safeLong("created_on") ?: -1,
                category = resultJson.safeString("n_category") ?: "",
                canUserUnpin = resultJson.safeBoolean("can_user_unpin") ?: false,
                id = resultJson.safeString("n_id") ?: ""
            )
            results.add(notificationModel)
        }

        return NotificationListModel(
            meta = meta,
            total = total,
            unseen = unseen,
            results = results
        )

    }
}