package app.suprsend.feed

import app.suprsend.utils.convertToList
import app.suprsend.utils.safeBoolean
import app.suprsend.utils.safeString
import org.json.JSONArray
import org.json.JSONObject

/**
 * Configuration options of a [Feed] instance.
 *
 * @param tenantId Tenant to read the feed from. Defaults to the tenant set while identifying
 *   the user (or via `SuprSend.changeTenant`), else the "default" tenant.
 * @param pageSize Notifications per page. Defaults to 20, capped at 100.
 * @param stores Filtered views inside the feed (multi tab inbox).
 * @param host Overrides the feed API and socket hosts.
 */
class IFeedOptions(
    val tenantId: String? = null,
    val pageSize: Int? = null,
    val stores: List<IStore>? = null,
    val host: FeedHost? = null
)

class FeedHost(
    val socketHost: String? = null,
    val apiHost: String? = null
)

class IStoreQuery(
    val tags: List<String>? = null,
    val categories: List<String>? = null,
    val read: Boolean? = null,
    val archived: Boolean? = null
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            if (tags != null) {
                put("tags", JSONArray().apply { tags.forEach { put(it) } })
            }
            if (categories != null) {
                put("categories", JSONArray().apply { categories.forEach { put(it) } })
            }
            putIfNotNull("read", read)
            putIfNotNull("archived", archived)
        }
    }

    companion object {
        fun fromJson(jsonObject: JSONObject): IStoreQuery {
            return IStoreQuery(
                tags = if (jsonObject.isNull("tags")) null else jsonObject.opt("tags").convertToList(),
                categories = if (jsonObject.isNull("categories")) null else jsonObject.opt("categories").convertToList(),
                read = jsonObject.safeBoolean("read"),
                archived = jsonObject.safeBoolean("archived")
            )
        }
    }
}

class IStore(
    val storeId: String,
    val label: String,
    val query: IStoreQuery? = null
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("store_id", storeId)
            put("label", label)
            putIfNotNull("query", query?.toJSONObject())
        }
    }

    companion object {
        fun fromJson(jsonObject: JSONObject): IStore {
            val queryJO = jsonObject.optJSONObject("query")
            return IStore(
                storeId = jsonObject.safeString("store_id") ?: jsonObject.safeString("storeId") ?: "",
                label = jsonObject.safeString("label") ?: "",
                query = if (queryJO == null) null else IStoreQuery.fromJson(queryJO)
            )
        }

        fun from(jsonArray: JSONArray?): List<IStore> {
            jsonArray ?: return listOf()
            return (0 until jsonArray.length())
                .mapNotNull { jsonArray.optJSONObject(it) }
                .map { fromJson(it) }
        }
    }
}

/**
 * Internal mutable state of a [Feed]. [IFeedData] is the read only view handed to consumers.
 */
class INotificationStore internal constructor(
    val notifications: List<IRemoteNotification>,
    val store: IStore,
    val pageInfo: IPageInfo,
    val meta: Map<String, String>,
    val apiStatus: APIResponseStatus,
    val isFirstFetch: Boolean
) {
    internal fun with(apiStatus: APIResponseStatus): INotificationStore {
        return INotificationStore(
            notifications = notifications,
            store = store,
            pageInfo = pageInfo,
            meta = meta,
            apiStatus = apiStatus,
            isFirstFetch = isFirstFetch
        )
    }

    internal fun with(meta: Map<String, String>?): INotificationStore {
        return INotificationStore(
            notifications = notifications,
            store = store,
            pageInfo = pageInfo,
            meta = meta ?: this.meta,
            apiStatus = apiStatus,
            isFirstFetch = isFirstFetch
        )
    }

    internal fun with(notifications: List<IRemoteNotification>): INotificationStore {
        return INotificationStore(
            notifications = notifications,
            store = store,
            pageInfo = pageInfo,
            meta = meta,
            apiStatus = apiStatus,
            isFirstFetch = isFirstFetch
        )
    }
}

/** Snapshot of the notification store exposed through [Feed.data]. */
class IFeedData internal constructor(
    val notifications: List<IRemoteNotification>,
    val store: IStore,
    val pageInfo: IPageInfo,
    val meta: Map<String, String>,
    val apiStatus: APIResponseStatus
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("notifications", JSONArray().apply { notifications.forEach { put(it.toJSONObject()) } })
            put("store", store.toJSONObject())
            put("pageInfo", pageInfo.toJSONObject())
            put("meta", JSONObject().apply { meta.forEach { put(it.key, it.value) } })
            put("apiStatus", apiStatus.name)
        }
    }
}

sealed class InboxEmitterEvents {

    data class NewNotification(val notification: IRemoteNotification) : InboxEmitterEvents()

    data class StoreUpdate(val data: IFeedData) : InboxEmitterEvents()
}
