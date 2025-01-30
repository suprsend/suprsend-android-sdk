package app.suprsend

internal data class SuprSendData(
    val host: String,
    var distinctId: String? = null,
    val publicApiKey: String? = null,
    val userTokenFetcher: UserTokenFetcher? = null,
    var notificationCallbackListener: NotificationCallbackListener? = null
)