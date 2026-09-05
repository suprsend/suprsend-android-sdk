package app.suprsend.feed

import app.suprsend.model.ResponseError
import app.suprsend.model.ResponseStatus
import app.suprsend.utils.convertToList
import app.suprsend.utils.safeBoolean
import app.suprsend.utils.safeDouble
import app.suprsend.utils.safeString
import org.json.JSONArray
import org.json.JSONObject

class FeedAPIResponse(
    override val status: ResponseStatus,
    override val statusCode: StatusCode? = null,
    override val body: FeedData? = null,
    override val error: ResponseError? = null
) : Response<FeedData> {

    internal companion object {

        fun success(statusCode: StatusCode? = null, body: FeedData? = null): FeedAPIResponse {
            return FeedAPIResponse(
                status = ResponseStatus.SUCCESS,
                statusCode = statusCode,
                body = body,
                error = null
            )
        }

        fun error(error: ResponseError?, statusCode: StatusCode? = null): FeedAPIResponse {
            return FeedAPIResponse(
                status = ResponseStatus.ERROR,
                statusCode = statusCode,
                body = null,
                error = error
            )
        }
    }
}

class FeedData(
    val results: List<IRemoteNotification>? = null,
    val meta: FeedMeta? = null
) {
    internal companion object {
        fun fromJson(jsonObject: JSONObject): FeedData {
            val resultsJA = jsonObject.optJSONArray("results")
            val results = if (resultsJA == null) null else IRemoteNotification.from(resultsJA)
            val metaJO = jsonObject.optJSONObject("meta")
            return FeedData(
                results = results,
                meta = if (metaJO == null) null else FeedMeta.fromJson(metaJO)
            )
        }
    }
}

class FeedMeta(
    val total_count: Int? = null,
    val current_page: Int? = null,
    val total_pages: Int? = null
) {
    internal companion object {
        fun fromJson(jsonObject: JSONObject): FeedMeta {
            return FeedMeta(
                total_count = jsonObject.safeInt("total_count"),
                current_page = jsonObject.safeInt("current_page"),
                total_pages = jsonObject.safeInt("total_pages")
            )
        }
    }
}

data class IActionObject(
    val name: String,
    val url: String,
    val open_in_new_tab: Boolean?
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("url", url)
            putIfNotNull("open_in_new_tab", open_in_new_tab)
        }
    }

    internal companion object {
        fun fromJson(jsonObject: JSONObject): IActionObject {
            return IActionObject(
                name = jsonObject.safeString("name") ?: "",
                url = jsonObject.safeString("url") ?: "",
                open_in_new_tab = jsonObject.safeBoolean("open_in_new_tab")
            )
        }
    }
}

data class IAvatarObject(
    val action_url: String?,
    val avatar_url: String
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            putIfNotNull("action_url", action_url)
            put("avatar_url", avatar_url)
        }
    }

    internal companion object {
        fun fromJson(jsonObject: JSONObject): IAvatarObject {
            return IAvatarObject(
                action_url = jsonObject.safeString("action_url"),
                avatar_url = jsonObject.safeString("avatar_url") ?: ""
            )
        }
    }
}

data class ISubTextObject(
    val action_url: String?,
    val text: String
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            putIfNotNull("action_url", action_url)
            put("text", text)
        }
    }

    internal companion object {
        fun fromJson(jsonObject: JSONObject): ISubTextObject {
            return ISubTextObject(
                action_url = jsonObject.safeString("action_url"),
                text = jsonObject.safeString("text") ?: ""
            )
        }
    }
}

data class IRemoteNotificationMessage(
    val header: String?,
    val schema: String,
    val text: String,
    val url: String?,
    val open_in_new_tab: Boolean?,
    val extra_data: String?,
    val actions: List<IActionObject>?,
    val avatar: IAvatarObject?,
    val subtext: ISubTextObject?
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            putIfNotNull("header", header)
            put("schema", schema)
            put("text", text)
            putIfNotNull("url", url)
            putIfNotNull("open_in_new_tab", open_in_new_tab)
            putIfNotNull("extra_data", extra_data)
            if (actions != null) {
                put("actions", JSONArray().apply { actions.forEach { put(it.toJSONObject()) } })
            }
            putIfNotNull("avatar", avatar?.toJSONObject())
            putIfNotNull("subtext", subtext?.toJSONObject())
        }
    }

    internal companion object {
        fun fromJson(jsonObject: JSONObject): IRemoteNotificationMessage {
            val actionsJA = jsonObject.optJSONArray("actions")
            val avatarJO = jsonObject.optJSONObject("avatar")
            val subtextJO = jsonObject.optJSONObject("subtext")
            return IRemoteNotificationMessage(
                header = jsonObject.safeString("header"),
                schema = jsonObject.safeString("schema") ?: "",
                text = jsonObject.safeString("text") ?: "",
                url = jsonObject.safeString("url"),
                open_in_new_tab = jsonObject.safeBoolean("open_in_new_tab"),
                extra_data = extraDataFromJson(jsonObject),
                actions = if (actionsJA == null) null else (0 until actionsJA.length())
                    .mapNotNull { actionsJA.optJSONObject(it) }
                    .map { IActionObject.fromJson(it) },
                avatar = if (avatarJO == null) null else IAvatarObject.fromJson(avatarJO),
                subtext = if (subtextJO == null) null else ISubTextObject.fromJson(subtextJO)
            )
        }
    }
}

data class IRemoteNotification(
    val n_id: String,
    val n_category: String,
    val created_on: Double,
    val seen_on: Double?,
    val read_on: Double?,
    val interacted_on: Double?,
    val archived: Boolean?,
    val tags: List<String>?,
    val expiry: Double?,
    val is_expiry_visible: Boolean,
    val is_pinned: Boolean,
    val can_user_unpin: Boolean?,
    val message: IRemoteNotificationMessage
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("n_id", n_id)
            put("n_category", n_category)
            put("created_on", created_on.toJsonNumber())
            putIfNotNull("seen_on", seen_on?.toJsonNumber())
            putIfNotNull("read_on", read_on?.toJsonNumber())
            putIfNotNull("interacted_on", interacted_on?.toJsonNumber())
            putIfNotNull("archived", archived)
            if (tags != null) {
                put("tags", JSONArray().apply { tags.forEach { put(it) } })
            }
            putIfNotNull("expiry", expiry?.toJsonNumber())
            put("is_expiry_visible", is_expiry_visible)
            put("is_pinned", is_pinned)
            putIfNotNull("can_user_unpin", can_user_unpin)
            put("message", message.toJSONObject())
        }
    }

    internal companion object {

        fun fromJson(jsonObject: JSONObject): IRemoteNotification? {
            val notificationId = jsonObject.safeString("n_id")
            val messageJO = jsonObject.optJSONObject("message")
            if (notificationId.isNullOrBlank() || messageJO == null) {
                return null
            }
            return IRemoteNotification(
                n_id = notificationId,
                n_category = jsonObject.safeString("n_category") ?: "",
                created_on = jsonObject.safeDouble("created_on") ?: 0.0,
                seen_on = jsonObject.safeDouble("seen_on"),
                read_on = jsonObject.safeDouble("read_on"),
                interacted_on = jsonObject.safeDouble("interacted_on"),
                archived = jsonObject.safeBoolean("archived"),
                tags = jsonObject.optJSONArray("tags")?.convertToList(),
                expiry = jsonObject.safeDouble("expiry"),
                is_expiry_visible = jsonObject.optBoolean("is_expiry_visible"),
                is_pinned = jsonObject.optBoolean("is_pinned"),
                can_user_unpin = jsonObject.safeBoolean("can_user_unpin"),
                message = IRemoteNotificationMessage.fromJson(messageJO)
            )
        }

        fun from(jsonArray: JSONArray): List<IRemoteNotification> {
            return (0 until jsonArray.length())
                .mapNotNull { jsonArray.optJSONObject(it) }
                .mapNotNull { fromJson(it) }
        }
    }
}

class IPageInfo internal constructor(
    val total: Int,
    val hasMore: Boolean,
    val pageSize: Int
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("total", total)
            put("hasMore", hasMore)
            put("pageSize", pageSize)
        }
    }
}

enum class APIResponseStatus {
    /** Before any request is made (or after a reset). */
    INITIAL,

    /** The initial API call is in progress. */
    LOADING,

    /** The API call was successful, and data has been received. */
    SUCCESS,

    /** The API call failed (network issue, server issue, etc.). */
    ERROR,

    /** The API call is fetching additional data (for pagination or infinite scroll). */
    FETCHING_MORE
}

class IInboxFetchOptions internal constructor(
    val pageSize: Int?
)

internal fun JSONObject.safeInt(key: String): Int? {
    return if (!isNull(key)) getInt(key) else null
}

internal fun JSONObject.putIfNotNull(key: String, value: Any?) {
    if (value != null) {
        put(key, value)
    }
}

/**
 * org.json renders whole doubles in scientific notation on some platforms, which the
 * feed backend rejects for epoch millisecond values. Collapse them to a long first.
 */
internal fun Double.toJsonNumber(): Any {
    return if (!isNaN() && !isInfinite() && this == Math.floor(this)) toLong() else this
}

private fun extraDataFromJson(jsonObject: JSONObject): String? {
    if (jsonObject.isNull("extra_data")) return null
    val extra = jsonObject.opt("extra_data") ?: return null
    return extra as? String ?: extra.toString()
}
