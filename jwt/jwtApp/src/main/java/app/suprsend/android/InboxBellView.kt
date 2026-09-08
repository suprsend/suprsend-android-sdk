package app.suprsend.android

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

class InboxBellView : FrameLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val bellView = LayoutInflater.from(context).inflate(R.layout.inbox_bell, this, true)

    init {
        applyStyle()
        updateCount()
    }

    fun updateCount() {
        val countTv = bellView.findViewById<TextView>(R.id.messagesCountTv)
        val bellCount = InboxViewModel.sharedOrNull()?.badge ?: 0
        countTv.visibility = if (bellCount == 0) View.GONE else View.VISIBLE
        countTv.text = if (bellCount > 99) {
            "99+"
        } else {
            bellCount.toString()
        }
    }

    private fun applyStyle() {
        // Matches iOS badge accent Color(red: 0.145, green: 0.388, blue: 0.922)
        val accent = Color.parseColor("#2570EB")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val notificationIv = bellView.findViewById<ImageView>(R.id.notificationIv)
            notificationIv.imageTintList = ColorStateList.valueOf(Color.BLACK)
        }

        val messagesCountTv = bellView.findViewById<TextView>(R.id.messagesCountTv)
        messagesCountTv.setTextColor(Color.WHITE)
        val messagesCountTvDrawable = messagesCountTv.background
        messagesCountTvDrawable?.setColorFilter(accent, PorterDuff.Mode.SRC_IN)
        messagesCountTv.background = messagesCountTvDrawable
    }
}
