package app.suprsend

import android.content.Context
import app.suprsend.notification.NotificationActionVo

interface NotificationCallbackListener {
    fun onPushPayloadReceived(context: Context, data: Map<String, String>)

    fun onNotificationClicked(notificationActionVo: NotificationActionVo, data: Map<String, String>) {}
}