package app.suprsend.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.suprsend.R
import org.json.JSONArray

internal class SSInboxMessageListFragment : Fragment() {

    var inboxRv: RecyclerView? = null
    var emptyMessageTv: TextView? = null

    lateinit var ssInboxConfig: SSInboxConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Todo : test with null value
        ssInboxConfig = arguments?.getParcelable("config") ?: SSInboxConfig()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.inbox_message_list_fragment, container, false)
        val inboxRv = view.findViewById<RecyclerView>(R.id.inboxRv)
        emptyMessageTv = view.findViewById(R.id.emptyMessageTv)
        this.inboxRv = inboxRv
        initializeRecyclerView(inboxRv)
        return view
    }

    private fun initializeRecyclerView(inboxRv: RecyclerView) {
        inboxRv.layoutManager = LinearLayoutManager(activity)
    }

    private fun dummyJA(): JSONArray {
        return JSONArray(
            """
            [{"created_on":1643105158102,"n_category":"transactional","message":{"image":"https://niksdevelop.herokuapp.com/images/346kb.jpg","text":"This is image item"},"n_id":"n1"},{"created_on":1643105158102,"n_category":"transactional","message":{"image":"https://niksdevelop.herokuapp.com/images/346kb.jpg","text":"This is image with button","button":"Go to Website","url":"https://www.suprsend.com"},"n_id":"n2"},{"created_on":1643105158102,"n_category":"transactional","message":{"image":"https://niksdevelop.herokuapp.com/images/346kb.jpg","text":"The disaster was caused by the failure of the two redundant O-ring seals in a joint in the Space Shuttle's right solid rocket booster (SRB). The record-low temperatures of the launch reduced the elasticity of the rubber O-rings, reducing their ability to seal the joints. The broken seals caused a breach in the joint shortly after liftoff, which allowed pressurized gas from within the SRB to leak and burn through the wall to the adjacent external fuel tank. This led to the separation of the right-hand SRB's aft attachment and the structural failure of the external tank. Following the explosion, the orbiter, which included the crew compartment, was broken up by aerodynamic forces."},"n_id":"n3"},{"created_on":1643105158102,"n_category":"transactional","message":{"image":"https://niksdevelop.herokuapp.com/images/346kb.jpg","text":"The disaster was caused by the failure of the two redundant O-ring seals in a joint in the Space Shuttle's right solid rocket booster (SRB). The record-low temperatures of the launch reduced the elasticity of the rubber O-rings, reducing their ability to seal the joints. The broken seals caused a breach in the joint shortly after liftoff, which allowed pressurized gas from within the SRB to leak and burn through the wall to the adjacent external fuel tank. This led to the separation of the right-hand SRB's aft attachment and the structural failure of the external tank. Following the explosion, the orbiter, which included the crew compartment, was broken up by aerodynamic forces.","button":"Go to Youtube","url":"vnd.youtube://4KnNVK-udTU"},"n_id":"n4"},{"created_on":1643093467494,"n_category":"transactional","seen_on":1643101796876,"message":{"image":"https://niksdevelop.herokuapp.com/images/346kb.jpg","text":"This is Seen !","url":"https://www.suprsend.com"},"n_id":"n5"}]
        """.trimIndent()
        )
    }

    override fun onStart() {
        super.onStart()
        setRecyclerViewData(parseInboxItems(dummyJA()))
    }

    private fun setRecyclerViewData(items: List<SSInboxItemVo>) {
        if (items.isEmpty()) {
            emptyMessageTv?.visibility = View.VISIBLE
            inboxRv?.visibility = View.GONE
        } else {
            emptyMessageTv?.visibility = View.GONE
            inboxRv?.visibility = View.VISIBLE
            inboxRv?.adapter = SSInboxMessageAdapter(items)
        }
    }
}