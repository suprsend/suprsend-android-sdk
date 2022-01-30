package app.suprsend.inbox

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentActivity
import app.suprsend.R
import app.suprsend.base.safeDrawable

//Todo : Verify all configs
class SSInboxActivity : FragmentActivity() {

    lateinit var ssInboxConfig: SSInboxConfig
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.inbox_activity)

        ssInboxConfig = intent.getParcelableExtra("config") ?: SSInboxConfig()

        val ssNotificationListFragment = SSInboxMessageListFragment()
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = ssInboxConfig.toolbarTitle
        toolbar.setTitleTextColor(Color.parseColor(ssInboxConfig.toolbarTitleColor))
        toolbar.setBackgroundColor(Color.parseColor(ssInboxConfig.toolbarBgColor))
        val backIconDrawable: Drawable? = safeDrawable(resources = resources, drawableId = R.drawable.ic_ss_back)
        backIconDrawable?.setColorFilter(Color.parseColor(ssInboxConfig.backButtonColor), PorterDuff.Mode.SRC_IN)
        toolbar.navigationIcon = backIconDrawable
        toolbar.setNavigationOnClickListener {
            finish()
        }
        ssNotificationListFragment.arguments = Bundle().apply {
            putParcelable("config", ssInboxConfig)
        }
        supportFragmentManager
            .beginTransaction()
            .add(R.id.fragmentContainer, ssNotificationListFragment, "inbox_list_fragment")
            .commit()
    }
}