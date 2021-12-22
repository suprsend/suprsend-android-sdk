package app.suprsend.fcm

import app.suprsend.SSApi
import app.suprsend.base.Logger
import app.suprsend.notification.SSNotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SSFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Logger.i(TAG, "FCM From : ${remoteMessage.from}")
        SSNotificationHelper.showFCMNotification(applicationContext, remoteMessage)
    }

    override fun onNewToken(token: String) {
        Logger.i(TAG, "FCM Token : $token")
        val instance = SSApi.getInstanceFromCachedApiKey()
        instance?.getUser()?.setAndroidFcmPush(token)
    }

    companion object {
        const val TAG = "push_fcm"
    }
}
