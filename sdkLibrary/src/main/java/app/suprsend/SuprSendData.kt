package app.suprsend

import app.suprsend.base.SSConstants

internal data class SuprSendData(
    var distinctId: String? = null,
    var tenantId: String? = null,
    var baseUrl: String = SSConstants.DEFAULT_BASE_API_URL,

    //Inbox
    var inboxBaseUrl: String = SSConstants.DEFAULT_INBOX_BASE_API_URL,
    var inboxSocketBaseUrl: String = SSConstants.DEFAULT_INBOX_SOCKET_API_URL,

    var publicApiKey: String? = null,
    var userTokenFetcher: UserTokenFetcher? = null,

    //Push notification
    var notificationCallbackListener: NotificationCallbackListener? = null,
)