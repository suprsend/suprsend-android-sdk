package app.suprsend.inbox.model

import app.suprsend.base.convertToList
import app.suprsend.base.safeBoolean
import app.suprsend.base.safeLong
import app.suprsend.base.safeString
import app.suprsend.inbox.SSInbox
import org.json.JSONArray
import org.json.JSONObject

data class NotificationMessage(
    val schema: String,
    val header: String,
    val text: String
)

data class NotificationModel(
    val tenantId: String,
    val isExpiryVisible: Boolean,
    val importance: String,
    val message: NotificationMessage,
    val tags: List<String>?,
    val isPinned: Boolean,
    val archived: Boolean,
    val createdOn: Long,
    val expiry: Long,
    val category: String,
    val canUserUnpin: Boolean,
    var seenOn: Long? = null,
    var interactedOn: Long? = null,
    val id: String
)

interface InboxStoreListener {
    fun bellCount(bellCount: Int)
    fun loading(storeId: String, isLoading: Boolean)
    fun onUpdate(
        storeId: String,
        allNotifications: List<NotificationModel>
//        totalPages: Int,
//        total: Int
    )

    fun onError(storeId: String, e: Exception)

    fun socket(isConnected:Boolean)
}

internal data class NotificationListModel(
    val meta: NotificationListMeta,
    val total: Int,
    val unseen: Int,
    val results: List<NotificationModel>
) {
    companion object {
        fun convertToModels(notificationListJson: String): NotificationListModel? {
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
                val notificationModel = convertToModel(resultJson)
                results.add(notificationModel)
            }

            return NotificationListModel(
                meta = meta,
                total = total,
                unseen = unseen,
                results = results
            )

        }

        fun convertToModel(resultJson: JSONObject): NotificationModel {
            val messageJson = resultJson.optJSONObject("message")
            return NotificationModel(
                tenantId = resultJson.safeString("tenant_id") ?: "",
                isExpiryVisible = resultJson.safeBoolean("is_expiry_visible") ?: false,
                importance = resultJson.safeString("importance") ?: "",
                message = NotificationMessage(
                    schema = messageJson?.safeString("schema") ?: "",
                    header = messageJson?.safeString("header") ?: "",
                    text = messageJson?.safeString("text") ?: ""
                ),
                tags = (resultJson.optJSONArray("tags") ?: JSONArray()).convertToList(),
                isPinned = resultJson.safeBoolean("is_pinned") ?: false,
                archived = resultJson.safeBoolean("archived") ?: false,
                createdOn = resultJson.safeLong("created_on") ?: -1,
                expiry = resultJson.safeLong("expiry") ?: -1,
                category = resultJson.safeString("n_category") ?: "",
                interactedOn = resultJson.safeLong("interacted_on") ?: -1,
                seenOn = resultJson.safeLong("seen_on"),
                canUserUnpin = resultJson.safeBoolean("can_user_unpin") ?: false,
                id = resultJson.safeString("n_id") ?: ""
            )
        }

    }
}

internal data class NotificationListMeta(
    val currentPage: Int,
    val totalPages: Int
)

data class NotificationStoreConfig(
    val storeId: String,
    val label: String? = null,
    val query: NotificationStoreQuery
) {
    fun toJson(): JSONObject? {
        val tags = query.tags ?: listOf()
        val categories = query.categories ?: listOf()
        val read = query.read

        val jsonObject = JSONObject()
        jsonObject.put("store_id", storeId)

        val queryJo = JSONObject()
        if (read != null)
            queryJo.put("read", read)
        queryJo.put("tags", queryOrOperator(tags))
        queryJo.put("categories", queryOrOperator(categories))
        jsonObject.put("query", queryJo)
        return jsonObject
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
}

internal fun List<NotificationStore>.hasMultipleStore(): Boolean {
    return firstOrNull()?.notificationStoreConfig?.storeId != SSInbox.DEFAULT_STORE_ID
}

data class NotificationStore(
    //Request
    val notificationStoreConfig: NotificationStoreConfig,

    //Response State
    val notifications: MutableList<NotificationModel> = mutableListOf(),
    var isLoading: Boolean = false,
    var currentPageNumber: Int = 0,
    var totalPages: Int = 0,
    var total: Int = -1,
    var unseenCount: Int = -1,
    var initialFetchTime: Long = -1
){
    fun hasInitialFetchTime(): Boolean {
        return initialFetchTime !=-1L
    }
}

data class NotificationStoreQuery(
    val tags: List<String>? = null,
    val categories: List<String>? = null,
    val read: Boolean? = null
)

data class InboxData(
    val notificationStores: MutableList<NotificationStore> = mutableListOf(),
    var bellCount: Int = 0,
    var subscriberId: String = "",
    var distinctId: String = "",
    var tenantId: String = ""
) {
    //Todo - Test
    var pageSize: Int = SSInbox.DEFAULT_PAGINATION_LIMIT
        set(value) {
            var temp = value
            temp = temp.coerceAtMost(SSInbox.DEFAULT_MAX_PAGINATION_LIMIT)
            temp = temp.coerceAtLeast(SSInbox.DEFAULT_MIN_PAGINATION_LIMIT)
            field = temp
        }
}