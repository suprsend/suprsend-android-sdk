package app.suprsend.inbox

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.suprsend.R
import app.suprsend.base.Logger
import app.suprsend.base.SSConstants
import app.suprsend.base.SdkAndroidCreator
import app.suprsend.config.ConfigHelper
import org.json.JSONArray

internal class SSInboxMessageListFragment : Fragment() {

    private var inboxRv: RecyclerView? = null
    private var emptyMessageTv: TextView? = null
    private var inboxLL: ViewGroup? = null
    private var countDownTimer: CountDownTimer? = null
    private var newUpdatesAvailableTv: TextView? = null

    private lateinit var ssInboxConfig: SSInboxConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            ssInboxConfig = arguments?.getParcelable("config") ?: SSInboxConfig()
        } catch (e: Exception) {
            Logger.e(SSInboxActivity.TAG, "onCreate", e)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.inbox_message_fragment, container, false)
        try {
            inboxLL = view.findViewById(R.id.inboxLL)
            inboxLL?.setBackgroundColor(Color.parseColor(ssInboxConfig.screenBgColor))

            emptyMessageTv = view.findViewById(R.id.emptyMessageTv)

            initializeRecyclerView(view)
            initializeNewUpdateAvailable(view)
        } catch (e: Exception) {
            Logger.e(SSInboxActivity.TAG, "onCreateView", e)
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        try {
            countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 10000) {
                override fun onTick(millisUntilFinished: Long) {
                    Logger.i(SSInboxActivity.TAG, "Periodic fetch inbox messages")
                    val subscriberId = arguments?.getString(SSInboxActivity.SUBSCRIBER_ID, "") ?: ""
                    val distinctId = arguments?.getString(SSInboxActivity.DISTINCT_ID, "") ?: ""
                    InboxHelper.fetchApiCall(subscriberId = subscriberId, distinctId = distinctId, messagesSeen = true) { isConnected ->
                        activity?.runOnUiThread {
                            setRecyclerViewData(isConnected)
                        }
                    }
                }

                override fun onFinish() {
                    start()
                }
            }.start()
            setRecyclerViewData(SdkAndroidCreator.networkInfo.isConnected())
        } catch (e: Exception) {
            Logger.e(SSInboxActivity.TAG, "onStart", e)
        }

    }

    override fun onStop() {
        super.onStop()
        try {
            Logger.i(SSInboxActivity.TAG, "Canceled periodic fetch inbox messages")
            countDownTimer?.cancel()
            countDownTimer = null
        } catch (e: Exception) {
            Logger.e(SSInboxActivity.TAG, "onStop", e)
        }
    }

    private fun initializeNewUpdateAvailable(view: View) {
        newUpdatesAvailableTv = view.findViewById(R.id.newUpdatesAvailableTv)
        newUpdatesAvailableTv?.setOnClickListener {
            newUpdatesAvailableTv?.visibility = View.GONE
        }
        newUpdatesAvailableTv?.text = ssInboxConfig.newUpdatesAvailableText
        newUpdatesAvailableTv?.setTextColor(Color.parseColor(ssInboxConfig.newUpdatesAvailableTextColor))
        val layoutParams = newUpdatesAvailableTv?.layoutParams as? FrameLayout.LayoutParams
        if (ssInboxConfig.newUpdatesAvailablePosition == "top") {
            layoutParams?.topMargin = resources.getDimension(R.dimen.margin_10).toInt()
            layoutParams?.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        } else {
            layoutParams?.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            layoutParams?.bottomMargin = resources.getDimension(R.dimen.margin_10).toInt()
        }
    }

    private fun initializeRecyclerView(view: View) {
        inboxRv = view.findViewById(R.id.inboxRv)
        inboxRv?.layoutManager = LinearLayoutManager(activity)
    }

    private fun setRecyclerViewData(isConnected: Boolean) {
        try {
            val response = ConfigHelper.get(SSConstants.INBOX_RESPONSE) ?: "[]"
            val items = InboxHelper.parseInboxItems(JSONArray(response))
            Logger.i(SSInboxActivity.TAG, "Showing Messages : ${items.size}")
            if (items.isEmpty()) {
                if (isConnected) {
                    showEmptyScreen(ssInboxConfig.emptyScreenMessage)
                } else {
                    showEmptyScreen(getString(R.string.no_internet))
                }
            } else {
                emptyMessageTv?.visibility = View.GONE
                inboxRv?.visibility = View.VISIBLE
                val prevAdapter = inboxRv?.adapter as? SSInboxMessageAdapter
                if (prevAdapter != null && prevAdapter.message.size != items.size) {
                    showNewUpdatesAvailable()
                }
                inboxRv?.adapter = SSInboxMessageAdapter(message = items, ssInboxConfig = ssInboxConfig)
            }
        } catch (e: Exception) {
            Logger.e(SSInboxActivity.TAG, "setRecyclerViewData", e)
        }
    }

    private fun showEmptyScreen(message: String) {
        emptyMessageTv?.text = message
        emptyMessageTv?.visibility = View.VISIBLE
        inboxRv?.visibility = View.GONE
    }

    private fun showNewUpdatesAvailable() {
        newUpdatesAvailableTv?.visibility = View.VISIBLE
        newUpdatesAvailableTv?.postDelayed({
            newUpdatesAvailableTv?.visibility = View.GONE
        }, 5000)
    }
}