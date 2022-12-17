package app.suprsend.android

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import app.suprsend.android.databinding.ActivityWelcomeBinding
import app.suprsend.notification.NotificationPermissionHelper.isNotificationPermissionGranted
import app.suprsend.notification.NotificationPermissionHelper.requestNotificationPermission
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

class WelcomeActivity : AppCompatActivity() {

    lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeSdk()

        val email = AppCreator.getEmail(this)
        if (email.isNotBlank()) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = WelcomeAdapter(
            layoutInflater, (1..10).map { index ->
                WelcomeVo(
                    id = index.toString(),
                    url = AppCreator.getProductImage()
                )
            }
        )

        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            @SuppressLint("SetTextI18n")
            override fun onPageSelected(position: Int) {
                val analyticsPos = position + 1
                binding.pageNumTv.text = "Page $analyticsPos"
                CommonAnalyticsHandler.track("walkthrough_viewed", JSONObject().apply {
                    put("position", analyticsPos)
                })
                CommonAnalyticsHandler.set("welcome_page_position", "$analyticsPos")
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })

        CommonAnalyticsHandler.track("welcome_screen_viewed")

        val analyticsPos = 1

        CommonAnalyticsHandler.track("walkthrough_viewed", JSONObject().apply {
            put("position", analyticsPos)
        })
        CommonAnalyticsHandler.set("welcome_page_position", "$analyticsPos")

        binding.loginTv.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        subscribeToTopic()
        requestNotificationPermission(NOTIFICATION_PERMISSION_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE && !isNotificationPermissionGranted()) {
            // You can show a dialog which explains the intent of this permission request how it is important
            // for certain features to work and re-request the permission by calling requestNotificationPermission()
            AlertDialog.Builder(this)
                .setView(R.layout.notification_permission_desc)
                .setPositiveButton("Grant") { _, _ ->
                    requestNotificationPermission(NOTIFICATION_PERMISSION_REQUEST_CODE)
                }
                .setNegativeButton("Deny") { _, _ ->
                }.show()
        }
    }

    private fun subscribeToTopic() {
        val topicName = "all_users"
        FirebaseMessaging
            .getInstance()
            .subscribeToTopic(topicName)
            .addOnCompleteListener { task ->
                var msg = "Subscribed to topic $topicName"
                if (!task.isSuccessful) {
                    msg = "Failed to subscribe to topic : $topicName"
                }
                Log.d("firebase", msg)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
    }

    private fun initializeSdk() {
        CommonAnalyticsHandler.initialize(applicationContext)
    }

    companion object {
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 12345678
    }
}
