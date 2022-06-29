package app.suprsend.inbox

import app.suprsend.base.safeJSONObject
import app.suprsend.base.safeString
import org.json.JSONArray

internal class SSInboxItemVo(
    val nID: String,
    val createdOn: Long? = null,
    var seenOn: Long? = null,

    //Message

    val header: String? = null,
    val text: String? = null,
    val url: String? = null,
    val imageUrl: String? = null
)

internal fun parseInboxItems(jsonArray: JSONArray?): List<SSInboxItemVo> {
    jsonArray ?: return listOf()
    val inboxItems = arrayListOf<SSInboxItemVo>()
    for (i in 0 until jsonArray.length()) {
        val jo = jsonArray.getJSONObject(i)
        val messageJO = jo.safeJSONObject("message")
        val id = jo.safeString("n_id") ?: ""
        inboxItems.add(
            SSInboxItemVo(
                nID = id,
                createdOn = jo.safeString("created_on")?.toLong(),
                seenOn = jo.safeString("seen_on")?.toLong(),

                //Message
                header = messageJO?.safeString("header"),
                text = messageJO?.safeString("text"),
                url = messageJO?.safeString("url"),
                imageUrl = messageJO?.safeString("image")
            )
        )
    }
    return inboxItems.sortedBy { item -> item.createdOn ?: 0 }
}