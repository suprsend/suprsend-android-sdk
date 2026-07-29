package app.suprsend.android

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import app.suprsend.SuprSend
import app.suprsend.android.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.userTypeSp.adapter = getSpinnerAdapter(this, arrayListOf("Retailer", "User"))

        binding.tenantIdEt.setText(AppCreator.getTenantId()?:"")
        binding.loginTv.setOnClickListener {
            val email = binding.emailEt.text.toString()
            AppCreator.setEmail(this, email)
            AppCreator.storeValue(AppConstants.PREF_TENANT_ID, binding.tenantIdEt.text.toString())
            CommonAnalyticsHandler.identify(email, AppCreator.getTenantId())
            CommonAnalyticsHandler.increment("login_count", 1)
            CommonAnalyticsHandler.setOnce("first_login_at", getReadableDate())
            CommonAnalyticsHandler.setSuperProperties("user_type", binding.userTypeSp.selectedItem.toString())
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }

        val jwtTokenBoolean = defaultSharedPreferences.getBoolean("jwtToken", true)
        binding.jwtTokenCb.isChecked = jwtTokenBoolean

        binding.jwtTokenCb.setOnCheckedChangeListener { _, isChecked ->
            defaultSharedPreferences.edit().apply {
                putBoolean("jwtToken", isChecked)
                apply()
            }
            if (isChecked) {
                SuprSend.setRefreshUserToken(RefreshUserTokenCallbackImpl())
            } else {
                SuprSend.setRefreshUserToken(null)
            }
        }

        CommonAnalyticsHandler.track("login_screen_viewed")
    }
}
