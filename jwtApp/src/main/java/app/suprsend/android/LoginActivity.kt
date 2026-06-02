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

        binding.tenantIdEt.setText(AppCreator.getValue(AppConstants.PREF_TENANT_ID, BuildConfig.SS_TENANT_ID))
        binding.tenantIdEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                AppCreator.storeValue(AppConstants.PREF_TENANT_ID, s?.toString() ?: "")
            }
        })
        binding.loginTv.setOnClickListener {
            val email = binding.emailEt.text.toString()
            AppCreator.setEmail(this, email)
            CommonAnalyticsHandler.identify(email)
            CommonAnalyticsHandler.increment("login_count", 1)
            CommonAnalyticsHandler.setOnce("first_login_at", getReadableDate())
            CommonAnalyticsHandler.setSuperProperties("user_type", binding.userTypeSp.selectedItem.toString())
            AppCreator.tenantId = binding.tenantIdEt.text.toString()
            SuprSend.setTenantId(AppCreator.tenantId)
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
                SuprSend.setRefreshTokenCallback(RefreshTokenCallbackImpl())
            } else {
                SuprSend.setRefreshTokenCallback(null)
            }
        }

        CommonAnalyticsHandler.track("login_screen_viewed")
    }
}
