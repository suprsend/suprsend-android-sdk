package app.suprsend.fcm

import app.suprsend.SuprSend
import app.suprsend.SSInternal
import app.suprsend.base.SSConstants
import app.suprsend.log.Logger
import app.suprsend.notification.SSNotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SSFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            SSNotificationHelper.showFCMNotification(applicationContext, remoteMessage)
            SSInternal.suprSendData.notificationCallbackListener?.onPushPayloadReceived(remoteMessage.data)
        } catch (e: Exception) {
            Logger.e(SSConstants.TAG_SUPRSEND, "onMessageReceived", e)
        }
    }

    override fun onNewToken(token: String) {
        try {
            Logger.i(SSConstants.TAG_SUPRSEND, "FCM Token : $token")
            val instance = SuprSend.getInstance()
            instance.user.setAndroidFcmPushAsync(token)
        } catch (e: Exception) {
            Logger.e(SSConstants.TAG_SUPRSEND, "onNewToken", e)
        }
    }

    companion object {
        const val TAG = "push_fcm"
    }
}
