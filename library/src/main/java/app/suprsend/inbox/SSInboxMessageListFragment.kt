package app.suprsend.inbox

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.suprsend.R
import app.suprsend.base.SSConstants
import app.suprsend.base.SdkAndroidCreator
import app.suprsend.config.ConfigHelper
import org.json.JSONArray

internal class SSInboxMessageListFragment : Fragment() {

    private var inboxRv: RecyclerView? = null
    private var emptyMessageTv: TextView? = null
    private var inboxLL: ViewGroup? = null
    private var countDownTimer: CountDownTimer? = null

    private lateinit var ssInboxConfig: SSInboxConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            ssInboxConfig = arguments?.getParcelable("config") ?: SSInboxConfig()
        } catch (e: Exception) {
            Log.e(SSInboxActivity.TAG, "onCreate", e)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.inbox_message_list_fragment, container, false)
        try {
            inboxLL = view.findViewById(R.id.inboxLL)
            inboxLL?.setBackgroundColor(Color.parseColor(ssInboxConfig.screenBgColor))
            val inboxRv = view.findViewById<RecyclerView>(R.id.inboxRv)
            emptyMessageTv = view.findViewById(R.id.emptyMessageTv)
            this.inboxRv = inboxRv
            initializeRecyclerView(inboxRv)
        } catch (e: Exception) {
            Log.e(SSInboxActivity.TAG, "onCreateView", e)
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        try {
            countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 10000) {
                override fun onTick(millisUntilFinished: Long) {
                    Log.i(SSInboxActivity.TAG, "Periodic fetch inbox messages")
                    val subscriberId = arguments?.getString(SSInboxActivity.SUBSCRIBER_ID, "") ?: ""
                    val distinctId = arguments?.getString(SSInboxActivity.DISTINCT_ID, "") ?: ""
                    InboxHelper.fetchApiCall(subscriberId = subscriberId, distinctId = distinctId) { isConnected ->
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
            Log.e(SSInboxActivity.TAG, "onStart", e)
        }

    }

    override fun onStop() {
        super.onStop()
        try {
            Log.i(SSInboxActivity.TAG, "Canceled periodic fetch inbox messages")
            countDownTimer?.cancel()
            countDownTimer = null
        } catch (e: Exception) {
            Log.e(SSInboxActivity.TAG, "onStop", e)
        }
    }

    private fun initializeRecyclerView(inboxRv: RecyclerView) {
        inboxRv.layoutManager = LinearLayoutManager(activity)
    }


    private fun setRecyclerViewData(isConnected: Boolean) {
        try {
            val response = ConfigHelper.get(SSConstants.INBOX_RESPONSE) ?: "[]"
            val items = InboxHelper.parseInboxItems(JSONArray(response))
            Log.i(SSInboxActivity.TAG, "Showing items : ${items.size}")
            if (items.isEmpty()) {
                if (isConnected) {
                    showEmptyScreen(ssInboxConfig.emptyScreenMessage)
                } else {
                    showEmptyScreen(getString(R.string.no_internet))
                }
            } else {
                emptyMessageTv?.visibility = View.GONE
                inboxRv?.visibility = View.VISIBLE
                inboxRv?.adapter = SSInboxMessageAdapter(message = items, ssInboxConfig = ssInboxConfig)
            }
        } catch (e: Exception) {
            Log.e(SSInboxActivity.TAG, "setRecyclerViewData", e)
        }
    }

    private fun showEmptyScreen(message: String) {
        emptyMessageTv?.text = message
        emptyMessageTv?.visibility = View.VISIBLE
        inboxRv?.visibility = View.GONE
    }
}