package app.suprsend.android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import app.suprsend.android.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    lateinit var binding: ActivitySettingsBinding

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
