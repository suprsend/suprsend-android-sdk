package app.suprsend.notification

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import app.suprsend.SSInternal
import app.suprsend.SuprSend
import app.suprsend.base.SSConstants
import app.suprsend.log.Logger
import app.suprsend.utils.mapToEnum
import org.json.JSONObject
import java.io.Serializable

class NotificationRedirectionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val activityExtras = intent?.extras
            if (activityExtras == null) {
                Logger.i(SSConstants.TAG_SUPRSEND, "NRA:meta data not received in activityExtras")
                return
            }
            handleFlowPayload(activityExtras)
        } catch (e: Exception) {
            Logger.e(SSConstants.TAG_SUPRSEND, "NRA:unable to handle meta data in handleFlowPayload", e)
        } finally {
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
                    Logger.i(SSConstants.TAG_SUPRSEND, "payload not handled")
                }
            }
        } else {
            Logger.i(SSConstants.TAG_SUPRSEND, "payload not found")
        }
    }


    private fun handleNotificationActionClicked(activityExtras: Bundle) {
        Logger.i(SSConstants.TAG_SUPRSEND, "Notification Clicked")
        val notificationActionVo = getNotificationActionVo(activityExtras)
        notificationActionVo ?: return

        SSInternal.suprSendData.notificationCallbackListener?.onNotificationClicked(
            notificationActionVo,
            getPushData(activityExtras)
        )

        // Notification Clicked
        // Using instance since we have to schedule it on sdk thread
        SuprSend.getInstance().trackEventAsync(
            eventName = SSConstants.S_EVENT_NOTIFICATION_CLICKED,
            properties = JSONObject().apply {
                put("id", notificationActionVo.notificationId)
                if (notificationActionVo.notificationActionType == NotificationActionType.BUTTON) {
                    put("label_id", notificationActionVo.id)
                }
            }
        )


        // Remove notification
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationActionVo.notificationActionType == NotificationActionType.BUTTON)
            notificationManager?.cancel((notificationActionVo.notificationId ?: "").hashCode())

        // Target intent — must run in its own task. Otherwise the destination
        // (browser / deeplink target) gets parented to NRA's task and ends up in Recents alongside it.
        val link = notificationActionVo.link
        val notificationActionIntent = if (!link.isNullOrBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(link))
        } else {
            packageManager.getLaunchIntentForPackage(packageName)
        }
        notificationActionIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        notificationActionIntent ?: return
        startActivity(notificationActionIntent)
    }

    private fun getNotificationActionVo(activityExtras: Bundle): NotificationActionVo? {
        return activityExtras.get(NotificationRedirection.FLOW_PAYLOAD) as? NotificationActionVo
    }

    private fun getPushData(activityExtras: Bundle): Map<String, String> {
        return try {
            @Suppress("DEPRECATION")
            val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activityExtras.getSerializable(NotificationRedirection.FLOW_PUSH_DATA, HashMap::class.java)
            } else {
                activityExtras.getSerializable(NotificationRedirection.FLOW_PUSH_DATA)
            }
            @Suppress("UNCHECKED_CAST")
            (raw as? HashMap<String, String>) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    companion object {

        fun getIntent(
            context: Context,
            notificationActionVo: NotificationActionVo,
            pushData: Map<String, String>? = null
        ): Intent {
            val bundle = Bundle()
            bundle.putString(NotificationRedirection.FLOW_NAME, NotificationRedirection.NOTIFICATION_CLICKED.name)
            bundle.putSerializable(NotificationRedirection.FLOW_PAYLOAD, notificationActionVo)
            if (!pushData.isNullOrEmpty()) {
                bundle.putSerializable(NotificationRedirection.FLOW_PUSH_DATA, HashMap(pushData))
            }
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
        const val FLOW_PUSH_DATA = "flow_push_data"
    }
}

data class NotificationDismissVo(
    val notificationId: String
) : Serializable
