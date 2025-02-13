package app.suprsend.android

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import app.suprsend.android.databinding.InboxMessageFragmentBinding
import app.suprsend.inbox.InBoxErrorType
import app.suprsend.inbox.InboxNotification
import app.suprsend.inbox.InboxStore
import app.suprsend.inbox.InboxStoreListener
import app.suprsend.inbox.SuprsendInbox
import app.suprsend.inbox.socket.ConnectionState
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class SSInboxMessageListFragment : Fragment() {


    lateinit var binding: InboxMessageFragmentBinding

    private lateinit var adapter: SSInboxMessageAdapter

    private lateinit var inboxStoreListener: InboxStoreListener

    private lateinit var inboxThemeConfig: InboxThemeConfig
    private var activeStoreId = ""
    private lateinit var suprsendInbox: SuprsendInbox

    private val coroutineScope = CoroutineScope(Dispatchers.IO)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            inboxThemeConfig = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                arguments?.getParcelable("config", InboxThemeConfig::class.java) ?: InboxThemeConfig()
            } else {
                arguments?.getParcelable("config") ?: InboxThemeConfig()
            }
            suprsendInbox = SuprsendInbox.getInstance()
        } catch (e: Exception) {
            Log.e(AppConstants.TAG, "App: onCreate", e)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = InboxMessageFragmentBinding.inflate(inflater, container, false)
        try {
            binding.inboxLL.setBackgroundColor(Color.parseColor(inboxThemeConfig.screenBgColor))
            initializeRecyclerView()
        } catch (e: Exception) {
            Log.e(AppConstants.TAG, "App: onCreateView", e)
        }
        return binding.root
    }

    private fun initializeRecyclerView() {
        binding.inboxRv.layoutManager = LinearLayoutManager(activity)
        adapter = SSInboxMessageAdapter(inflater = layoutInflater, message = listOf())
        binding.inboxRv.adapter = adapter

        updateConnectionState()
        inboxStoreListener = object : InboxStoreListener {
            override fun bellCount(bellCount: Int) {
                Log.i(AppConstants.TAG,"App: Bell count :$bellCount")
                updateTabTitles()
            }

            override fun loading(storeId: String, isLoading: Boolean) {
                if (activeStoreId == storeId) {
                    if (isLoading && suprsendInbox.getStore(activeStoreId).inboxMessagesList.isEmpty())
                        showLoading(true)
                }
            }

            override fun onUpdate(inboxStore: InboxStore) {
                if (activeStoreId != inboxStore.storeId)
                    return
                Log.i(AppConstants.TAG, "App: ${inboxStore.storeId} : Store data is changed")
                updateTabTitles()
                updateList()
            }

            override fun onError(id: String, errorType: InBoxErrorType, message: String, e: Exception?) {
                myToast("Socket : $id : $errorType : ${e?.message}")
            }

            override fun socket(connectionState: ConnectionState) {
                updateConnectionState()
            }

            override fun newNotification(notificationModel: InboxNotification) {
                myToast("New Notification : ${notificationModel.message.header}")
            }
        }
        suprsendInbox.registerCallback(inboxStoreListener)

        if (suprsendInbox.getStoreCount() == 0) {
            binding.tabLayout.visibility = View.GONE
        } else {
            binding.tabLayout.visibility = View.VISIBLE
            initTabs()
            coroutineScope.launch(Dispatchers.IO) {
                suprsendInbox.fetchBellCount()
                suprsendInbox.getStores().first().load()
            }
        }
    }

    private fun updateConnectionState() {
        val connectionState = suprsendInbox.getSocketConnectionState()
        when (connectionState) {
            ConnectionState.CONNECTING -> {
                binding.socketStatusIv.setImageResource(R.drawable.ic_connecting)
            }

            ConnectionState.CONNECTED -> {
                binding.socketStatusIv.setImageResource(R.drawable.ic_connected)
            }

            ConnectionState.DISCONNECTED -> {
                binding.socketStatusIv.setImageResource(R.drawable.ic_disconnected)
            }

            ConnectionState.FAILED -> {
                binding.socketStatusIv.setImageResource(R.drawable.ic_disconnected)
            }
        }
    }

    private fun updateTabTitles() {
        binding.tabLayout.forEachTab { tab ->
            val storeId = tab.tag?.toString()
            setTabTitle(tab, suprsendInbox.getStore(storeId))
        }
    }

    private fun initTabs() {
        activeStoreId = suprsendInbox.getStores().first().storeId
        val storeList = suprsendInbox.getStores()
        storeList.forEach { store ->
            val tab = binding.tabLayout.newTab()
            setTabTitle(tab, store)
            tab.tag = store.storeId
            binding.tabLayout.addTab(tab)
            //On tab click fetch notification
            binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val storeId = tab?.tag as String
                    if (storeId == activeStoreId) {
                        updateList()
                        return
                    }
                    activeStoreId = storeId
                    coroutineScope.launch(Dispatchers.IO) {
                        suprsendInbox.getStore(storeId = storeId)?.apply {
                            reset()
                            suprsendInbox.fetchBellCount()
                            load()
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                }
            })
        }
    }

    private fun setTabTitle(tab: TabLayout.Tab, store: InboxStore) {
        var tabText = "${store.label}"
        if (store.unseenCount > 0) {
            tabText += "(${store.unseenCount})"
        }
        tab.text = tabText
        Log.i(AppConstants.TAG, "App: Tab Title : $tabText")
    }

    private fun updateList() {
        try {
            val inbox = suprsendInbox
            val items = inbox.getStore(activeStoreId).inboxMessagesList.toMutableList()
            if (items.isEmpty()) {
                if (binding.emptyMessageTv.context.isConnected()) {
                    showEmptyScreen(inboxThemeConfig.emptyScreenMessage)
                } else {
                    showEmptyScreen(getString(R.string.no_internet))
                }
            } else {
                showDataScreen()
                adapter.newList(items)
            }
        } catch (e: Exception) {
            Log.e(AppConstants.TAG, "App: setRecyclerViewData", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        suprsendInbox.unRegisterCallback(inboxStoreListener)
    }

    private fun showEmptyScreen(message: String) {
        binding.emptyMessageTv.text = message
        binding.emptyMessageTv.visibility = View.VISIBLE
        binding.inboxRv.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.setVisible(isLoading)
        binding.emptyMessageTv.setVisible(false)
        binding.inboxRv.setVisible(false)
    }

    fun showDataScreen() {
        binding.inboxRv.setVisible(true)
        binding.progressBar.setVisible(false)
        binding.emptyMessageTv.setVisible(false)
    }
}
