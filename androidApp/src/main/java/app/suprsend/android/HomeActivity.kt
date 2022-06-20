package app.suprsend.android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import app.suprsend.android.databinding.ActivityHomeBinding
import app.suprsend.notification.SSNotificationHelper

class HomeActivity : AppCompatActivity() {

    lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val layoutManager = GridLayoutManager(this, 2)

        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return getSpanCount(position)
            }
        }
        binding.recyclerView.layoutManager = layoutManager

        binding.recyclerView.adapter = HomeRecyclerAdapter(AppCreator.homeItemsList)
        binding.settingsTv.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        CommonAnalyticsHandler.track("home_screen_viewed")
        SSNotificationHelper.showSSNotification(this,"{\"id\":\"01G603M5F62KWQZ97SF60V92HJ\",\"notificationGroupId\":\"01G603M5F5N76D1AHCH7HK4KT5\",\"channelId\":\"transactional\",\"channelName\":\"Transactional\",\"channelDescription\":\"Transactional\",\"channelShowBadge\":true,\"channelLockScreenVisibility\":\"PUBLIC\",\"channelImportance\":\"DEFAULT\",\"priority\":\"DEFAULT\",\"color\":\"#FFFFFF\",\"notificationTitle\":\"Innoviti Payments\",\"subText\":\"Link App\",\"shortDescription\":\"Innoviti Payments request Payment of Rs. 1.00\",\"longDescription\":\"Innoviti Payments request Payment of Rs. 1.00\",\"tickerText\":\"Innoviti Payments\",\"deeplink\":\"https://innoviti.com\",\"setGroupSummary\":false,\"autoCancel\":true,\"onGoing\":false,\"localOnly\":true,\"showWhenTimeStamp\":false,\"actions\":[{\"id\":\"209-test-pay-by-upi\",\"title\":\"Pay By UPI\",\"link\":\"upi://pay?pa=devandro789-1@okaxis\\u0026pn=PayApp\\u0026tr=123123123\\u0026am=1\"},{\"id\":\"209-test-pay-by-card\",\"title\":\"Pay By Card\",\"link\":\"https://innoviti.com\"}]}")
    }

    private fun getSpanCount(position: Int): Int {
        return if (position == 0)
            2
        else 1
    }
}
