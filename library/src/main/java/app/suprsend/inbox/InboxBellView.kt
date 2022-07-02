package app.suprsend.inbox

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import app.suprsend.R

class InboxBellView : FrameLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    private val bellView = LayoutInflater.from(context).inflate(R.layout.inbox_message_count, this, true)

    fun updateCount(distinctId: String, subscriberId: String, activity: Activity) {
        setCount()
        InboxHelper.fetchApiCall(
            distinctId = distinctId,
            subscriberId = subscriberId
        ) {
            activity.runOnUiThread {
                setCount()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setCount() {
        val countTv = bellView.findViewById<TextView>(R.id.messagesCountTv)
        val count = InboxHelper.getUnReadMessagesCount()
        countTv.visibility = if (count == 0) View.GONE else View.VISIBLE
        if (count > 99) {
            countTv.text = "99+"
        } else {
            countTv.text = count.toString()
        }
    }

}