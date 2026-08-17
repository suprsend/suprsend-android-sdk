package app.suprsend.inbox

import app.suprsend.utils.convertToList
import app.suprsend.utils.safeLong
import org.json.JSONArray
import org.json.JSONObject


data class InboxNotificationsResponse(
    val inStoreMeta: InStoreMeta,
    val results: List<InboxNotification>
) {
    companion object {
        fun fromJson(jsonString: String): InboxNotificationsResponse {
            val jsonObject = JSONObject(jsonString)

            // Parsing meta
            val metaObject = jsonObject.getJSONObject("meta")
            val inStoreMeta = InStoreMeta.fromJson(metaObject)

            // Parsing results array
            val resultsArray = jsonObject.getJSONArray("results")
            val results = mutableListOf<InboxNotification>()

            for (i in 0 until resultsArray.length()) {
                results.add(InboxNotification.fromJson(resultsArray.getJSONObject(i)))
            }

            return InboxNotificationsResponse(inStoreMeta, results)
        }
    }
}

data class InStoreMeta(
    val totalCount: Int = -1,
    val currentPageNo: Int = 0,
    val totalPages: Int = -1
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): InStoreMeta {
            return InStoreMeta(
                totalCount = jsonObject.optInt("total_count"),
                currentPageNo = jsonObject.optInt("current_page"),
                totalPages = jsonObject.optInt("total_pages")
            )
        }
    }
}

data class InboxNotification(
    val tenantId: String,
    var archived: Boolean,
    val isExpiryVisible: Boolean,
    val createdOn: Long,
    val category: String,
    var seenOn:Long?,
    var readOn:Long?,
    var interactedOn:Long?,
    val expiry: Long?,
    val message: InboxNotificationMessage,
    val canUserUnpin: Boolean,
    val isPinned: Boolean,
    val id: String,
    val tags: List<String>?
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): InboxNotification {
            return InboxNotification(
                tenantId = jsonObject.optString("tenant_id"),
                archived = jsonObject.optBoolean("archived"),
                isExpiryVisible = jsonObject.optBoolean("is_expiry_visible"),
                createdOn = jsonObject.optLong("created_on"),
                category = jsonObject.optString("n_category"),
                seenOn = jsonObject.optLong("seen_on"),
                readOn = jsonObject.optLong("read_on"),
                interactedOn = jsonObject.optLong("interacted_on"),
                expiry = jsonObject.safeLong("expiry"),
                message = InboxNotificationMessage.fromJson(jsonObject.getJSONObject("message")),
                canUserUnpin = jsonObject.optBoolean("can_user_unpin"),
                isPinned = jsonObject.optBoolean("is_pinned"),
                id = jsonObject.optString("n_id"),
                tags = (jsonObject.optJSONArray("tags") ?: JSONArray()).convertToList()
            )
        }
    }
}

data class InboxNotificationMessage(
    val schema: String,
    val header: String,
    val text: String
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): InboxNotificationMessage {
            return InboxNotificationMessage(
                schema = jsonObject.optString("schema"),
                header = jsonObject.optString("header"),
                text = jsonObject.optString("text")
            )
        }
    }
}