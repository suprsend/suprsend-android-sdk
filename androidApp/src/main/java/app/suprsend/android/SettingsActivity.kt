package app.suprsend.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import app.suprsend.android.databinding.ActivitySettingsBinding
import app.suprsend.android.preference.UserPreferenceActivity

class SettingsActivity : AppCompatActivity() {

    lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Settings"
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.emailEt.setText(getValue("email", "nikhilesh@suprsend.com"))
        binding.emailTv.clickWithThrottle {
            val email = binding.emailEt.text.toString()
            CommonAnalyticsHandler.setEmail(email)
            storeValue("email", email)
        }
        binding.unSetEmailTv.clickWithThrottle {
            val email = binding.emailEt.text.toString()
            CommonAnalyticsHandler.unSetEmail(email)
            storeValue("email", "")
            binding.emailEt.setText("")
        }

        binding.smsEt.setText(getValue("sms", "+918983364103"))
        binding.smsTv.clickWithThrottle {
            val sms = binding.smsEt.text.toString()
            CommonAnalyticsHandler.setSms(sms)
            storeValue("sms", sms)
        }
        binding.unSetSmsTv.clickWithThrottle {
            val sms = binding.smsEt.text.toString()
            CommonAnalyticsHandler.unSetSms(sms)
            storeValue("sms", "")
            binding.smsEt.setText("")
        }

        binding.whatsAppEt.setText(getValue("whatsapp", "+918983364103"))
        binding.whatsAppTv.clickWithThrottle {
            val whatsapp = binding.whatsAppEt.text.toString()
            CommonAnalyticsHandler.setWhatsApp(whatsapp)
            storeValue("whatsapp", whatsapp)
        }
        binding.unSetWhatsAppTv.clickWithThrottle {
            val email = binding.whatsAppEt.text.toString()
            CommonAnalyticsHandler.unSetWhatsApp(email)
            storeValue("whatsapp", "")
            binding.whatsAppEt.setText("")
        }

        binding.setSuprProperty.clickWithThrottle {
            CommonAnalyticsHandler.setSuperProperties(binding.setSuprPropertyKey.text.toString(), binding.setSuprPropertyValue.text.toString())
        }

        binding.unSetSuprProperty.clickWithThrottle {
            CommonAnalyticsHandler.unSetSuperProperties(binding.setSuprPropertyKey.text.toString())
        }

        binding.preferredLanguageTv.clickWithThrottle {
            CommonAnalyticsHandler.setPreferredLanguage(binding.preferredLanguageEt.text.toString())
        }

        binding.logoutTv.clickWithThrottle {
            logout(false)
        }

        binding.logoutUnSubTv.clickWithThrottle {
            logout(true)
        }

        binding.userPreference.clickWithThrottle {
            val intent = Intent(this, UserPreferenceActivity::class.java)
            startActivity(intent)
        }
    }

    private fun logout(unSubscribeNotification: Boolean) {
        CommonAnalyticsHandler.unset("choices")
        CommonAnalyticsHandler.reset(unSubscribeNotification)
        startActivity(Intent(this, WelcomeActivity::class.java))
        finishAffinity()
        AppCreator.setEmail(this, "")
    }

    private fun getValue(key: String, default: String = ""): String {
        return defaultSharedPreferences.getString(key, default) ?: ""
    }

    private fun storeValue(key: String, value: String) {
        defaultSharedPreferences.Edit {
            putString(key, value)
        }
    }
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
