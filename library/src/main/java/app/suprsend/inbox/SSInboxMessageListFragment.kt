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
import app.suprsend.BuildConfig
import app.suprsend.R
import app.suprsend.base.SSConstants
import app.suprsend.base.SdkAndroidCreator
import app.suprsend.base.appExecutorService
import app.suprsend.base.generateSignature
import app.suprsend.base.makeHttpRequest
import app.suprsend.base.safeString
import app.suprsend.config.ConfigHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

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
                    fetchApiCall()
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

    private fun fetchApiCall() {
        if (!SdkAndroidCreator.networkInfo.isConnected()) {
            activity?.runOnUiThread {
                setRecyclerViewData(false)
            }
            return
        }
        appExecutorService.execute {
            try {
                var after = ConfigHelper.get(SSConstants.INBOX_FETCH_TIME)?.toLong()
                if (after == null && after != -1L) {
                    val dayTime: Long = 1000 * 60 * 60 * 24
                    after = System.currentTimeMillis() - (30 * dayTime)
                }
                val baseUrl = ConfigHelper.get(SSConstants.CONFIG_API_BASE_URL) ?: SSConstants.DEFAULT_BASE_API_URL
                val subscriberId = arguments?.getString(SSInboxActivity.SUBSCRIBER_ID, "") ?: ""
                val distinctId = arguments?.getString(SSInboxActivity.DISTINCT_ID, "") ?: ""
                val route = "/inbox/fetch/?after=$after&distinct_id=$distinctId&subscriber_id=$subscriberId"
                val envKey = ConfigHelper.get(SSConstants.CONFIG_API_KEY) ?: ""
                val date = Date().toString()
                val signature = generateSignature(method = "GET", route = route, date = date)

                val httpResponse = makeHttpRequest(
                    method = "GET",
                    urL = "$baseUrl$route",
                    authorization = "$envKey:$signature",
                    date = date
                )
                if (httpResponse.statusCode == 200) {
                    ConfigHelper.addOrUpdate(SSConstants.INBOX_FETCH_TIME, System.currentTimeMillis().toString())
                    val latestJA = httpResponse.response?.let { JSONObject(it).optJSONArray("results") } ?: JSONArray()
                    Log.i(SSInboxActivity.TAG, "Latest items received : ${latestJA.length()}")
                    if (latestJA.length() > 0) {
                        storeResponse(latestJA)
                        activity?.runOnUiThread {
                            setRecyclerViewData(true)
                        }
                    }

                }
                if (BuildConfig.DEBUG) {
                    Log.i(
                        SSInboxActivity.TAG, "Response : $route" +
                            "\ndistinctId:$distinctId" +
                            "\nsubscriberId:$subscriberId" +
                            "\n${httpResponse.response}"
                    )
                }

            } catch (e: Exception) {
                Log.e(SSInboxActivity.TAG, "fetchApiCall", e)
            }
        }
    }

    private fun storeResponse(latestJA: JSONArray) {
        val prevResponse = ConfigHelper.get(SSConstants.INBOX_RESPONSE) ?: "[]"
        val prevJA = JSONArray(prevResponse)
        val ids = arrayListOf<String>()
        for (i in 0 until prevJA.length()) {
            val id = prevJA.getJSONObject(i).safeString("n_id") ?: ""
            ids.add(id)
        }
        for (i in 0 until latestJA.length()) {
            val id = latestJA.getJSONObject(i).safeString("n_id") ?: ""
            if (ids.contains(id))
                continue
            prevJA.put(latestJA.getJSONObject(i))
        }
        ConfigHelper.addOrUpdate(SSConstants.INBOX_RESPONSE, latestJA.toString())
        Log.i(SSInboxActivity.TAG, "Merged items Total : ${latestJA.length()}")
    }

    private fun setRecyclerViewData(isConnected: Boolean) {
        try {
            val response = ConfigHelper.get(SSConstants.INBOX_RESPONSE) ?: "[]"
            val items = parseInboxItems(JSONArray(response))
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