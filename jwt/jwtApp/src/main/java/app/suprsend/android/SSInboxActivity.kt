package app.suprsend.android

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity

class SSInboxActivity : FragmentActivity() {

    private var unsubscribeInbox: (() -> Unit)? = null

    private val inboxViewModel = InboxViewModel.shared

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.inbox_activity) 

            val ssNotificationListFragment = SSInboxMessageListFragment()
            val titleTv = findViewById<TextView>(R.id.titleTv)
            val startIconIv = findViewById<ImageView>(R.id.startIconIv)
            val markAllAsReadTv = findViewById<TextView>(R.id.markAllAsReadTv)

            // Hardcoded to match iOS InboxScreen secondary background chrome.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.statusBarColor = Color.parseColor("#F2F2F7")
                window.navigationBarColor = Color.parseColor("#F2F2F7")
            }

            titleTv.setText(R.string.inbox_title)
            titleTv.setTextColor(Color.BLACK)
            markAllAsReadTv.setTextColor(Color.BLACK)
            val imageDrawable: Drawable? = safeDrawable(resources = resources, drawableId = R.drawable.ic_ss_back)
            imageDrawable?.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
            startIconIv.setImageDrawable(imageDrawable)
            startIconIv.setOnClickListener {
                finish()
            }

            bindMarkAllAsRead(markAllAsReadTv)

            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, ssNotificationListFragment, "inbox_list_fragment")
                .commit()
        } catch (e: Exception) {
            Log.e(AppConstants.TAG, "", e)
        }
    }

    private fun bindMarkAllAsRead(markAllAsReadTv: TextView) {
        fun updateEnabled() {
            val isEmpty = inboxViewModel.notifications.isEmpty()
            markAllAsReadTv.isEnabled = !isEmpty
            markAllAsReadTv.alpha = if (isEmpty) 0.5f else 1f
        }
        updateEnabled()
        unsubscribeInbox = inboxViewModel.subscribe { updateEnabled() }
        markAllAsReadTv.setOnClickListener {
            inboxViewModel.markAllAsRead()
        }
    }

    override fun onDestroy() {
        unsubscribeInbox?.invoke()
        unsubscribeInbox = null
        super.onDestroy()
    }
}
