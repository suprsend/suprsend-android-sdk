package app.suprsend.inbox

class SSInboxItemVo(
    val nID: String,
    val createdOn: Long? = null,
    var seenOn: Long? = null,

    // Message

    val header: String? = null,
    val text: String? = null,
    val url: String? = null,
    val imageUrl: String? = null
)
