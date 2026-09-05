package app.suprsend.android

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import app.suprsend.SuprSend
import app.suprsend.android.databinding.ActivitySettingsBinding
import app.suprsend.android.preference.UserPreferenceActivity

class SettingsActivity : AppCompatActivity() {

    lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Settings"
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tenantIdEt.setText(AppCreator.getTenantId() ?: "")
        binding.tenantIdTv.clickWithThrottle {
            val tenantId = binding.tenantIdEt.text.toString()
            AppCreator.storeValue(AppConstants.PREF_TENANT_ID, tenantId)
            SuprSend.changeTenant(tenantId)
            InboxViewModel.shared.reconnectAndRefresh()
        }

        binding.emailEt.setText(AppCreator.getValue("email", "nikhilesh@suprsend.com"))
        binding.emailEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                AppCreator.storeValue("email", s?.toString() ?: "")
            }
        })
        binding.emailTv.clickWithThrottle {
            val email = binding.emailEt.text.toString()
            CommonAnalyticsHandler.setEmail(email)
        }
        binding.unSetEmailTv.clickWithThrottle {
            val email = binding.emailEt.text.toString()
            CommonAnalyticsHandler.unSetEmail(email)
        }

        binding.smsEt.setText(AppCreator.getValue("sms", "+918983364103"))
        binding.smsTv.clickWithThrottle {
            val sms = binding.smsEt.text.toString()
            CommonAnalyticsHandler.setSms(sms)
            AppCreator.storeValue("sms", sms)
        }
        binding.unSetSmsTv.clickWithThrottle {
            val sms = binding.smsEt.text.toString()
            CommonAnalyticsHandler.unSetSms(sms)
        }

        binding.whatsAppEt.setText(AppCreator.getValue("whatsapp", "+918983364103"))
        binding.whatsAppTv.clickWithThrottle {
            val whatsapp = binding.whatsAppEt.text.toString()
            CommonAnalyticsHandler.setWhatsApp(whatsapp)
            AppCreator.storeValue("whatsapp", whatsapp)
        }
        binding.unSetWhatsAppTv.clickWithThrottle {
            val email = binding.whatsAppEt.text.toString()
            CommonAnalyticsHandler.unSetWhatsApp(email)
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
            intent.putExtra("showOptOutChannels", binding.showOptOutChannelsCb.isChecked)
            startActivity(intent)
        }
        binding.inbox.clickWithThrottle {
            CommonAnalyticsHandler.set("inbox_visit_at", System.currentTimeMillis().toString())
            InboxViewModel.shared.resetBadge()
            AppCreator.startInboxActivity(this)
        }
    }

    private fun logout(unSubscribeNotification: Boolean) {
        InboxViewModel.reset()
        CommonAnalyticsHandler.unset("choices")
        CommonAnalyticsHandler.reset(unSubscribeNotification)
        startActivity(Intent(this, WelcomeActivity::class.java))
        finishAffinity()
        AppCreator.setEmail(this, "")
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
