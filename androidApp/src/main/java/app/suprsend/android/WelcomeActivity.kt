package app.suprsend.android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import app.suprsend.android.databinding.ActivityWelcomeBinding
import app.suprsend.notification.NotificationPermissionHelper.isNotificationPermissionGranted
import app.suprsend.notification.NotificationPermissionHelper.requestNotificationPermission
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

class WelcomeActivity : AppCompatActivity() {

    lateinit var binding: ActivityWelcomeBinding

    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (!isGranted) {
            // You can show a dialog which explains the intent of this permission request of how it is important for certain features of your app to work
            AlertDialog.Builder(this)
                .setView(R.layout.notification_permission_desc)
                .setTitle(getString(R.string.app_name))
                .setPositiveButton("Proceed") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val uri: Uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
                .setNegativeButton("Deny") { _, _ ->
                }.show()
        }
    }

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activityResultLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
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

}
