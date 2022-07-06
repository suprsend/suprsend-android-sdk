package app.suprsend.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import app.suprsend.android.databinding.ActivityHomeBinding
import app.suprsend.inbox.SSInboxActivity
import app.suprsend.inbox.SSInboxConfig
import org.json.JSONObject

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
        fetchInboxTheme()
        val ssInboxConfig = SSInboxConfig(getThemeJson())
        binding
            .inboxBellView
            .initialize(
                distinctId = AppCreator.getEmail(this),
                // This is generated here just for testing purpose we do not recommend doing this in mobile app instead this should be generated on server
                // We should not keep inbox secret at mobile end
                subscriberId = HmacGeneratation()
                    .hmacRawURLSafeBase64String(
                        AppCreator.getEmail(this),
                        BuildConfig.INBOX_SECRET
                    ),
                ssInboxConfig = ssInboxConfig
            )
        binding
            .inboxBellView
            .setOnClickListener {
                val intent = Intent(this, SSInboxActivity::class.java)
                intent.putExtra(SSInboxActivity.DISTINCT_ID, AppCreator.getEmail(this))
                intent.putExtra(
                    SSInboxActivity.SUBSCRIBER_ID,
                    HmacGeneratation()
                        .hmacRawURLSafeBase64String(
                            AppCreator.getEmail(this),
                            BuildConfig.INBOX_SECRET
                        )
                )
                intent.putExtra(SSInboxActivity.CONFIG, ssInboxConfig)
                startActivity(intent)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.inboxBellView.dispose()
    }

    private fun getSpanCount(position: Int): Int {
        return if (position == 0)
            2
        else 1
    }

    private fun fetchInboxTheme() {
        demoAppExecutorService.execute {
            val responseStr = makeGetCall("https://freeappcreator.in/http/uploads/inbox_screen_theme.json")
            defaultSharedPreferences.Edit {
                putString(SettingsActivity.APP_INBOX_THEME, responseStr)
            }
            if (responseStr.isBlank()) {
                return@execute
            }
            val config = SSInboxConfig(JSONObject(responseStr))
            runOnUiThread {
                binding.inboxBellView.setThemeConfig(config)
            }
        }
    }
}

fun Activity.getThemeJson(): JSONObject {
    return try {
        defaultSharedPreferences.getString(SettingsActivity.APP_INBOX_THEME, "{}")?.let { JSONObject(it) } ?: JSONObject()
    } catch (e: Exception) {
        JSONObject()
    }
}
