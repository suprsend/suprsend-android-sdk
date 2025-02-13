package app.suprsend.android

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import app.suprsend.android.databinding.ActivityHomeBinding
import app.suprsend.inbox.InBoxErrorType
import app.suprsend.inbox.InboxNotification
import app.suprsend.inbox.InboxStore
import app.suprsend.inbox.InboxStoreListener
import app.suprsend.inbox.SuprsendInbox
import app.suprsend.inbox.socket.ConnectionState
import org.json.JSONObject

class HomeActivity : AppCompatActivity() {

    lateinit var binding: ActivityHomeBinding

    private var inboxBellView: InboxBellView? = null

    private lateinit var inboxThemeConfig: InboxThemeConfig
    private lateinit var inboxNotificationUpdateCallback: InboxStoreListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val jsonStr = readStringFromAsset("inbox_screen_theme.json")
        inboxThemeConfig = InboxThemeConfig(JSONObject(jsonStr))

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

        CommonAnalyticsHandler.initializeInbox()

        SuprsendInbox.getInstance().fetchBellCountAsync()

        inboxNotificationUpdateCallback = object : InboxStoreListener {
            override fun bellCount(bellCount: Int) {
                inboxBellView?.updateCount()
            }

            override fun loading(storeId: String, isLoading: Boolean) {
            }

            override fun onUpdate(inboxStore: InboxStore) {
            }

            override fun onError(id: String, errorType: InBoxErrorType, message: String, e: Exception?) {
            }

            override fun socket(connectionState: ConnectionState) {
            }

            override fun newNotification(notificationModel: InboxNotification) {
            }

        }

        SuprsendInbox.getInstance().registerCallback(inboxNotificationUpdateCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.let { safeMenu ->
            menuInflater.inflate(R.menu.home, safeMenu)
        }
        return true
    }

    override fun onStart() {
        super.onStart()
        SuprsendInbox.getInstance().openConnection()
    }

    override fun onStop() {
        super.onStop()
        // You can close connection by calling this method
        // SuprsendInbox.getInstance().closeConnection()
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let { safeMenu ->
            val notificationMenuItem = safeMenu.findItem(R.id.notificationMenu)
            inboxBellView = notificationMenuItem.actionView?.findViewById(R.id.inboxBellView)
            inboxBellView?.setOnClickListener {
                SuprsendInbox.getInstance().resetBellCountAsync()
                AppCreator.startInboxActivity(this)
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun getSpanCount(position: Int): Int {
        return if (position == 0)
            2
        else 1
    }

    override fun onDestroy() {
        super.onDestroy()
        SuprsendInbox.getInstance().unRegisterCallback(inboxNotificationUpdateCallback)
    }

}
