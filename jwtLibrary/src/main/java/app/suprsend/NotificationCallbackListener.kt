package app.suprsend

interface NotificationCallbackListener {
    fun onPushPayloadReceived(data: Map<String, String>)
}