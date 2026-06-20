package app.suprsend

import app.suprsend.notification.NotificationActionVo

interface NotificationCallbackListener {
    fun onPushPayloadReceived(data: Map<String, String>)

    fun onNotificationClicked(notificationActionVo: NotificationActionVo, data: Map<String, String>) {}
}