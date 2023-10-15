package app.suprsend.inbox

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.CountDownTimer
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import app.suprsend.R
import app.suprsend.base.Logger
import java.util.Locale

class InboxBellView : FrameLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val bellView = LayoutInflater.from(context).inflate(R.layout.inbox_bell, this, true)
    private var countDownTimer: CountDownTimer? = null
    private var subscriberId: String? = null
    private var distinctId: String? = null
    private var ssInboxConfig: SSInboxConfig? = null

    fun initialize(
        distinctId: String,
        subscriberId: String,
        ssInboxConfig: SSInboxConfig? = null
    ) {
        try {
            this.distinctId = distinctId
            this.subscriberId = subscriberId
            this.ssInboxConfig = ssInboxConfig
            if (ssInboxConfig != null)
                setThemeConfig(ssInboxConfig)
            syncCount()
        } catch (e: Exception) {
            Logger.e(SSInboxActivity.TAG, "", e)
        }
    }

    fun onStart() {
        try {
            countDownTimer?.cancel()
            countDownTimer = null
            val flushInterval = ssInboxConfig?.inboxFetchInterval ?: 10000
            Logger.i(SSInboxActivity.TAG, "Bell : Setting Periodic Fetch $flushInterval")
            countDownTimer = object : CountDownTimer(Long.MAX_VALUE, flushInterval) {
                override fun onTick(millisUntilFinished: Long) {
                    try {
                        Logger.i(SSInboxActivity.TAG, "Bell : Periodic fetch inbox messages")
                        val safeSubscriberId = this@InboxBellView.subscriberId ?: return
                        val safeDistinctId = this@InboxBellView.distinctId ?: return
                        InboxHelper.fetchApiCall(subscriberId = safeSubscriberId, distinctId = safeDistinctId) { _, showNewUpdatesAvailable ->
                            try {
                                post {
                                    try {
                                        syncCount(showNewUpdatesAvailable = showNewUpdatesAvailable)
                                    } catch (e: Exception) {
                                        Log.e(SSInboxActivity.TAG, "", e)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(SSInboxActivity.TAG, "", e)
                            }
                        }
                    } catch (e: Exception) {
                        Logger.e(SSInboxActivity.TAG, "", e)
                    }
                }

                override fun onFinish() {
                    start()
                }
            }.start()
        } catch (e: Exception) {
            Logger.e(SSInboxActivity.TAG, "", e)
        }
    }

    fun onStop() {
        try {
            Logger.i(SSInboxActivity.TAG, "Bell :Stopping Periodic Fetch")
            countDownTimer?.cancel()
            countDownTimer = null
        } catch (e: Exception) {
            Logger.e(SSInboxActivity.TAG, "", e)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun syncCount(showNewUpdatesAvailable: Boolean = false) {
        val countTv = bellView.findViewById<TextView>(R.id.messagesCountTv)
        val count = InboxHelper.getUnReadMessagesCount()
        countTv.visibility = if (count == 0) View.GONE else View.VISIBLE
        if (count > 99) {
            countTv.text = "99+"
        } else {
            countTv.text = count.toString()
        }
        val newUpdatesAvailableText = ssInboxConfig?.newUpdatesAvailableText ?: ""
        if (showNewUpdatesAvailable && newUpdatesAvailableText.isNotBlank()) {
            Toast.makeText(context, newUpdatesAvailableText, Toast.LENGTH_SHORT).apply {
                if (ssInboxConfig?.newUpdatesAvailablePosition?.toLowerCase(Locale.ROOT) == "top") {
                    setGravity(Gravity.TOP, 0, resources.getDimension(R.dimen.margin_70).toInt())
                }
                show()
            }
        }
    }

    private fun setThemeConfig(ssInboxConfig: SSInboxConfig) {
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
