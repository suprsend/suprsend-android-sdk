package app.suprsend.android

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

class SSInboxActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.inbox_activity)

            val inboxThemeConfig = AppCreator.inboxThemeConfig

            val ssNotificationListFragment = SSInboxMessageListFragment()
            val headerLL = findViewById<View>(R.id.headerLL)
            val titleTv = findViewById<TextView>(R.id.titleTv)
            val startIconIv = findViewById<ImageView>(R.id.startIconIv)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.statusBarColor = Color.parseColor(inboxThemeConfig.statusBarColor)
                window.navigationBarColor = Color.parseColor(inboxThemeConfig.navigationBarColor)
            }

            headerLL.setBackgroundColor(Color.parseColor(inboxThemeConfig.toolbarBgColor))
            titleTv.text = inboxThemeConfig.toolbarTitle
            titleTv.setTextColor(Color.parseColor(inboxThemeConfig.toolbarTitleColor))
            val imageDrawable: Drawable? = safeDrawable(resources = resources, drawableId = R.drawable.ic_ss_back)
            imageDrawable?.setColorFilter(Color.parseColor(inboxThemeConfig.backButtonColor), PorterDuff.Mode.SRC_IN)
            startIconIv.setImageDrawable(imageDrawable)
            startIconIv.setOnClickListener {
                finish()
            }
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, ssNotificationListFragment, "inbox_list_fragment")
                .commit()
        } catch (e: Exception) {
            Log.e(AppConstants.TAG, "", e)
        }
    }
}
