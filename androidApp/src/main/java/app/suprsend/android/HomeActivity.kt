package app.suprsend.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import app.suprsend.android.databinding.ActivityHomeBinding
import app.suprsend.inbox.InboxMenuHandler
import app.suprsend.inbox.SSInboxActivity
import app.suprsend.inbox.SSInboxConfig
import org.json.JSONObject

class HomeActivity : AppCompatActivity() {

    lateinit var binding: ActivityHomeBinding

    var inboxMenuHandler: InboxMenuHandler? = null

    lateinit var ssInboxConfig: SSInboxConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ssInboxConfig = getThemeJson()?.let { SSInboxConfig(it) } ?: SSInboxConfig()

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
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.let { safeMenu ->
            menuInflater.inflate(R.menu.home, safeMenu)
        }
        return true
    }

    override fun onStart() {
        super.onStart()
        inboxMenuHandler?.onStart()
    }

    override fun onStop() {
        super.onStop()
        inboxMenuHandler?.onStop()
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let { safeMenu ->
            inboxMenuHandler = InboxMenuHandler(
                item = safeMenu.findItem(R.id.notificationMenu),
                distinctId = AppCreator.getEmail(this),
                // This is generated here just for testing purpose we do not recommend doing this in mobile app instead this should be generated on server
                // We should not keep inbox secret at mobile end
                subscriberId = HmacGeneratation()
                    .hmacRawURLSafeBase64String(
                        AppCreator.getEmail(this),
                        BuildConfig.INBOX_SECRET
                    ),
                ssInboxConfig = ssInboxConfig,
                onClickListener = View.OnClickListener {
                    val inboxIntent = Intent(this, SSInboxActivity::class.java)
                    inboxIntent.putExtra(SSInboxActivity.DISTINCT_ID, AppCreator.getEmail(this))
                    inboxIntent.putExtra(
                        SSInboxActivity.SUBSCRIBER_ID,
                        HmacGeneratation()
                            .hmacRawURLSafeBase64String(
                                AppCreator.getEmail(this),
                                BuildConfig.INBOX_SECRET
                            )
                    )
                    inboxIntent.putExtra(SSInboxActivity.CONFIG, ssInboxConfig)
                    startActivity(inboxIntent)
                }
            )
            inboxMenuHandler?.onStart()
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun getSpanCount(position: Int): Int {
        return if (position == 0)
            2
        else 1
    }

    private fun fetchInboxTheme() {
        demoAppExecutorService.execute {

            try {
                val responseStr = makeGetCall("https://freeappcreator.in/http/uploads/inbox_screen_theme.json")
                defaultSharedPreferences.Edit {
                    putString(SettingsActivity.APP_INBOX_THEME, responseStr)
                }
                if (responseStr.isBlank()) {
                    return@execute
                }
            } catch (e: Exception) {
                Log.e("home", "", e)
            }
        }
    }
}
