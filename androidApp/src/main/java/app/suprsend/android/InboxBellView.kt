package app.suprsend.android

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import app.suprsend.inbox.SuprsendInbox

class InboxBellView : FrameLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val bellView = LayoutInflater.from(context).inflate(R.layout.inbox_bell, this, true)

    init {
        setThemeConfig()
        updateCount()
    }

    fun updateCount() {
        val countTv = bellView.findViewById<TextView>(R.id.messagesCountTv)
        val bellCount = SuprsendInbox.getInstance().getBellCount()
        countTv.visibility = if (bellCount == 0) View.GONE else View.VISIBLE
        countTv.text = if (bellCount > 99) {
            "99+"
        } else {
            bellCount.toString()
        }
    }

    private fun setThemeConfig() {
        try {
            val inboxThemeConfig = AppCreator.inboxThemeConfig
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val notificationIv = bellView.findViewById<ImageView>(R.id.notificationIv)
                notificationIv.imageTintList = ColorStateList.valueOf(Color.parseColor(inboxThemeConfig.bellIconColor))
            }

            val messagesCountTv = bellView.findViewById<TextView>(R.id.messagesCountTv)
            messagesCountTv.setTextColor(Color.parseColor(inboxThemeConfig.bellIconCountTextColor))

            val messagesCountTvDrawable = messagesCountTv.background
            messagesCountTvDrawable?.setColorFilter(Color.parseColor(inboxThemeConfig.bellIconCountBgColor), PorterDuff.Mode.SRC_IN)
            messagesCountTv.background = messagesCountTvDrawable
        } catch (e: Exception) {
            Log.e(AppConstants.TAG, "", e)
        }
    }
}
