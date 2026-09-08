package app.suprsend.feed

/**
 * Default values for [Feed] configuration and requests.
 */
internal object FeedConstants {
    const val PAGE_SIZE = 20
    const val TENANT_ID = "default"
    const val MAX_PAGE_SIZE = 100
    val STORE = IStore(storeId = "\$suprsend_default_store", label = "")
    const val DEFAULT_API_HOST = "https://inboxs.live"
    const val DEFAULT_SOCKET_HOST = "https://betainbox.suprsend.com"
    const val EXPIRY_CHECK_INTERVAL_IN_MILLIS = 30_000L
    const val BADGE = "badge"
}
