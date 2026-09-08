package app.suprsend.android

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import app.suprsend.android.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    lateinit var binding: ActivityHomeBinding

    private var inboxBellView: InboxBellView? = null

    private var unsubscribeInbox: (() -> Unit)? = null

    private val inboxViewModel = InboxViewModel.shared

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
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.let { safeMenu ->
            menuInflater.inflate(R.menu.home, safeMenu)
        }
        return true
    }

    override fun onStart() {
        super.onStart()
        unsubscribeInbox = inboxViewModel.subscribe {
            inboxBellView?.updateCount()
        }
        inboxBellView?.updateCount()
    }

    override fun onStop() {
        super.onStop()
        unsubscribeInbox?.invoke()
        unsubscribeInbox = null
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let { safeMenu ->
            val notificationMenuItem = safeMenu.findItem(R.id.notificationMenu)
            inboxBellView = notificationMenuItem.actionView?.findViewById(R.id.inboxBellView)
            inboxBellView?.updateCount()
            inboxBellView?.setOnClickListener {
                inboxViewModel.resetBadge()
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
}
