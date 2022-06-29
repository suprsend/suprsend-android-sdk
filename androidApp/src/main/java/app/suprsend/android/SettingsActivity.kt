package app.suprsend.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import app.suprsend.android.databinding.ActivitySettingsBinding
import app.suprsend.event.Algo
import app.suprsend.inbox.SSInboxActivity
import app.suprsend.inbox.SSInboxConfig
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {

    lateinit var binding: ActivitySettingsBinding

    var ssInboxConfig: SSInboxConfig? = SSInboxConfig()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.emailEt.setText(getFromSp("email", "nikhilesh@suprsend.com"))
        binding.emailTv.clickWithThrottle {
            val email = binding.emailEt.text.toString()
            CommonAnalyticsHandler.setEmail(email)
            storeInSp("email", email)
        }
        binding.unSetEmailTv.clickWithThrottle {
            val email = binding.emailEt.text.toString()
            CommonAnalyticsHandler.unSetEmail(email)
            storeInSp("email", "")
            binding.emailEt.setText("")
        }

        binding.smsEt.setText(getFromSp("sms", "+918983364103"))
        binding.smsTv.clickWithThrottle {
            val sms = binding.smsEt.text.toString()
            CommonAnalyticsHandler.setSms(sms)
            storeInSp("sms", sms)
        }
        binding.unSetSmsTv.clickWithThrottle {
            val sms = binding.smsEt.text.toString()
            CommonAnalyticsHandler.unSetSms(sms)
            storeInSp("sms", "")
            binding.smsEt.setText("")
        }

        binding.whatsAppEt.setText(getFromSp("whatsapp", "+918983364103"))
        binding.whatsAppTv.clickWithThrottle {
            val whatsapp = binding.whatsAppEt.text.toString()
            CommonAnalyticsHandler.setWhatsApp(whatsapp)
            storeInSp("whatsapp", whatsapp)
        }
        binding.unSetWhatsAppTv.clickWithThrottle {
            val email = binding.whatsAppEt.text.toString()
            CommonAnalyticsHandler.unSetWhatsApp(email)
            storeInSp("whatsapp", "")
            binding.whatsAppEt.setText("")
        }

        binding.setSuprProperty.clickWithThrottle {
            CommonAnalyticsHandler.setSuperProperties(binding.setSuprPropertyKey.text.toString(), binding.setSuprPropertyValue.text.toString())
        }

        binding.unSetSuprProperty.clickWithThrottle {
            CommonAnalyticsHandler.unSetSuperProperties(binding.setSuprPropertyKey.text.toString())
        }

        binding.notificationTv.setOnClickListener {
            val intent = Intent(this, SSInboxActivity::class.java)
            intent.putExtra(SSInboxActivity.DISTINCT_ID, AppCreator.getEmail(this))
            intent.putExtra(SSInboxActivity.SUBSCRIBER_ID, HmacGeneratation()
                .hmacRawURLSafeBase64String(
                    AppCreator.getEmail(this),
                BuildConfig.INBOX_SECRET
                )
            )
            if (ssInboxConfig != null)
                intent.putExtra(SSInboxActivity.CONFIG, ssInboxConfig)
            startActivity(intent)
        }

        binding.logoutTv.clickWithThrottle {
            CommonAnalyticsHandler.unset("choices")
            CommonAnalyticsHandler.reset()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finishAffinity()
            AppCreator.setEmail(this, "")
        }

        fetchInboxTheme()
    }

    private fun fetchInboxTheme() {
        demoAppExecutorService.execute {
            // Todo : Testing url
            val responseStr = makeGetCall("https://freeappcreator.in/http/uploads/inbox_screen_theme.json")
            if (responseStr.isBlank()) {
                ssInboxConfig = null
                return@execute
            }

            val response = JSONObject(responseStr)
            ssInboxConfig = SSInboxConfig(
                statusBarColor = response.optString("statusBarColor"),
                navigationBarColor = response.optString("navigationBarColor"),
                toolbarBgColor = response.optString("toolbarBgColor"),
                toolbarTitle = response.optString("toolbarTitle"),
                toolbarTitleColor = response.optString("toolbarTitleColor"),
                screenBgColor = response.optString("screenBgColor"),
                cardBackgroundColor = response.optString("cardBackgroundColor"),
                backButtonColor = response.optString("backButtonColor"),
                emptyScreenMessage = response.optString("emptyScreenMessage"),
                emptyScreenMessageTextColor = response.optString("emptyScreenMessageTextColor"),
                messageTextColor = response.optString("messageTextColor")
            )
        }
    }
}

// This is generated here just for testing purpose we do not recommend doing this in mobile app instead this should be generated on server
private fun generateSubscriberId(distinctId: String): String? {
    val secret = BuildConfig.SS_SECRET
    return Algo.base64(Algo.generateHashWithHmac256(distinctId, secret))
}

fun View.clickWithThrottle(throttleTime: Long = 600L, action: () -> Unit) {
    this.setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long = 0

        override fun onClick(v: View) {
            if (System.currentTimeMillis() - lastClickTime < throttleTime) return
            else action()

            lastClickTime = System.currentTimeMillis()
        }
    })
}
