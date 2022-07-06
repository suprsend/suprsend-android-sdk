package app.suprsend.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import app.suprsend.android.databinding.ActivitySettingsBinding
import app.suprsend.inbox.SSInboxConfig

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

        binding.logoutTv.clickWithThrottle {
            CommonAnalyticsHandler.unset("choices")
            CommonAnalyticsHandler.reset()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finishAffinity()
            AppCreator.setEmail(this, "")
        }
    }

    companion object {
        const val APP_INBOX_THEME = "inbox_theme"
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
