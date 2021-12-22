package app.suprsend.xiaomi

import android.content.Context
import app.suprsend.SSApi
import app.suprsend.base.Logger
import app.suprsend.notification.SSNotificationHelper
import app.suprsend.notification.getToken
import com.xiaomi.mipush.sdk.MiPushCommandMessage
import com.xiaomi.mipush.sdk.MiPushMessage
import com.xiaomi.mipush.sdk.PushMessageReceiver

class SSXiaomiReceiver : PushMessageReceiver() {

    override fun onNotificationMessageArrived(context: Context?, miPushMessage: MiPushMessage?) {
        try {
            Logger.i(TAG, "Xiaomi NM From : ${miPushMessage?.messageId}")
            context ?: return
            miPushMessage ?: return
            SSNotificationHelper.showXiaomiNotification(context, miPushMessage)
        } catch (e: Exception) {
            Logger.e(TAG, "onNotificationMessageArrived exception ", e)
        }
    }

    override fun onReceivePassThroughMessage(context: Context?, miPushMessage: MiPushMessage?) {
        try {
            Logger.i(TAG, "Xiaomi PM From : ${miPushMessage?.messageId}")
            context ?: return
            miPushMessage ?: return
            SSNotificationHelper.showXiaomiNotification(context, miPushMessage)
        } catch (e: Exception) {
            Logger.e(TAG, "onReceivePassThroughMessage exception ", e)
        }
    }

    override fun onReceiveRegisterResult(context: Context?, miPushCommandMessage: MiPushCommandMessage?) {
        try {
            val token = miPushCommandMessage.getToken()

            if (token.isNullOrBlank())
                return
            Logger.i(TAG, "Xiaomi Token : $token")
            val instance = SSApi.getInstanceFromCachedApiKey()
            instance?.getUser()?.setAndroidXiaomiPush(token)
        } catch (e: Exception) {
            Logger.e(TAG, "onReceiveRegisterResult exception ", e)
        }
    }

    companion object {
        const val TAG = "push_xio"
    }
}
