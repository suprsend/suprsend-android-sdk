package app.suprsend.inbox

import android.annotation.SuppressLint
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
import app.suprsend.R
import app.suprsend.base.Logger
import app.suprsend.base.SSConstants
import app.suprsend.config.ConfigHelper
import app.suprsend.config.ConfigListener

class InboxBellView : FrameLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val bellView = LayoutInflater.from(context).inflate(R.layout.inbox_bell, this, true)

    private var configListener = object : ConfigListener {
        override fun onChange(key:String) {
            try {
                bellView.post {
                    try {
                        syncCount()
                    } catch (e: Exception) {
                        Log.e(SSInboxActivity.TAG, "", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(SSInboxActivity.TAG, "", e)
            }
        }
    }

    init {
        onStart()
    }


    fun initialize(
        distinctId: String,
        subscriberId: String,
        ssInboxConfig: SSInboxConfig? = null
    ) {
        if (ssInboxConfig != null)
            setThemeConfig(ssInboxConfig)
        syncCount()
        InboxHelper.fetchApiCall(
            distinctId = distinctId,
            subscriberId = subscriberId
        )
    }

    fun onStart() {
        ConfigHelper.setChangeListener(configListener)
        syncCount()
    }

    fun onStop() {
        ConfigHelper.removeListener(configListener)
    }

    @SuppressLint("SetTextI18n")
    private fun syncCount() {
        val countTv = bellView.findViewById<TextView>(R.id.messagesCountTv)
        val count = InboxHelper.getUnReadMessagesCount()
        countTv.visibility = if (count == 0) View.GONE else View.VISIBLE
        if (count > 99) {
            countTv.text = "99+"
        } else {
            countTv.text = count.toString()
        }
    }

    fun setThemeConfig(ssInboxConfig: SSInboxConfig) {
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val notificationIv = bellView.findViewById<ImageView>(R.id.notificationIv)
                notificationIv.imageTintList = ColorStateList.valueOf(Color.parseColor(ssInboxConfig.bellIconColor))
            }

            val messagesCountTv = bellView.findViewById<TextView>(R.id.messagesCountTv)
            messagesCountTv.setTextColor(Color.parseColor(ssInboxConfig.bellIconCountTextColor))

            val messagesCountTvDrawable = messagesCountTv.background
            messagesCountTvDrawable?.setColorFilter(Color.parseColor(ssInboxConfig.bellIconCountBgColor), PorterDuff.Mode.SRC_IN)
            messagesCountTv.background = messagesCountTvDrawable


        } catch (e: Exception) {
            Log.e(SSInboxActivity.TAG, "", e)
        }
    }

}