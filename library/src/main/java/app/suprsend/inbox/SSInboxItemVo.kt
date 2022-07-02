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