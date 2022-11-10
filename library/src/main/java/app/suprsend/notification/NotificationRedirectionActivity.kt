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
        if (activityExtras.containsKey(NotificationRedirection.FLOW_NAME)) {
            when (activityExtras.getString(NotificationRedirection.FLOW_NAME, "").mapToEnum<NotificationRedirection>()) {
                NotificationRedirection.NOTIFICATION_CLICKED -> {
                    handleNotificationActionClicked(activityExtras)
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


    private fun handleNotificationActionClicked(activityExtras: Bundle) {
        Logger.i(TAG, "Notification Clicked")
        val notificationActionVo = getNotificationActionVo(activityExtras)
        notificationActionVo ?: return

        val instance = SSApi.getInstanceFromCachedApiKey()
        SSApiInternal.saveTrackEventPayload(
            eventName = SSConstants.S_EVENT_NOTIFICATION_CLICKED,
            propertiesJO = JSONObject().apply {
                put("id", notificationActionVo.notificationId)
                if(notificationActionVo.notificationActionType == NotificationActionType.BUTTON) {
                    put("label_id", notificationActionVo.id)
                }
            }
        )
        instance.flush()

        // Remove notification
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationActionVo.notificationActionType == NotificationActionType.BUTTON)
            notificationManager?.cancel((notificationActionVo.notificationId ?: "").hashCode())

        // Target intent
        val link = notificationActionVo.link
        val notificationActionIntent = if (!link.isNullOrBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(link))
        } else {
            packageManager.getLaunchIntentForPackage(packageName)
        }
        notificationActionIntent?.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(notificationActionIntent)
    }

    private fun getNotificationActionVo(activityExtras: Bundle): NotificationActionVo? {
        return activityExtras.get(NotificationRedirection.FLOW_PAYLOAD) as? NotificationActionVo
    }


    companion object {
        const val TAG = "NRA"

        fun getIntent(context: Context, notificationActionVo: NotificationActionVo): Intent {
            val bundle = Bundle()
            bundle.putString(NotificationRedirection.FLOW_NAME, NotificationRedirection.NOTIFICATION_CLICKED.name)
            bundle.putSerializable(NotificationRedirection.FLOW_PAYLOAD, notificationActionVo)
            return Intent()
                .setClass(context, NotificationRedirectionActivity::class.java)
                .putExtras(bundle)
        }
    }
}

enum class NotificationRedirection {
    NOTIFICATION_CLICKED, NOTIFICATION_DISMISS;

    companion object {
        const val FLOW_NAME = "flow_name"
        const val FLOW_PAYLOAD = "flow_payload"
    }
}

data class NotificationDismissVo(
    val notificationId: String
) : Serializable
