package app.suprsend.inbox

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.suprsend.R
import app.suprsend.base.SdkAndroidCreator
import app.suprsend.base.appExecutorService
import app.suprsend.base.makeGetCall
import org.json.JSONArray

internal class SSInboxMessageListFragment : Fragment() {

    private var inboxRv: RecyclerView? = null
    private var emptyMessageTv: TextView? = null
    private var inboxLL: ViewGroup? = null

    private lateinit var ssInboxConfig: SSInboxConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ssInboxConfig = arguments?.getParcelable("config") ?: SSInboxConfig()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.inbox_message_list_fragment, container, false)
        inboxLL = view.findViewById(R.id.inboxLL)
        inboxLL?.setBackgroundColor(Color.parseColor(ssInboxConfig.screenBgColor))
        val inboxRv = view.findViewById<RecyclerView>(R.id.inboxRv)
        emptyMessageTv = view.findViewById(R.id.emptyMessageTv)
        this.inboxRv = inboxRv
        initializeRecyclerView(inboxRv)
        return view
    }

    private fun initializeRecyclerView(inboxRv: RecyclerView) {
        inboxRv.layoutManager = LinearLayoutManager(activity)
    }

    override fun onStart() {
        super.onStart()
        if (SdkAndroidCreator.networkInfo.isConnected()) {
            appExecutorService.execute {
                //Todo : Testing url
                val messageResponseStr = makeGetCall("https://abc.in/http/uploads/inbox_screen_messages.json")
                val messagesJA = JSONArray(messageResponseStr)
                val messagesItemList = parseInboxItems(messagesJA)
                activity?.runOnUiThread {
                    setRecyclerViewData(messagesItemList)
                }
            }
        } else {
            showEmptyScreen(getString(R.string.no_internet))
        }

    }

    private fun setRecyclerViewData(items: List<SSInboxItemVo>) {
        if (items.isEmpty()) {
            showEmptyScreen(ssInboxConfig.emptyScreenMessage)
        } else {
            emptyMessageTv?.visibility = View.GONE
            inboxRv?.visibility = View.VISIBLE
            inboxRv?.adapter = SSInboxMessageAdapter(message = items, ssInboxConfig = ssInboxConfig)
        }
    }

    private fun showEmptyScreen(message: String) {
        emptyMessageTv?.text = message
        emptyMessageTv?.visibility = View.VISIBLE
        inboxRv?.visibility = View.GONE
    }
}