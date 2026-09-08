package app.suprsend.feed

import app.suprsend.SSInternal
import app.suprsend.SuprSend
import app.suprsend.base.SSConstants
import app.suprsend.log.Logger
import app.suprsend.model.ErrorType
import app.suprsend.model.ResponseError
import app.suprsend.model.ResponseStatus
import app.suprsend.utils.urlEncode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** A class responsible for handling inbox feed. */
class Feed internal constructor(
    private val config: SuprSend,
    options: IFeedOptions? = null
) {

    private val feedOptions: IFeedOptions

    private val storeLock = Any()

    // Store is a normal field touched from IO coroutines, socket coroutines, timers, and the main thread so we need volatile also storeLock
    @Volatile
    private var store: INotificationStore

    @Volatile
    private var socket: SocketClient? = null

    @Volatile
    private var expiryTimerJob: Job? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client = FeedApiClient()

    /**
     * Bumped whenever in flight fetches should be considered stale (e.g. on store switch).
     * Fetch checks this after the request and bails if it changed.
     */
    @Volatile
    private var fetchGeneration = 0

    // Android is more defensive for JVM concurrency / re-entrancy
    @Volatile
    private var removed = false

    val emitter = FeedEmitter()

    val data: IFeedData
        get() {
            val storeData = store
            return IFeedData(
                notifications = storeData.notifications,
                store = storeData.store,
                pageInfo = storeData.pageInfo,
                meta = storeData.meta,
                apiStatus = storeData.apiStatus
            )
        }

    init {
        var pageSize = FeedConstants.PAGE_SIZE
        val pageSizeOption = options?.pageSize
        if (pageSizeOption != null && pageSizeOption in 1..FeedConstants.MAX_PAGE_SIZE) {
            pageSize = pageSizeOption
        }
        feedOptions = IFeedOptions(
            tenantId = options?.tenantId ?: SSInternal.suprSendData.tenantId ?: FeedConstants.TENANT_ID,
            pageSize = pageSize,
            stores = validatedStores(options?.stores),
            host = options?.host
        )
        store = initialStore(feedOptions.stores?.firstOrNull() ?: FeedConstants.STORE)
    }

    internal fun reset() {
        resetState(feedOptions.stores?.firstOrNull() ?: FeedConstants.STORE)
    }

    private fun resetState(activeStore: IStore, preservesMeta: Boolean = false) {
        synchronized(storeLock) {
            store = initialStore(
                activeStore = activeStore,
                meta = if (preservesMeta) store.meta else mapOf(FeedConstants.BADGE to "0")
            )
        }
        emitter.send(InboxEmitterEvents.StoreUpdate(data))

        expiryTimerJob?.cancel()
        expiryTimerJob = null
    }

    internal fun remove() {
        if (removed) return
        removed = true
        reset()
        emitter.complete()
        socket?.disconnect()
        socket = null
        scope.cancel()
        config.feeds.removeInstance(this)
    }

    private fun validatedStores(stores: List<IStore>?): List<IStore>? {
        stores ?: return null

        if (stores.isEmpty()) {
            Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "SuprSend: stores should be an array of objects")
            return stores
        }

        return stores.mapNotNull { store ->
            if (store.storeId.isBlank()) {
                Logger.i(
                    SSConstants.TAG_SUPRSEND_INBOX,
                    "SuprSend: storeId is mandatory for each store. Ignoring store without storeId"
                )
                null
            } else {
                IStore(
                    storeId = store.storeId,
                    label = store.label.ifBlank { store.storeId },
                    query = store.query
                )
            }
        }
    }

    private fun initialStore(
        activeStore: IStore,
        meta: Map<String, String> = mapOf(FeedConstants.BADGE to "0")
    ): INotificationStore {
        return INotificationStore(
            notifications = listOf(),
            store = activeStore,
            pageInfo = IPageInfo(total = 0, hasMore = false, pageSize = FeedConstants.PAGE_SIZE),
            meta = meta,
            apiStatus = APIResponseStatus.INITIAL,
            isFirstFetch = true
        )
    }



    private inline fun updateStore(block: (INotificationStore) -> INotificationStore) {
        synchronized(storeLock) {
            store = block(store)
        }
    }

    // region API Calls

    private val requestInprogress: Boolean
        get() = store.apiStatus == APIResponseStatus.LOADING || store.apiStatus == APIResponseStatus.FETCHING_MORE

    suspend fun fetchCount(): FeedCountAPIResponse = withContext(Dispatchers.IO) {
        val queryParams = mapOf(
            "distinct_id" to config.getDistinctId(),
            "tenant_id" to feedOptions.tenantId,
            "stores" to if (feedOptions.stores != null) storesQueryParamObj(feedOptions.stores) else null
        )

        val url = getUrl(path = "notifications_count", qp = queryParams)

        val response = client.request(url = url, type = RequestType.GET).toFeedCountAPIResponse()

        if (response.status == ResponseStatus.SUCCESS) {
            val meta = mutableMapOf(FeedConstants.BADGE to "${response.body?.badge ?: 0}")
            response.body?.storeCounts?.forEach { entry -> meta[entry.key] = "${entry.value}" }
            updateStore { it.with(meta = meta) }
        }

        emitter.send(InboxEmitterEvents.StoreUpdate(data))
        response
    }

    suspend fun fetch(options: IInboxFetchOptions? = null): FeedAPIResponse = withContext(Dispatchers.IO) {
        if (requestInprogress) {
            return@withContext FeedAPIResponse.error(
                ResponseError(type = ErrorType.VALIDATION_ERROR, message = "Already fetching data")
            )
        }

        val generation = fetchGeneration
        val pageSize = options?.pageSize ?: feedOptions.pageSize

        val isFirstFetch = store.isFirstFetch
        if (isFirstFetch) {
            updateStore { it.with(apiStatus = APIResponseStatus.LOADING) }
            // Badge count refresh runs alongside the first page request, mirroring the web SDK
            // which doesn't await it. fetchCount updates the store when it resolves.
            scope.launch { fetchCount() }
        } else {
            updateStore { it.with(apiStatus = APIResponseStatus.FETCHING_MORE) }
        }

        val storeData = store
        emitter.send(InboxEmitterEvents.StoreUpdate(data))

        val lastNotification = storeData.notifications.lastOrNull()
        val searchAfter = JSONArray()
        if (lastNotification != null) {
            searchAfter.put(if (lastNotification.is_pinned) 1 else 0)
            searchAfter.put(lastNotification.created_on.toJsonNumber())
        }

        val queryParams = mapOf(
            "distinct_id" to config.getDistinctId(),
            "tenant_id" to feedOptions.tenantId,
            "page_size" to pageSize,
            "store" to if (storeData.store.storeId != FeedConstants.STORE.storeId) storeQueryParamObj(storeData.store) else null,
            "search_after" to searchAfter
        )

        val url = getUrl(path = "notifications", qp = queryParams)
        val response = client.request(url = url, type = RequestType.GET).toFeedAPIResponse()

        if (generation != fetchGeneration) {
            return@withContext FeedAPIResponse.error(
                ResponseError(type = ErrorType.VALIDATION_ERROR, message = "Fetch cancelled")
            )
        }

        if (response.status == ResponseStatus.ERROR) {
            updateStore { it.with(apiStatus = APIResponseStatus.ERROR) }
            emitter.send(InboxEmitterEvents.StoreUpdate(data))
            return@withContext response
        }

        val results = response.body?.results ?: listOf()
        val notifications = if (storeData.isFirstFetch) results else storeData.notifications + results

        val totalCount = response.body?.meta?.total_count ?: 0
        val currentPage = response.body?.meta?.current_page
        val totalPages = response.body?.meta?.total_pages
        val hasMore = if (currentPage != null && totalPages != null) {
            currentPage < totalPages
        } else {
            notifications.size < totalCount
        }

        synchronized(storeLock) {
            store = INotificationStore(
                notifications = notifications,
                store = storeData.store,
                pageInfo = IPageInfo(
                    total = totalCount,
                    hasMore = hasMore,
                    pageSize = storeData.pageInfo.pageSize
                ),
                meta = store.meta,
                apiStatus = APIResponseStatus.SUCCESS,
                isFirstFetch = false
            )
        }

        emitter.send(InboxEmitterEvents.StoreUpdate(data))

        startExpiryTimer()

        response
    }

    suspend fun fetchNextPage(): FeedAPIResponse {
        if (!store.pageInfo.hasMore) {
            return FeedAPIResponse.error(
                ResponseError(type = ErrorType.VALIDATION_ERROR, message = "No more pages to fetch")
            )
        }
        return fetch()
    }

    /**
     * Switches the active store for this feed. Any in flight [fetch] is invalidated so its
     * result won't paint into the new store's view, then state is reset and the first page
     * (plus badge count) is refetched. Returns a validation error if [storeId] doesn't match
     * any configured store.
     */
    suspend fun changeActiveStore(storeId: String): FeedAPIResponse {
        val newStore = feedOptions.stores?.firstOrNull { it.storeId == storeId }
            ?: return FeedAPIResponse.error(
                ResponseError(
                    type = ErrorType.VALIDATION_ERROR,
                    message = "No store configured with storeId: $storeId"
                )
            )

        if (newStore.storeId == store.store.storeId) {
            return FeedAPIResponse.success()
        }

        fetchGeneration += 1
        resetState(activeStore = newStore, preservesMeta = true)

        return fetch()
    }

    suspend fun fetchDetails(notificationId: String): FeedDetailAPIResponse = withContext(Dispatchers.IO) {
        val url = getUrl(
            path = "notifications/$notificationId",
            qp = mapOf(
                "tenant_id" to feedOptions.tenantId,
                "distinct_id" to config.getDistinctId()
            )
        )

        client.request(url = url, type = RequestType.GET).toFeedDetailAPIResponse()
    }

    suspend fun markAsSeen(notificationId: String): APIResponse = withContext(Dispatchers.IO) {
        var alreadyUpdated = false

        updateStore { storeData ->
            storeData.with(
                notifications = storeData.notifications.map { notification ->
                    if (notification.n_id == notificationId) {
                        if (notification.seen_on == null) {
                            return@map notification.copy(seen_on = nowInMillis())
                        }
                        alreadyUpdated = true
                    }
                    notification
                }
            )
        }

        if (alreadyUpdated) {
            return@withContext APIResponse.success()
        }

        val url = actionUrl("notifications/$notificationId/seen")

        emitter.send(InboxEmitterEvents.StoreUpdate(data))
        client.request(url = url, type = RequestType.PATCH).toAPIResponse()
    }

    suspend fun markAsRead(notificationId: String): APIResponse = withContext(Dispatchers.IO) {
        var alreadyUpdated = false

        updateStore { storeData ->
            storeData.with(
                notifications = storeData.notifications.map { notification ->
                    if (notification.n_id == notificationId) {
                        if (notification.read_on == null) {
                            val now = nowInMillis()
                            return@map notification.copy(
                                read_on = now,
                                seen_on = notification.seen_on ?: now
                            )
                        }
                        alreadyUpdated = true
                    }
                    notification
                }
            )
        }

        if (alreadyUpdated) {
            return@withContext APIResponse.success()
        }

        val url = actionUrl("notifications/$notificationId/read")

        emitter.send(InboxEmitterEvents.StoreUpdate(data))
        client.request(url = url, type = RequestType.PATCH).toAPIResponse()
    }

    suspend fun markAsUnread(notificationId: String): APIResponse = withContext(Dispatchers.IO) {
        var alreadyUpdated = false

        updateStore { storeData ->
            storeData.with(
                notifications = storeData.notifications.map { notification ->
                    if (notification.n_id == notificationId) {
                        if (notification.read_on != null) {
                            return@map notification.copy(read_on = null)
                        }
                        alreadyUpdated = true
                    }
                    notification
                }
            )
        }

        if (alreadyUpdated) {
            return@withContext APIResponse.success()
        }

        val url = actionUrl("notifications/$notificationId/unread")

        emitter.send(InboxEmitterEvents.StoreUpdate(data))
        client.request(url = url, type = RequestType.PATCH).toAPIResponse()
    }

    suspend fun markAsInteracted(notificationId: String): APIResponse = withContext(Dispatchers.IO) {
        // Track whether anything actually changed so we still emit/API when only read_on is newly set.
        var needsNetwork = false

        updateStore { storeData ->
            storeData.with(
                notifications = storeData.notifications.map { notification ->
                    if (notification.n_id != notificationId) {
                        return@map notification
                    }
                    var interactedOn = notification.interacted_on
                    var readOn = notification.read_on
                    if (interactedOn == null) {
                        interactedOn = nowInMillis()
                        needsNetwork = true
                    }
                    if (readOn == null) {
                        readOn = nowInMillis()
                        needsNetwork = true
                    }
                    notification.copy(interacted_on = interactedOn, read_on = readOn)
                }
            )
        }

        if (!needsNetwork) {
            return@withContext APIResponse.success()
        }

        val url = actionUrl("notifications/$notificationId/interacted")

        emitter.send(InboxEmitterEvents.StoreUpdate(data))
        client.request(url = url, type = RequestType.PATCH).toAPIResponse()
    }

    suspend fun markAsArchived(notificationId: String): APIResponse = withContext(Dispatchers.IO) {
        var alreadyUpdated = false

        updateStore { storeData ->
            storeData.with(
                notifications = storeData.notifications.filter { notification ->
                    if (notification.n_id == notificationId) {
                        alreadyUpdated = notification.archived == true
                        false
                    } else {
                        true
                    }
                }
            )
        }

        if (alreadyUpdated) {
            // Row already removed locally — notify UI even when skipping the network call.
            emitter.send(InboxEmitterEvents.StoreUpdate(data))
            return@withContext APIResponse.success()
        }

        val url = actionUrl("notifications/$notificationId/archive")

        emitter.send(InboxEmitterEvents.StoreUpdate(data))
        client.request(url = url, type = RequestType.PATCH).toAPIResponse()
    }

    suspend fun markBulkAsSeen(notificationIds: List<String>): APIResponse = withContext(Dispatchers.IO) {
        updateStore { storeData ->
            storeData.with(
                notifications = storeData.notifications.map { notification ->
                    if (notificationIds.contains(notification.n_id) && notification.seen_on == null) {
                        notification.copy(seen_on = nowInMillis())
                    } else {
                        notification
                    }
                }
            )
        }

        val url = actionUrl("bulk/notifications/seen")
        val payload = JSONObject().apply {
            put("notification_ids", JSONArray().apply { notificationIds.forEach { put(it) } })
        }

        emitter.send(InboxEmitterEvents.StoreUpdate(data))
        client.request(url = url, type = RequestType.POST, payload = payload).toAPIResponse()
    }

    suspend fun resetBadgeCount(): APIResponse = withContext(Dispatchers.IO) {
        updateStore { storeData ->
            storeData.with(meta = storeData.meta.toMutableMap().apply { put(FeedConstants.BADGE, "0") })
        }

        val url = actionUrl("reset_bell_count")

        emitter.send(InboxEmitterEvents.StoreUpdate(data))
        client.request(url = url, type = RequestType.PATCH).toAPIResponse()
    }

    suspend fun markAllAsRead(): APIResponse = withContext(Dispatchers.IO) {
        updateStore { storeData ->
            storeData
                .with(meta = storeData.meta.toMutableMap().apply { put(FeedConstants.BADGE, "0") })
                .with(
                    notifications = storeData.notifications.map { notification ->
                        if (notification.read_on == null) notification.copy(read_on = nowInMillis()) else notification
                    }
                )
        }

        val url = actionUrl("mark_all_read")

        emitter.send(InboxEmitterEvents.StoreUpdate(data))
        client.request(url = url, type = RequestType.PATCH).toAPIResponse()
    }

    // endregion

    // region Expiry Timer

    private fun startExpiryTimer() {
        if (expiryTimerJob != null) return

        expiryTimerJob = scope.launch {
            while (isActive) {
                delay(FeedConstants.EXPIRY_CHECK_INTERVAL_IN_MILLIS)
                removeExpiredFeed()
            }
        }
    }

    private suspend fun removeExpiredFeed() {
        var hasExpired = false
        // `expiry` is delivered by the backend in milliseconds since epoch.
        val nowInMillis = System.currentTimeMillis().toDouble()

        updateStore { storeData ->
            storeData.with(
                notifications = storeData.notifications.filter { notification ->
                    val expired = notification.expiry != null && nowInMillis > notification.expiry
                    if (expired) {
                        hasExpired = true
                    }
                    !expired
                }
            )
        }

        if (hasExpired) {
            fetchCount()
            emitter.send(InboxEmitterEvents.StoreUpdate(data))
        }
    }

    // endregion

    // region URL & Query Params

    private fun storeQueryParamObj(store: IStore): JSONObject {
        val query = JSONObject().apply {
            putIfNotNull("read", store.query?.read)
            putIfNotNull("archived", store.query?.archived)
            put("tags", orFilter(store.query?.tags))
            put("categories", orFilter(store.query?.categories))
        }
        return JSONObject().apply {
            put("store_id", store.storeId)
            put("query", query)
        }
    }

    private fun orFilter(values: List<String>?): JSONObject {
        return JSONObject().apply {
            put("or", JSONArray().apply { values?.forEach { put(it) } })
        }
    }

    private fun storesQueryParamObj(stores: List<IStore>?): JSONArray? {
        stores ?: return null
        return JSONArray().apply { stores.forEach { put(storeQueryParamObj(it)) } }
    }

    private fun getUrl(path: String, qp: Map<String, Any?>): String {
        var host = feedOptions.host?.apiHost ?: FeedConstants.DEFAULT_API_HOST
        while (host.endsWith("/")) {
            host = host.dropLast(1)
        }
        val urlPath = "$host/v1/feed/$path"
        val queryParams = qp
            .filterValues { it != null }
            .map { entry -> "${entry.key}=${urlEncode(entry.value.toString())}" }

        return if (queryParams.isEmpty()) urlPath else "$urlPath?${queryParams.joinToString("&")}"
    }

    private fun actionUrl(path: String): String {
        return getUrl(
            path = path,
            qp = mapOf(
                "tenant_id" to feedOptions.tenantId,
                "distinct_id" to config.getDistinctId()
            )
        )
    }

    // Epoch millis — matches API / web SDK (created_on, read_on, seen_on, interacted_on).
    private fun nowInMillis(): Double = System.currentTimeMillis().toDouble()

    // endregion

    // region Filter, Sort

    private fun notificationBelongToStore(
        notification: IRemoteNotification,
        store: IStore?
    ): Boolean {
        val notifRead = notification.read_on != null
        val notifArchived = notification.archived
        val notifTags = notification.tags
        val notifCategory = notification.n_category

        val storeRead = store?.query?.read
        val storeArchived = store?.query?.archived
        val storeTags = store?.query?.tags
        val storeCategories = store?.query?.categories

        val sameRead = storeRead == null || notifRead == storeRead
        val sameArchived = (notifArchived ?: false) == (storeArchived ?: false)

        val sameTags = if (storeTags == null || storeTags.isEmpty()) {
            true
        } else {
            storeTags.any { tag -> notifTags?.contains(tag) == true }
        }

        val sameCategory = if (storeCategories == null || storeCategories.isEmpty()) {
            true
        } else {
            storeCategories.contains(notifCategory)
        }

        return sameRead && sameTags && sameCategory && sameArchived
    }

    private fun orderNotificationsBasedOnPinFlag(
        newNotification: IRemoteNotification,
        existingNotifications: List<IRemoteNotification>
    ): List<IRemoteNotification> {
        // pinned notifications go to the very top, unpinned ones right after the pinned block
        if (newNotification.is_pinned) {
            return listOf(newNotification) + existingNotifications
        }

        var addedNotification = false
        val notifications = mutableListOf<IRemoteNotification>()

        existingNotifications.forEach { notification ->
            if (notification.is_pinned) {
                notifications.add(notification)
            } else {
                if (!addedNotification) {
                    notifications.add(newNotification)
                    addedNotification = true
                }
                notifications.add(notification)
            }
        }

        return if (addedNotification) notifications else existingNotifications + newNotification
    }

    // endregion

    // region Web Socket

    fun initializeSocketConnection() {
        if (socket != null) return

        var host = feedOptions.host?.socketHost ?: FeedConstants.DEFAULT_SOCKET_HOST
        while (host.endsWith("/")) {
            host = host.dropLast(1)
        }

        val socketClient = SocketClient(serverURL = host, headers = socketHeaders())
        socket = socketClient
        initializeSocketEvents(socketClient)
        socketClient.connect()
    }

    private fun socketHeaders(): Map<String, String> {
        return mapOf(
            "authorization" to (SSInternal.suprSendData.publicApiKey ?: ""),
            "x-ss-signature" to (SSInternal.getToken() ?: ""),
            "distinct_id" to (config.getDistinctId() ?: ""),
            "tenant_id" to (feedOptions.tenantId ?: FeedConstants.TENANT_ID),
            "schema" to "1"
        )
    }

    private fun initializeSocketEvents(socketClient: SocketClient) {
        socketClient.receivedMessage = { eventType, payload ->
            when (eventType) {
                SocketClient.EventType.NEW_NOTIFICATION ->
                    scope.launch { handleNewNotificationSocketEvent(payload) }

                SocketClient.EventType.NOTIFICATION_UPDATE ->
                    scope.launch { handleNotificationUpdateSocketEvent(payload) }

                SocketClient.EventType.BULK_NOTIFICATION_UPDATE ->
                    scope.launch { handleBulkNotificationUpdateSocketEvent(payload) }

                SocketClient.EventType.RESET_BADGE -> handleResetBadge()
            }
        }

        socketClient.connectionLost = {
            scope.launch { handleSocketConnectionLost() }
        }
    }

    /**
     * Refreshes an expired user token and pushes the new auth headers into [SocketClient] so the
     * next scheduled reconnect uses fresh credentials. No-op while the token is still valid or
     * when no refresh callback is configured.
     */
    private fun handleSocketConnectionLost() {
        val distinctId = config.getDistinctId() ?: return
        if (SSInternal.suprSendData.refreshUserToken == null) return

        val refreshStatus = SSInternal.refreshTokenIfRequired(distinctId = distinctId)
        if (!refreshStatus.isSuccess()) {
            Logger.i(SSConstants.TAG_SUPRSEND_INBOX, "Couldn't refresh userToken for socket reconnect")
            return
        }

        socket?.updateHeaders(socketHeaders())
    }

    private fun handleResetBadge() {
        updateStore { storeData ->
            storeData.with(meta = storeData.meta.toMutableMap().apply { put(FeedConstants.BADGE, "0") })
        }

        emitter.send(InboxEmitterEvents.StoreUpdate(data))
    }

    private suspend fun handleNewNotificationSocketEvent(payload: JSONObject?) {
        val notificationId = payload?.optString("n_id")
        if (notificationId.isNullOrBlank()) return

        val response = fetchDetails(notificationId = notificationId)
        val newNotificationData = response.body
        if (response.status == ResponseStatus.ERROR || newNotificationData == null) return

        val storeData = store
        var emitNewNotificationEvent = false
        val newMetaData = storeData.meta.toMutableMap()

        if (notificationBelongToStore(newNotificationData, storeData.store)) {
            emitNewNotificationEvent = true
            updateStore {
                it.with(
                    notifications = orderNotificationsBasedOnPinFlag(
                        newNotification = newNotificationData,
                        existingNotifications = it.notifications
                    )
                )
            }
        }

        feedOptions.stores?.forEach { configuredStore ->
            if (notificationBelongToStore(newNotificationData, configuredStore)) {
                emitNewNotificationEvent = true
                val count = storeData.meta[configuredStore.storeId]?.toIntOrNull() ?: 0
                newMetaData[configuredStore.storeId] = "${count + 1}"
            }
        }

        if (emitNewNotificationEvent) {
            val badge = newMetaData[FeedConstants.BADGE]?.toIntOrNull() ?: 0
            newMetaData[FeedConstants.BADGE] = "${badge + 1}"
        }
        updateStore { it.with(meta = newMetaData) }

        if (emitNewNotificationEvent) {
            emitter.send(InboxEmitterEvents.NewNotification(newNotificationData))
        }

        emitter.send(InboxEmitterEvents.StoreUpdate(data))
    }

    private suspend fun handleNotificationUpdateSocketEvent(payload: JSONObject?) {
        val notificationId = payload?.optString("n_id")
        if (notificationId.isNullOrBlank()) return

        // Fetch in parallel
        val detail = coroutineScope {
            val details = async { fetchDetails(notificationId = notificationId) }
            val count = async { fetchCount() }
            count.await()
            details.await()
        }
        val newNotificationData = detail.body
        if (detail.status != ResponseStatus.SUCCESS || newNotificationData == null) return

        val storeData = store
        val notificationPresent = storeData.notifications.any { it.n_id == newNotificationData.n_id }
        val belongsToStore = notificationBelongToStore(newNotificationData, storeData.store)

        updateStore { current ->
            when {
                // Insert new notification
                belongsToStore && !notificationPresent -> current.with(
                    notifications = orderNotificationsBasedOnPinFlag(
                        newNotification = newNotificationData,
                        existingNotifications = current.notifications
                    )
                )
                // Update existing notification data
                belongsToStore -> current.with(
                    notifications = current.notifications.map { notification ->
                        if (notification.n_id == newNotificationData.n_id) newNotificationData else notification
                    }
                )
                // Filter out notification
                else -> current.with(
                    notifications = current.notifications.filter { it.n_id != newNotificationData.n_id }
                )
            }
        }

        emitter.send(InboxEmitterEvents.StoreUpdate(data))
    }

    private fun handleBulkNotificationUpdateSocketEvent(payload: JSONObject?) {
        payload ?: return

        val action = payload.optString("action")
        val notificationIds = payload.opt("notification_ids")

        if (action == "read" && notificationIds == "all") {
            updateStore { storeData ->
                val meta = storeData.meta.mapValues { "0" }
                storeData
                    .with(meta = meta)
                    .with(
                        notifications = storeData.notifications.map { notification ->
                            if (notification.read_on == null) notification.copy(read_on = nowInMillis()) else notification
                        }
                    )
            }
        }

        if (action == "seen" && notificationIds is JSONArray) {
            val ids = (0 until notificationIds.length()).mapNotNull { notificationIds.optString(it) }
            updateStore { storeData ->
                storeData.with(
                    notifications = storeData.notifications.map { notification ->
                        if (ids.contains(notification.n_id)) notification.copy(seen_on = nowInMillis()) else notification
                    }
                )
            }
        }

        emitter.send(InboxEmitterEvents.StoreUpdate(data))
    }

    // endregion
}
