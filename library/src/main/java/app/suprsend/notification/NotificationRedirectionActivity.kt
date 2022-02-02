package app.suprsend.notification

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import app.suprsend.SSApi
import app.suprsend.SSApiInternal
import app.suprsend.base.Logger
import app.suprsend.base.SSConstants
import app.suprsend.base.mapToEnum
import app.suprsend.base.safeIntent
import org.json.JSONObject
import java.io.Serializable

class NotificationRedirectionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val activityExtras = intent.extras
            if (null == intent || activityExtras == null) {
                Logger.i(TAG, "meta data not received in $TAG")
                return
            }
            handleFlowPayload(activityExtras)
            finish()
        } catch (e: Exception) {
            Logger.i(TAG, "unable to handle meta data in $TAG")
            finish()
        }
    }

    private fun handleFlowPayload(activityExtras: Bundle) {
        if (activityExtras.containsKey(FLOW_NAME)) {
            when (activityExtras.getString(FLOW_NAME, "").mapToEnum<NotificationRedirection>()) {
                NotificationRedirection.NOTIFICATION_ACTION_CLICKED -> {
                    handleNotificationActionClicked(activityExtras)
                }
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
        instance?.flush()
    }

    private fun handleNotificationActionClicked(activityExtras: Bundle) {
        Logger.i(TAG, "Notification Action Clicked")
        val notificationActionVo = getNotificationActionVo(activityExtras)
        notificationActionVo ?: return

        val instance = SSApi.getInstanceFromCachedApiKey()
        SSApiInternal.saveTrackEventPayload(
            eventName = SSConstants.S_EVENT_NOTIFICATION_CLICKED,
            propertiesJO = JSONObject().apply {
                put("id", notificationActionVo.notificationId)
                if (notificationActionVo.notificationId != notificationActionVo.id) {
                    put("label_id", notificationActionVo.id)
                }
            }
        )
        instance?.flush()

        // Remove notification
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationActionVo.notificationActionType == NotificationActionType.BUTTON)
            notificationManager?.cancel((notificationActionVo.notificationId ?: "").hashCode())

        // Target intent
        val link = notificationActionVo.link
        val notificationActionIntent = safeIntent(link)
        notificationActionIntent?.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (notificationActionIntent != null)
            startActivity(notificationActionIntent)
    }

    private fun getNotificationActionVo(activityExtras: Bundle): NotificationActionVo? {
        return activityExtras.get(FLOW_PAYLOAD) as? NotificationActionVo
    }

    private fun getNotificationDismissVo(activityExtras: Bundle): NotificationDismissVo? {
        return activityExtras.get(FLOW_PAYLOAD) as? NotificationDismissVo
    }

    companion object {
        private const val TAG = "NRA"
        private const val FLOW_NAME = "flow_name"
        private const val FLOW_PAYLOAD = "flow_payload"

        internal fun getIntent(context: Context, notificationActionVo: NotificationActionVo? = null): Intent? {
            if (notificationActionVo?.link == null) {
                return context.packageManager.getLaunchIntentForPackage(context.packageName)
            }
            val bundle = Bundle()
            bundle.putString(FLOW_NAME, NotificationRedirection.NOTIFICATION_ACTION_CLICKED.name)
            bundle.putSerializable(FLOW_PAYLOAD, notificationActionVo)

            return Intent()
                .setClass(context, NotificationRedirectionActivity::class.java)
                .putExtras(bundle)
        }

        internal fun notificationDismissIntent(context: Context, notificationDismissVo: NotificationDismissVo): Intent {
            val bundle = Bundle()
            bundle.putString(FLOW_NAME, NotificationRedirection.NOTIFICATION_DISMISS.name)
            bundle.putSerializable(FLOW_PAYLOAD, notificationDismissVo)
            return Intent()
                .setClass(context, NotificationRedirectionActivity::class.java)
                .putExtras(bundle)
        }
    }
}

internal enum class NotificationRedirection {
    NOTIFICATION_ACTION_CLICKED, NOTIFICATION_DISMISS
}

internal data class NotificationDismissVo(
    val notificationId: String
) : Serializable
