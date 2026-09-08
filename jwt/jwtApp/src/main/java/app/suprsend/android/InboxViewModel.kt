package app.suprsend.android

import android.widget.Toast
import app.suprsend.SuprSend
import app.suprsend.feed.APIResponseStatus
import app.suprsend.feed.Feed
import app.suprsend.feed.FeedHost
import app.suprsend.feed.IFeedData
import app.suprsend.feed.IFeedOptions
import app.suprsend.feed.IRemoteNotification
import app.suprsend.feed.IStore
import app.suprsend.feed.IStoreQuery
import app.suprsend.feed.InboxEmitterEvents
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class InboxViewModel {

    var notifications: List<IRemoteNotification> = listOf()
    var apiStatus: APIResponseStatus = APIResponseStatus.INITIAL
    var hasMore: Boolean = false
    var badge: Int = 0
    var activeStoreId: String
    var storeBadges: Map<String, Int> = mapOf()

    val stores: List<IStore> = listOf(
        IStore(storeId = "all", label = "All"),
        IStore(storeId = "unread", label = "Unread", query = IStoreQuery(read = false)),
        IStore(storeId = "read", label = "Read", query = IStoreQuery(read = true)),
        IStore(storeId = "archived", label = "Archived", query = IStoreQuery(archived = true)),
        IStore(
            storeId = "transactional",
            label = "Transactional",
            query = IStoreQuery(categories = listOf("transactional"))
        ),
        IStore(storeId = "custom", label = "Custom", query = IStoreQuery(tags = listOf("name")))
    )

    private var feed: Feed
    private var cancellables: MutableList<() -> Unit> = mutableListOf()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    init {
        this.activeStoreId = stores.firstOrNull()?.storeId ?: ""
        feed = SuprSend.getInstance().feeds.initialize(options = makeOptions(stores))
        bindFeed()
        scope.launch { initialFetch() }
    }

    fun subscribe(listener: () -> Unit): () -> Unit {
        listeners.add(listener)
        return { listeners.remove(listener) }
    }

    fun reconnectAndRefresh() {
        cancellables.forEach { it.invoke() }
        cancellables.clear()
        SuprSend.getInstance().feeds.removeAll()
        feed = SuprSend.getInstance().feeds.initialize(options = makeOptions(stores))
        bindFeed()
        scope.launch { initialFetch() }
    }

    fun changeStore(storeId: String) {
        if (storeId == activeStoreId) return
        activeStoreId = storeId
        scope.launch { feed.changeActiveStore(storeId = storeId) }
    }

    suspend fun initialFetch() {
        feed.fetch()
    }

    suspend fun refresh() {
        feed.fetch()
    }

    suspend fun loadMore() {
        if (!hasMore || apiStatus == APIResponseStatus.FETCHING_MORE) return
        feed.fetchNextPage()
    }

    fun onItemTap(item: IRemoteNotification) {
        scope.launch {
            if (item.read_on == null) {
                feed.markAsRead(notificationId = item.n_id)
            }
            feed.markAsInteracted(notificationId = item.n_id)
        }
    }

    fun markAsRead(item: IRemoteNotification) {
        scope.launch { feed.markAsRead(notificationId = item.n_id) }
    }

    fun markAsUnread(item: IRemoteNotification) {
        scope.launch { feed.markAsUnread(notificationId = item.n_id) }
    }

    fun archive(item: IRemoteNotification) {
        scope.launch { feed.markAsArchived(notificationId = item.n_id) }
    }

    fun markAllAsRead() {
        scope.launch { feed.markAllAsRead() }
    }

    fun resetBadge() {
        scope.launch { feed.resetBadgeCount() }
    }

    private fun bindFeed() {
        feed.initializeSocketConnection()
        cancellables.add(feed.emitter.subscribe { event ->
            if (event is InboxEmitterEvents.NewNotification) {
                Toast.makeText(AppCreator.context, "New:"+event.notification.message.header.orEmpty(), Toast.LENGTH_SHORT).show()
            }
            if (event is InboxEmitterEvents.StoreUpdate) {
                apply(data = event.data)
            }
        })
    }

    private fun apply(data: IFeedData) {
        notifications = data.notifications
        apiStatus = data.apiStatus
        hasMore = data.pageInfo.hasMore
        badge = data.meta["badge"]?.toIntOrNull() ?: 0
        val badges = mutableMapOf<String, Int>()
        stores.forEach { store ->
            badges[store.storeId] = data.meta[store.storeId]?.toIntOrNull() ?: 0
        }
        storeBadges = badges
        listeners.forEach { it.invoke() }
    }

    private fun destroy() {
        cancellables.forEach { it.invoke() }
        cancellables.clear()
        listeners.clear()
        scope.coroutineContext[Job]?.cancel()
        SuprSend.getInstance().feeds.removeAll()
    }

    companion object {
        @Volatile
        private var instance: InboxViewModel? = null

        val shared: InboxViewModel
            get() {
                val existing = instance
                if (existing != null) return existing
                synchronized(this) {
                    val again = instance
                    if (again != null) return again
                    return InboxViewModel().also { instance = it }
                }
            }

        fun sharedOrNull(): InboxViewModel? = instance

        fun reset() {
            synchronized(this) {
                instance?.destroy()
                instance = null
            }
        }

        private fun makeOptions(stores: List<IStore>): IFeedOptions {
            return IFeedOptions(
                tenantId = null,
                pageSize = 10,
                stores = stores,
                host = feedHost
            )
        }

        /** Nil when neither host is configured, so the SDK's own defaults apply. */
        private val feedHost: FeedHost?
            get() {
                val apiHost = BuildConfig.SS_INBOX_BASE_URL.takeIf { it.isNotBlank() }
                val socketHost = BuildConfig.SS_INBOX_SOCKET_URL.takeIf { it.isNotBlank() }
                if (apiHost == null && socketHost == null) {
                    return null
                }
                return FeedHost(
                    socketHost = socketHost,
                    apiHost = apiHost
                )
            }
    }
}
