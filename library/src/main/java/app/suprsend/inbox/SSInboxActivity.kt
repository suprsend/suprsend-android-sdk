package app.suprsend.inbox

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import app.suprsend.R
import app.suprsend.base.safeDrawable

class SSInboxActivity : FragmentActivity() {

    lateinit var ssInboxConfig: SSInboxConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.inbox_activity)

            ssInboxConfig = intent.getParcelableExtra(CONFIG) ?: SSInboxConfig()

            val ssNotificationListFragment = SSInboxMessageListFragment()
            val headerLL = findViewById<View>(R.id.headerLL)
            val titleTv = findViewById<TextView>(R.id.titleTv)
            val startIconIv = findViewById<ImageView>(R.id.startIconIv)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.statusBarColor = Color.parseColor(ssInboxConfig.statusBarColor)
                window.navigationBarColor = Color.parseColor(ssInboxConfig.navigationBarColor)
            }

            headerLL.setBackgroundColor(Color.parseColor(ssInboxConfig.toolbarBgColor))
            titleTv.text = ssInboxConfig.toolbarTitle
            titleTv.setTextColor(Color.parseColor(ssInboxConfig.toolbarTitleColor))
            val imageDrawable: Drawable? = safeDrawable(resources = resources, drawableId = R.drawable.ic_ss_back)
            imageDrawable?.setColorFilter(Color.parseColor(ssInboxConfig.backButtonColor), PorterDuff.Mode.SRC_IN)
            startIconIv.setImageDrawable(imageDrawable)
            startIconIv.setOnClickListener {
                finish()
            }
            ssNotificationListFragment.arguments = Bundle().apply {
                putParcelable(CONFIG, ssInboxConfig)
                putString(SUBSCRIBER_ID, intent.getStringExtra(SUBSCRIBER_ID))
                putString(DISTINCT_ID, intent.getStringExtra(DISTINCT_ID))
            }
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragmentContainer, ssNotificationListFragment, "inbox_list_fragment")
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "", e)
        }
    }

    companion object {
        const val TAG = "ss_inbox"
        const val CONFIG = "config"
        const val SUBSCRIBER_ID = "subscriber_id"
        const val DISTINCT_ID = "distinct_id"
    }
}
