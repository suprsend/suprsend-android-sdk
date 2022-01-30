package app.suprsend.inbox

import app.suprsend.base.safeJSONObject
import app.suprsend.base.safeString
import org.json.JSONArray

internal class SSInboxItemVo(
    val createdOn: String? = null,
    val nCategory: String? = null,
    val seenOn: String? = null,
    val nId: String? = null,
    val text: String? = null,
    val imageUrl: String? = null,
    val button: String? = null,
    val url: String? = null
)

internal fun parseInboxItems(jsonArray: JSONArray?): List<SSInboxItemVo> {
    jsonArray ?: return listOf()
    val inboxItems = arrayListOf<SSInboxItemVo>()
    for (i in 0 until jsonArray.length()) {
        val jo = jsonArray.getJSONObject(i)
        val messageJO = jo.safeJSONObject("message")
        inboxItems.add(
            SSInboxItemVo(
                createdOn = jo.safeString("created_on"),
                nCategory = jo.safeString("n_category"),
                seenOn = jo.safeString("seen_on"),
                nId = jo.safeString("n_id"),
                text = messageJO?.safeString("text"),
                imageUrl = messageJO?.safeString("image"),
                button = messageJO?.safeString("button"),
                url = messageJO?.safeString("url")
            )
        )
    }
    return inboxItems
}