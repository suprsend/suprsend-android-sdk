package app.suprsend.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import app.suprsend.SuprSend
import app.suprsend.base.SSConstants
import app.suprsend.log.Logger
import app.suprsend.utils.mapToEnum
import org.json.JSONObject

class SSNotificationDismissBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            val activityExtras = intent?.extras
            if (null == intent || activityExtras == null) {
                Logger.i(SSConstants.TAG_SUPRSEND, "dismiss:meta data not received in extras")
                return
            }
            handleFlowPayload(activityExtras)
        } catch (e: Exception) {
            Logger.e(SSConstants.TAG_SUPRSEND, "dismiss:unable to handle meta data in handleFlowPayload", e)
        }
    }

    private fun handleFlowPayload(activityExtras: Bundle) {
        if (activityExtras.containsKey(NotificationRedirection.FLOW_NAME)) {
            when (activityExtras.getString(NotificationRedirection.FLOW_NAME, "").mapToEnum<NotificationRedirection>()) {
                NotificationRedirection.NOTIFICATION_DISMISS -> {
                    handleNotificationDismissClicked(activityExtras)
                }

                else -> {
                    // do nothing
                    Logger.i(SSConstants.TAG_SUPRSEND, "payload not handled")
                }
            }
        } else {
            Logger.i(SSConstants.TAG_SUPRSEND, "payload not found")
        }
    }

    private fun handleNotificationDismissClicked(activityExtras: Bundle) {
        Logger.i(SSConstants.TAG_SUPRSEND, "Notification dismissed")
        val notificationDismissVo = getNotificationDismissVo(activityExtras)
        notificationDismissVo ?: return
        // Notification Dismiss
        // Using instance since we have to schedule it on sdk thread
        SuprSend.getInstance().trackEventAsync(
            eventName = SSConstants.S_EVENT_NOTIFICATION_DISMISS,
            properties = JSONObject().apply {
                put("id", notificationDismissVo.notificationId)
            }
        )
    }

    private fun getNotificationDismissVo(activityExtras: Bundle): NotificationDismissVo? {
        return activityExtras.get(NotificationRedirection.FLOW_PAYLOAD) as? NotificationDismissVo
    }

    companion object {
        fun notificationDismissIntent(context: Context, notificationDismissVo: NotificationDismissVo): Intent {
            val bundle = Bundle()
            bundle.putString(NotificationRedirection.FLOW_NAME, NotificationRedirection.NOTIFICATION_DISMISS.name)
            bundle.putSerializable(NotificationRedirection.FLOW_PAYLOAD, notificationDismissVo)
            return Intent()
                .setClass(context, SSNotificationDismissBroadcastReceiver::class.java)
                .putExtras(bundle)
        }
    }
}