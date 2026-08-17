package app.suprsend.base


internal class BasicDetails(
    val apiKey: String,
    val apiSecret: String,
    private val apiBaseUrl: String? = null,
    private val inboxBaseUrl: String? = null,
    private val inboxSocketUrl: String? = null
) {
    fun getApiBaseUrl(): String {
        if (apiBaseUrl == null) {
            return SSConstants.DEFAULT_BASE_API_URL
        }
        return apiBaseUrl.trim().removeSuffix("/")
    }

    fun getInboxBaseUrl(): String {
        if (inboxBaseUrl == null) {
            return SSConstants.DEFAULT_INBOX_BASE_API_URL
        }
        return inboxBaseUrl.trim().removeSuffix("/")
    }

    fun getInboxSocketUrl(): String {
        if (inboxSocketUrl == null) {
            return SSConstants.DEFAULT_INBOX_SOCKET_API_URL
        }
        return inboxSocketUrl.trim().removeSuffix("/")
    }

}
