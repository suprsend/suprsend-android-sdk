package app.suprsend.android

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import app.suprsend.android.databinding.ActivityNotificationTestBinding
import app.suprsend.notification.SSNotificationHelper
import org.json.JSONObject

class NotificationTestActivity : AppCompatActivity() {

    lateinit var binding: ActivityNotificationTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        listOf(
            "Text" to "notification/text.json",
            "Image" to "notification/image.json",
            "Image Kit" to "notification/image_kit.json",
            "Lock Private" to "notification/lock_private.json",
            "Lock Public" to "notification/lock_public.json",
            "Lock Secret" to "notification/lock_secret.json",
            "M11" to "notification/m11.json",
            "M12" to "notification/m12.json",
            "M13" to "notification/m13.json",
            "M21" to "notification/m21.json",
            "M22" to "notification/m22.json",
            "M23" to "notification/m23.json",
            "Silent" to "notification/silent.json"
        ).forEach {
            val textView = AppCompatTextView(ContextThemeWrapper(this, R.style.btn))
            textView.text = it.first
            textView.textSize = 17f
            textView.gravity = android.view.Gravity.CENTER
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            layoutParams.topMargin = resources.getDimension(R.dimen.margin_10).toInt()
            layoutParams.marginStart = resources.getDimension(R.dimen.margin_10).toInt()
            layoutParams.marginEnd = resources.getDimension(R.dimen.margin_10).toInt()
            textView.clickWithThrottle {
                val notificationJO = JSONObject(readAssetFile(it.second))
                notificationJO.put("id", System.currentTimeMillis().toString())
                val data = hashMapOf(
                    AppConstants.NOTIFICATION_PAYLOAD to notificationJO.toString()
                )
                if (binding.customCollapsedView.isChecked) {
                    data["collapsed"] = "true"
                }
                if (binding.customExpandedView.isChecked) {
                    data["expanded"] = "true"
                }
                SSNotificationHelper.showSSNotification(
                    this,
                    data
                )
            }
            binding.linearLayout.addView(textView, layoutParams)
        }
    }

}