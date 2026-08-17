package app.suprsend.android

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import app.suprsend.android.databinding.ActivityLoginBinding
import app.suprsend.base.getReadableDate

class LoginActivity : AppCompatActivity() {

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.userTypeSp.adapter = getSpinnerAdapter(this, arrayListOf("Retailer", "User"))

        binding.loginTv.setOnClickListener {
            val email = binding.emailEt.text.toString()
            AppCreator.setEmail(this, email)
            CommonAnalyticsHandler.identify(email)
            CommonAnalyticsHandler.increment("login_count", 1)
            CommonAnalyticsHandler.setOnce("first_login_at", getReadableDate())
            CommonAnalyticsHandler.setSuperProperties("user_type", binding.userTypeSp.selectedItem.toString())
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }

        CommonAnalyticsHandler.track("login_screen_viewed")
    }
}
