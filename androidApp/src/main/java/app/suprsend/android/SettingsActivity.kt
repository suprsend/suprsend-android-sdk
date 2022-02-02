package app.suprsend.android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import app.suprsend.android.databinding.ActivitySettingsBinding
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

        binding.emailEt.setText(getValue("email"))
        binding.emailTv.setOnClickListener {
            val email = binding.emailEt.text.toString()
            CommonAnalyticsHandler.setEmail(email)
            storeValue("email", email)
        }
        binding.unSetEmailTv.setOnClickListener {
            val email = binding.emailEt.text.toString()
            CommonAnalyticsHandler.unSetEmail(email)
            storeValue("email", "")
            binding.emailEt.setText("")
        }

        binding.smsEt.setText(getValue("sms"))
        binding.smsTv.setOnClickListener {
            val sms = binding.smsEt.text.toString()
            CommonAnalyticsHandler.setSms(sms)
            storeValue("sms", sms)
        }
        binding.unSetSmsTv.setOnClickListener {
            val sms = binding.smsEt.text.toString()
            CommonAnalyticsHandler.unSetSms(sms)
            storeValue("sms", "")
            binding.smsEt.setText("")
        }

        binding.whatsAppEt.setText(getValue("whatsapp"))
        binding.whatsAppTv.setOnClickListener {
            val whatsapp = binding.whatsAppEt.text.toString()
            CommonAnalyticsHandler.setWhatsApp(whatsapp)
            storeValue("whatsapp", whatsapp)
        }
        binding.unSetWhatsAppTv.setOnClickListener {
            val email = binding.whatsAppEt.text.toString()
            CommonAnalyticsHandler.unSetWhatsApp(email)
            storeValue("whatsapp", "")
            binding.whatsAppEt.setText("")
        }

        binding.logoutTv.setOnClickListener {
            CommonAnalyticsHandler.unset("choices")
            CommonAnalyticsHandler.reset()
            CommonAnalyticsHandler.unSetSuperProperties("user_type")
            startActivity(Intent(this, WelcomeActivity::class.java))
            finishAffinity()
            AppCreator.setEmail(this, "")
        }

        binding.notificationTv.setOnClickListener {
            val intent = Intent(this, SSInboxActivity::class.java)
            if (ssInboxConfig != null)
                intent.putExtra("config", ssInboxConfig)
            startActivity(intent)
        }
        demoAppExecutorService.execute {
            // Todo : Testing url
            val responseStr = makeGetCall("https://abc.in/http/uploads/inbox_screen_theme.json")
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
                backButtonColor = response.optString("backButtonColor"),
                emptyScreenMessage = response.optString("emptyScreenMessage"),
                emptyScreenMessageTextColor = response.optString("emptyScreenMessageTextColor"),
                messageTextColor = response.optString("messageTextColor"),
                messageActionBgColor = response.optString("messageActionBgColor"),
                messageActionTextColor = response.optString("messageActionTextColor")
            )
        }
    }

    private fun getValue(key: String): String {
        return defaultSharedPreferences.getString(key, "") ?: ""
    }

    private fun storeValue(key: String, value: String) {
        defaultSharedPreferences.Edit {
            putString(key, value)
        }
    }
}
