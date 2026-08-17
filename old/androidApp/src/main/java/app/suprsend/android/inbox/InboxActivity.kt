package app.suprsend.android.inbox

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.suprsend.android.AppCreator
import app.suprsend.android.BaseViewHolder
import app.suprsend.android.R
import app.suprsend.android.convertToList
import app.suprsend.android.databinding.ActivityInboxBinding
import app.suprsend.android.databinding.InboxItemActionBinding
import app.suprsend.android.databinding.InboxItemBinding
import app.suprsend.android.databinding.InboxTabItemBinding
import app.suprsend.android.getIntent
import app.suprsend.android.layoutInflater
import app.suprsend.android.logInfo
import app.suprsend.android.myToast
import app.suprsend.android.safeStartActivity
import app.suprsend.android.setMarkDownText
import app.suprsend.android.setVisibleOrGone
import app.suprsend.inbox.ConnectionState
import app.suprsend.inbox.SSInbox
import app.suprsend.inbox.model.InboxStoreListener
import app.suprsend.inbox.model.NotificationModel
import app.suprsend.inbox.model.NotificationStoreConfig
import app.suprsend.inbox.model.NotificationStoreQuery
import app.suprsend.inbox.util.getReadableTime
import app.suprsend.inbox.util.todayMidNightMilli
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
        notificationStoreConfigs =
            getNotificationConfigs(intent.getStringExtra("inboxStoreJson") ?: "")
        ssInbox = SSInbox.initialize(
            subscriberId = intent.getStringExtra("inboxSubscriberId") ?: "",
            distinctId = AppCreator.getEmail(this),
            pageSize = 20,
            notificationStoreConfigs = notificationStoreConfigs
        )

        initRecyclerview()

        if (notificationStoreConfigs.isNotEmpty()) {
            tabSelectedPosition = 0
            selectedNotificationStoreConfig = notificationStoreConfigs.first()
        }

        // Render tabs with default count
        renderTabs()
        binding.bellCountTv.text = "BellCount${ssInbox.getData().bellCount.showCount()}"
        ssInbox.addListener(object : InboxStoreListener {

            override fun loading(storeId: String, isLoading: Boolean) {
                if (selectedNotificationStoreConfig?.storeId == storeId && SSInbox.getInstance()?.findStore(storeId)?.notifications?.isEmpty() == true) {
                    binding.progressBar.visibility = setVisibleOrGone(isLoading)
                } else {
                    binding.progressBar.visibility = setVisibleOrGone(false)
                }
            }

            override fun bellCount(bellCount: Int) {
                binding.bellCountTv.text = "BellCount${ssInbox.getData().bellCount.showCount()}"
                renderTabs()
            }

            override fun onUpdate(
                storeId: String,
                allNotifications: List<NotificationModel>
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

            override fun socket(connectionState: ConnectionState) {
            }

            override fun newNotification(notificationModel: NotificationModel) {
                myToast("New Notification : ${notificationModel.message.header}")
            }
        })

        if (notificationStoreConfigs.isEmpty()) {
            ssInbox.load()
        } else {
            ssInbox.load(notificationStoreConfigs.first().storeId)
        }
        binding.bellCountTv.setOnClickListener {
            ssInbox.markBellClicked()
        }
        binding.markAllRead.setOnClickListener {
            ssInbox.markAllNotificationRead()
        }

    }

    private fun initRecyclerview() {
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = layoutManager
        inboxRecyclerAdapter = InboxRecyclerAdapter(
            activity = this,
            ssInbox = ssInbox,
            list = listOf()
        )
        binding.recyclerView.adapter = inboxRecyclerAdapter
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                val position = layoutManager.findLastCompletelyVisibleItemPosition()
                logInfo("At position $position")
                val store = ssInbox.findStore(selectedNotificationStoreConfig?.storeId)
                if (hasReachedEnd(store?.notifications, position))
                    ssInbox.load(store?.notificationStoreConfig?.storeId)
            }
        })
    }

    fun hasReachedEnd(list: List<Any>?, position: Int): Boolean {
        if (list == null)
            return false
        return position >= list.size - 3
    }


    private fun renderTabs() {
        binding.tabsLL.removeAllViews()
        notificationStoreConfigs.forEachIndexed { index, notificationStoreConfig ->
            val tabItemViewBinding = InboxTabItemBinding.inflate(layoutInflater)
            val store = ssInbox.getData().notificationStores[index]
            tabItemViewBinding.title =
                (notificationStoreConfig.label + store.unseenCount.showCount())
            tabItemViewBinding.selected = (index == tabSelectedPosition)
            tabItemViewBinding.tabTitleTv.setOnClickListener {
                tabSelectedPosition = index
                selectedNotificationStoreConfig = notificationStoreConfig
                ssInbox.reload(notificationStoreConfig.storeId)
                updateItemsInUI(store.notifications)
                //Selected tab position is changed
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
                val archived = if (queryJO.has("archived")) queryJO.optBoolean("archived") else null
                list.add(
                    NotificationStoreConfig(
                        storeJo.optString("storeId"),
                        label = storeJo.optString("label").ifBlank { null },
                        query = NotificationStoreQuery(
                            tags = tags,
                            categories = categories,
                            read = read,
                            archived = archived
                        )
                    )
                )
            }
        } catch (e: Exception) {
        }
        return list
    }

    override fun onStart() {
        super.onStart()
        ssInbox.connect()
    }

    override fun onStop() {
        super.onStop()
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
            holder.binding.root.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            val context = binding.titleTv.context
            val notification = list[position]
            binding.obj = notification

            binding.titleTv.setMarkDownText(notification.message.header)
            binding.messageTv.setMarkDownText(notification.message.text)
            binding.subTextTv.setMarkDownText(notification.message.subText?.text)
            binding.timeTv.text = context.getString(R.string.createdOn) + getReadableTime(notification.createdOn)
            val expiry = notification.expiry
            if (expiry != null)
                binding.expiryTimeTv.text = context.getString(R.string.expiry) + " " + getReadableTime(expiry, todayMidNightMilli())

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
            binding.archiveTv.setOnClickListener {
                ssInbox.markNotificationArchive(
                    storeId = activity.selectedNotificationStoreConfig?.storeId ?: "",
                    notificationId = notification.id
                )
            }
            val avtarUrl = notification.message.avtar?.avtarUrl
            if (!avtarUrl.isNullOrBlank())
                AppCreator.loadUrl(binding.avtarIv.context, avtarUrl, binding.avtarIv)
            binding.avtarIv.setOnClickListener {
                binding.avtarIv.context.safeStartActivity(notification.message.avtar?.actionUrl?.getIntent())
            }

            binding.actionButtonsLL.removeAllViews()
            binding.subTextTv.setOnClickListener {
                binding.subTextTv.context.safeStartActivity(notification.message.subText?.actionUrl?.getIntent())
            }
            binding.actionUrl.setOnClickListener {
                binding.subTextTv.context.safeStartActivity(notification.message.url?.getIntent())
                ssInbox.markNotificationClicked(
                    storeId = activity.selectedNotificationStoreConfig?.storeId ?: "",
                    notificationId = notification.id
                )
            }
            notification.message.actions?.forEach { action ->
                val actionBinding = InboxItemActionBinding.inflate(binding.titleTv.layoutInflater())
                actionBinding.button.text = action.name
                actionBinding.button.setOnClickListener {
                    context.safeStartActivity(action.url.getIntent())
                }
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(
                    0,
                    0,
                    context.resources.getDimension(R.dimen.margin_10).toInt(),
                    0
                )
                binding.actionButtonsLL.addView(actionBinding.root, params)
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
