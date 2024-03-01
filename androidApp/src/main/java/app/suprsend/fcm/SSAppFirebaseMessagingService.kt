package app.suprsend.fcm

import android.util.Log
import app.suprsend.SSApi
import app.suprsend.notification.SSNotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

class SSAppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            //Custom config event is sent in this custom fcm service for testing the custom payload
            val jsonObject = JSONObject()
            remoteMessage.data.keys.forEach {key->
                if (!key.equals("supr_send_n_pl"))
                    jsonObject.put(key, remoteMessage.data[key])
            }
            SSApi.getInstance().track(EVENT_NOTIFICATION_CUSTOM_CONFIG,jsonObject)
            SSNotificationHelper.showFCMNotification(applicationContext, remoteMessage)
        } catch (e: Exception) {
            Log.e(TAG, "onMessageReceived", e)
        }
    }

    override fun onNewToken(token: String) {
        try {
            Log.i(TAG, "FCM Token : $token")
            val instance = SSApi.getInstance()
            instance.getUser().setAndroidFcmPush(token)
        } catch (e: Exception) {
            Log.e(TAG, "onNewToken", e)
        }
    }

    companion object {
        const val TAG = "push_fcm"
        const val EVENT_NOTIFICATION_CUSTOM_CONFIG = "notification_custom_config"
    }
}
