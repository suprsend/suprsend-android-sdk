package app.suprsend.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import app.suprsend.SSApi
import app.suprsend.SSApiInternal
import app.suprsend.base.Logger
import app.suprsend.base.SSConstants
import app.suprsend.base.mapToEnum
import org.json.JSONObject

class SSNotificationDismissBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            val activityExtras = intent?.extras
            if (null == intent || activityExtras == null) {
                Logger.i(TAG, "meta data not received in $TAG")
                return
            }
            handleFlowPayload(activityExtras)
        } catch (e: Exception) {
            Logger.i(TAG, "unable to handle meta data in $TAG")
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
                    Logger.i(TAG, "payload not handled")
                }
            }
        } else {
            Logger.i(TAG, "payload not found")
        }
    }

    private fun handleNotificationDismissClicked(activityExtras: Bundle) {
        Logger.i(TAG, "Notification dismissed")
        val notificationDismissVo = getNotificationDismissVo(activityExtras)
        notificationDismissVo ?: return
        val instance = SSApi.getInstanceFromCachedApiKey()
        SSApiInternal.saveTrackEventPayload(
            eventName = SSConstants.S_EVENT_NOTIFICATION_DISMISS,
            propertiesJO = JSONObject().apply {
                put("id", notificationDismissVo.notificationId)
            }
        )
        instance.flush()
    }

    private fun getNotificationDismissVo(activityExtras: Bundle): NotificationDismissVo? {
        return activityExtras.get(NotificationRedirection.FLOW_PAYLOAD) as? NotificationDismissVo
    }

    companion object {
        const val TAG = "NDR"
        internal fun notificationDismissIntent(context: Context, notificationDismissVo: NotificationDismissVo): Intent {
            val bundle = Bundle()
            bundle.putString(NotificationRedirection.FLOW_NAME, NotificationRedirection.NOTIFICATION_DISMISS.name)
            bundle.putSerializable(NotificationRedirection.FLOW_PAYLOAD, notificationDismissVo)
            return Intent()
                .setClass(context, SSNotificationDismissBroadcastReceiver::class.java)
                .putExtras(bundle)
        }
    }
}