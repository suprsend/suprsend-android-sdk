package app.suprsend.inbox

import app.suprsend.utils.convertToList
import app.suprsend.utils.toJsonArray
import org.json.JSONObject

data class InboxQuery(
    val tags: List<String> = listOf(),
    val categories: List<String> = listOf(),
    val read: Boolean? = null,
    val archived: Boolean? = null
) {

    constructor(json: JSONObject) : this(
        tags = json.opt("tags").convertToList(),
        categories = json.opt("categories").convertToList(),
        read = if (json.has("read")) json.getBoolean("read") else null,
        archived = if (json.has("archived")) json.getBoolean("archived") else null
    )

    fun toJSONObject(): JSONObject {
        val jo = JSONObject().apply {
            put("read", read)
            put("archived", archived)
            put("tags", JSONObject().apply {
                put("or", tags.toJsonArray())
            })
            put("categories", JSONObject().apply {
                put("or", categories.toJsonArray())
            })
        }
        return jo
    }
}