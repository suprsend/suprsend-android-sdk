package app.suprsend.xiaomi

import android.content.Context
import android.util.Log
import app.suprsend.SSApi
import app.suprsend.android.toKotlinJsonObject
import app.suprsend.fcm.SSAppFirebaseMessagingService
import app.suprsend.notification.SSNotificationHelper
import app.suprsend.notification.getToken
import app.suprsend.notification.isSuprSendPush
import com.xiaomi.mipush.sdk.MiPushCommandMessage
import com.xiaomi.mipush.sdk.MiPushMessage
import com.xiaomi.mipush.sdk.PushMessageReceiver

class SSAppXiaomiReceiver : PushMessageReceiver() {

    override fun onNotificationMessageArrived(context: Context?, miPushMessage: MiPushMessage?) {
        showNotification(context, miPushMessage)
    }

    override fun onReceivePassThroughMessage(context: Context?, miPushMessage: MiPushMessage?) {
        showNotification(context, miPushMessage)
    }

    override fun onReceiveRegisterResult(context: Context?, miPushCommandMessage: MiPushCommandMessage?) {
        try {
            val token = miPushCommandMessage.getToken()

            if (token.isNullOrBlank())
                return
            Log.i(TAG, "Xiaomi Token : $token")
            val instance = SSApi.getInstance()
            instance.getUser().setAndroidXiaomiPush(token)
        } catch (e: Exception) {
            Log.e(TAG, "onReceiveRegisterResult exception ", e)
        }
    }

    private fun showNotification(context: Context?, miPushMessage: MiPushMessage?) {
        try {
            context ?: return
            miPushMessage ?: return
            if (miPushMessage.isSuprSendPush()) {
                val jsonObject = miPushMessage.content.toKotlinJsonObject()
                jsonObject.remove("supr_send_n_pl")
                SSApi.getInstance().track(SSAppFirebaseMessagingService.EVENT_NOTIFICATION_CUSTOM_CONFIG, jsonObject)
                SSNotificationHelper.showXiaomiNotification(context, miPushMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onNotificationMessageArrived exception ", e)
        }
    }

    companion object {
        const val TAG = "app_push_xio"
    }
}
