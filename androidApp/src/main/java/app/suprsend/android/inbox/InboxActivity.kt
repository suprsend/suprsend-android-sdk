package app.suprsend.android.inbox

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.suprsend.SSApi
import app.suprsend.android.BaseViewHolder
import app.suprsend.android.convertToList
import app.suprsend.android.databinding.ActivityInboxBinding
import app.suprsend.android.databinding.InboxItemBinding
import app.suprsend.android.databinding.InboxTabItemBinding
import app.suprsend.android.setVisibleOrGone
import app.suprsend.inbox.SSInbox
import app.suprsend.inbox.model.InboxStoreListener
import app.suprsend.inbox.model.NotificationModel
import app.suprsend.inbox.model.NotificationStoreConfig
import app.suprsend.inbox.model.NotificationStoreQuery
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("SetTextI18n")
class InboxActivity : AppCompatActivity() {

    lateinit var binding: ActivityInboxBinding

    lateinit var ssInbox: SSInbox

    private var tabSelectedPosition = 0
    private var selectedNotificationStoreConfig: NotificationStoreConfig? = null
    private lateinit var notificationStoreConfigs: List<NotificationStoreConfig>
    private lateinit var inboxRecyclerAdapter: InboxRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Inbox"
        binding = ActivityInboxBinding.inflate(layoutInflater)
        setContentView(binding.root)
        notificationStoreConfigs = getNotificationConfigs(intent.getStringExtra("inboxStoreJson") ?: "")
        ssInbox = SSInbox(
            subscriberId = intent.getStringExtra("inboxSubscriberId") ?: "",
            distinctId = SSApi.getInstance().getDistinctId(),
            pageSize = 20,
            notificationStoreConfigs = notificationStoreConfigs
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        inboxRecyclerAdapter = InboxRecyclerAdapter(
            activity = this,
            ssInbox = ssInbox,
            list = listOf()
        )
        binding.recyclerView.adapter = inboxRecyclerAdapter

        if (notificationStoreConfigs.isNotEmpty()) {
            tabSelectedPosition = 0
            selectedNotificationStoreConfig = notificationStoreConfigs.first()
        }

        // Render tabs with default count
        renderTabs()
        binding.bellCountTv.text = "BellCount${ssInbox.getData().bellCount.showCount()}"
        ssInbox.addListener(object : InboxStoreListener {

            override fun loading(storeId: String, isLoading: Boolean) {
            }

            override fun bellCount(bellCount: Int) {
                binding.bellCountTv.text = "BellCount${ssInbox.getData().bellCount.showCount()}"
                renderTabs()
            }

            override fun onUpdate(
                storeId: String,
                allNotifications: List<NotificationModel>
//                totalPages: Int,
//                total: Int
            ) {
                if (
                    selectedNotificationStoreConfig == null || // No stores are present
                    selectedNotificationStoreConfig?.storeId == storeId // If store is present
                ) {
                    val notifications = allNotifications.map { it.copy() }
                    updateItemsInUI(notifications)
                    binding.bellCountTv.text = "BellCount${ssInbox.getData().bellCount.showCount()}"
                    // On Unread action tab count changes
                    renderTabs()
                }
            }

            override fun onError(storeId: String, e: Exception) {
            }

            override fun socket(isConnected: Boolean) {
                binding.connectDisconnect.text = if (isConnected) "Disconnect" else "Connect"
            }
        })

        if (notificationStoreConfigs.isEmpty()) {
            ssInbox.load()
        } else {
            ssInbox.load(notificationStoreConfigs.first().storeId)
        }
        binding.loadNextPageTv.setOnClickListener {
            if (notificationStoreConfigs.isEmpty())
                ssInbox.load()
            else
                ssInbox.load(selectedNotificationStoreConfig?.storeId ?: "")
        }
        binding.bellCountTv.setOnClickListener {
            ssInbox.markBellClicked()
        }
        binding.markAllRead.setOnClickListener {
            ssInbox.markAllNotificationRead()
        }
        binding.connectDisconnect.setOnClickListener {
            if (ssInbox.isSocketConnected()) {
                ssInbox.disconnect()
            } else {
                ssInbox.connect()
            }
        }
    }

    private fun renderTabs() {
        binding.tabsLL.removeAllViews()
        notificationStoreConfigs.forEachIndexed { index, notificationStoreConfig ->
            val tabItemViewBinding = InboxTabItemBinding.inflate(layoutInflater)
            val store = ssInbox.getData().notificationStores[index]
            tabItemViewBinding.title = (notificationStoreConfig.label + store.unseenCount.showCount())
            tabItemViewBinding.selected = (index == tabSelectedPosition)
            tabItemViewBinding.tabTitleTv.setOnClickListener {
                tabSelectedPosition = index
                selectedNotificationStoreConfig = notificationStoreConfig
                if (!store.hasInitialFetchTime())
                    ssInbox.load(selectedNotificationStoreConfig?.storeId ?: "")
                updateItemsInUI(store.notifications)
                renderTabs()
            }
            binding.tabsLL.addView(tabItemViewBinding.root)
        }
    }

    private fun updateItemsInUI(notifications: List<NotificationModel>) {
        val isEmpty = notifications.isEmpty()
        inboxRecyclerAdapter.updateList(notifications)
        binding.messageTv.text = "No notifications yet"
        binding.messageTv.visibility = setVisibleOrGone(isEmpty)
        binding.recyclerView.visibility = setVisibleOrGone(!isEmpty)
    }

    private fun getNotificationConfigs(json: String): List<NotificationStoreConfig> {
        val list = mutableListOf<NotificationStoreConfig>()
        try {
            val ja = JSONArray(json)
            for (i in 0 until ja.length()) {
                val storeJo = ja.getJSONObject(i)
                val queryJO = storeJo.optJSONObject("query") ?: JSONObject()
                val tags: List<String>? = if (queryJO.has("tags")) {
                    val tags = queryJO.get("tags")
                    if (tags is String) {
                        listOf(tags)
                    } else {
                        queryJO.getJSONArray("tags").convertToList<String>()
                    }
                } else null
                val categories: List<String>? = if (queryJO.has("categories")) {
                    val categories = queryJO.get("categories")
                    if (categories is String) {
                        listOf(categories)
                    } else {
                        queryJO.getJSONArray("categories").convertToList<String>()
                    }
                } else null
                val read = if (queryJO.has("read")) queryJO.optBoolean("read") else null
                list.add(
                    NotificationStoreConfig(
                        storeJo.optString("storeId"),
                        label = storeJo.optString("label").ifBlank { null },
                        query = NotificationStoreQuery(
                            tags,
                            categories,
                            read
                        )
                    )
                )
            }
        } catch (e: Exception) {
        }
        return list
    }

    override fun onDestroy() {
        super.onDestroy()
        ssInbox.disconnect()
    }

    private class InboxRecyclerAdapter(
        val activity: InboxActivity,
        val ssInbox: SSInbox,
        private var list: List<NotificationModel>
    ) : RecyclerView.Adapter<BaseViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            return BaseViewHolder(InboxItemBinding.inflate(LayoutInflater.from(parent.context)))
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
            val binding = holder.binding as InboxItemBinding
            val notification = list[position]
            binding.obj = notification
            binding.markUnreadTv.setOnClickListener {
                ssInbox.markNotificationUnRead(
                    storeId = activity.selectedNotificationStoreConfig?.storeId ?: "",
                    notificationId = notification.id
                )
            }
            binding.markReadTv.setOnClickListener {
                ssInbox.markNotificationRead(
                    storeId = activity.selectedNotificationStoreConfig?.storeId ?: "",
                    notificationId = notification.id
                )
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        fun updateList(list: List<NotificationModel>) {
            this.list = list
            notifyDataSetChanged()
        }
    }
}

private fun Int.showCount(): String {
    return if (this <= 0) {
        ""
    } else {
        " ($this)"
    }
}
