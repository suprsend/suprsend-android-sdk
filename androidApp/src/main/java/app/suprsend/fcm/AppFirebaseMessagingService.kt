package app.suprsend.fcm

import android.util.Log
import app.suprsend.SuprSend
import app.suprsend.android.AppConstants
import app.suprsend.notification.SSNotificationHelper
import app.suprsend.notification.isSuprSendRemoteMessage
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

/**
 * To test the silent push notification use this receiver
 */
class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            if (remoteMessage.isSuprSendRemoteMessage()) {
                // Custom config event is sent in this custom fcm service for testing the custom payload / silent push
                val jsonObject = JSONObject()
                remoteMessage.data.keys.forEach { key ->
                    if (!key.equals("supr_send_n_pl"))
                        jsonObject.put(key, remoteMessage.data[key])
                }
                // Sending this earlier since flush will be done in showFCMNotification
                SuprSend.getInstance().trackEventAsync(EVENT_NOTIFICATION_CUSTOM_CONFIG, jsonObject)
                SSNotificationHelper.showFCMNotification(applicationContext, remoteMessage)
            }
        } catch (e: Exception) {
            Log.e(AppConstants.TAG, "onMessageReceived", e)
        }
    }

    override fun onNewToken(token: String) {
        try {
            Log.i(AppConstants.TAG, "FCM Token : $token")
            val instance = SuprSend.getInstance()
            instance.user.setAndroidFcmPushAsync(token)
        } catch (e: Exception) {
            Log.e(AppConstants.TAG, "onNewToken", e)
        }
    }

    companion object {
        const val EVENT_NOTIFICATION_CUSTOM_CONFIG = "notification_custom_config"
    }
}
