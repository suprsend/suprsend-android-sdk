package app.suprsend.fcm

import app.suprsend.SSApi
import app.suprsend.base.Logger
import app.suprsend.notification.SSNotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SSFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            SSNotificationHelper.showFCMNotification(applicationContext, remoteMessage)
        } catch (e: Exception) {
            Logger.e(TAG, "onMessageReceived", e)
        }
    }

    override fun onNewToken(token: String) {
        try {
            Logger.i(TAG, "FCM Token : $token")
            val instance = SSApi.getInstanceFromCachedApiKey()
            instance.getUser().setAndroidFcmPush(token)
        } catch (e: Exception) {
            Logger.e(TAG, "onNewToken", e)
        }
    }

    companion object {
        const val TAG = "push_fcm"
    }
}
