# InApp Feed (Headless)

Add a headless in-app inbox to your Android app with SuprSend SDK methods to fetch notifications, mark read, and build a fully custom feed UI.

The SDK provides the **data layer only** — no built-in UI. Bind to its data and
events and render your own.

This API matches the [iOS Swift SDK](https://github.com/suprsend/suprsend-swift-sdk) (`Feed`,
`FeedsFactory`, `IFeedOptions`, …) so a Flutter (or other) bridge can call the same class,
method and property names on both platforms.

## Prerequisites

Integration of the [Android SDK](../README.md).

## Initialize Feed Client

```kotlin
val feed: Feed = SuprSend.getInstance().feeds.initialize(
    options = IFeedOptions(
        tenantId = null,
        pageSize = 20,
        stores = null,
        host = FeedHost(
            socketHost = "https://betainbox.suprsend.com",
            apiHost = "https://inboxs.live"
        )
    )
)
```

**Options** (`IFeedOptions`)

| Parameter  | Type            | Description                                                                 |
| ---------- | --------------- | --------------------------------------------------------------------------- |
| `tenantId` | `String?`       | Tenant to read from. Defaults to the active tenant set in `identify` or `SuprSend.changeTenant`, else the `"default"` tenant |
| `pageSize` | `Int?`          | Notifications per page. Defaults to 20, max 100                             |
| `stores`   | `List<IStore>?` | Filtered views inside feed (multi-tab inbox)                                |
| `host`     | `FeedHost?`     | Override API/socket host via `FeedHost(socketHost, apiHost)`                |

> **Warning**
> If you pass `tenantId` on the feed (or have set it via `identify` / `changeTenant`), make sure that tenant is included in the `scope.tenant_id` key while creating the [userToken](https://docs.suprsend.com/docs/client-authentication) passed during identifying the user, else a **403** error is thrown due to scope mismatch.

## Feed Client

Feed methods that talk to the network are `suspend` functions (mirroring iOS `async`). Call them
from a coroutine. Do not run them on the main thread without `Dispatchers.IO` / `withContext`.

### Get Feed Data Store

Returns the current notification store — the list of notifications plus metadata
like page info and badge counts. You can call this anytime to get updated store data.

```kotlin
val feedData: IFeedData = feed.data

feedData.notifications     // List<IRemoteNotification>
feedData.store             // IStore — active store
feedData.pageInfo          // IPageInfo — total, hasMore, pageSize
feedData.meta              // "badge" — latest notifs count since last opened, plus per-store counts
feedData.apiStatus         // INITIAL / LOADING / SUCCESS / ERROR / FETCHING_MORE
```

### Initialize Socket for Realtime Updates

```kotlin
feed.initializeSocketConnection()
```

> **Warning**
> **Keep exactly one active socket per feed.** Multiple live sockets cause
> duplicated events, doubled badge increments, and inflated counts. Avoid:
>
> - **Recreating the feed without teardown.** `removeAll()` (or
>   `removeInstance(feed)`) the old feed *before* calling `feeds.initialize(...)` again.
> - **Owning the feed somewhere short-lived.** A feed re-created with its view
>   opens a socket each time; own it in a single, stable place and reuse it.
> - **Skipping cleanup on dismiss/logout.** An unclosed socket keeps running in
>   the background — tear down in `onDestroy` or on logout. `SuprSend.reset()`
>   calls `feeds.removeAll()`.

### Fetch Notification Data

Gets the first page of notifications from the SuprSend server and sets it in the
notification store (it also fetches badge counts on the first call).

```kotlin
feed.fetch()
```

### Fetch More Notifications

Gets the next page and appends it to the notification store. Call this only when
`pageInfo.hasMore == true`.

```kotlin
feed.fetchNextPage()
```

### Listening for Updates

Subscribe to `feed.emitter` to keep your UI in sync. Events are delivered on the **main thread**.
It fires two events:

- `InboxEmitterEvents.StoreUpdate` — the notification store changed (new notification, state
  update, pagination, mutations, socket events). Listen to this and update your local state so that the UI is refreshed.
- `InboxEmitterEvents.NewNotification` — use this listener to show a toast notification when a new notification is received.

```kotlin
sealed class InboxEmitterEvents {
    data class StoreUpdate(val data: IFeedData) : InboxEmitterEvents()
    data class NewNotification(val notification: IRemoteNotification) : InboxEmitterEvents()
}
```

```kotlin
val unsubscribe = feed.emitter.subscribe { event ->
    when (event) {
        is InboxEmitterEvents.StoreUpdate -> render(event.data)
        is InboxEmitterEvents.NewNotification -> showToast(event.notification)
    }
}

// later
unsubscribe()
```

### Removing Feed

Removes the feed client and aborts its socket connection.

```kotlin
SuprSend.getInstance().feeds.removeInstance(feed)  // one instance
SuprSend.getInstance().feeds.removeAll()           // all active inbox instances
```

`SuprSend.getInstance().reset(...)` also calls `feeds.removeAll()`.

> **Note**
> **Reconnecting:** When the app returns to the foreground, remove existing feed instances,
> re-initialize, reconnect the socket and emitters, then refetch data.

### Action Methods

```kotlin
// Change active store (when using multi-tab stores)
feed.changeActiveStore(storeId = "Unread")

// Reset the bell-icon badge count
feed.resetBadgeCount()

// Mark notifications as seen
feed.markBulkAsSeen(notificationIds = listOf("n_id_1", "n_id_2"))

// Mark all notifications as read
feed.markAllAsRead()

// Mark notification as read
feed.markAsRead(notificationId = "n_id")

// Mark notification as unread
feed.markAsUnread(notificationId = "n_id")

// Mark notification as interacted
feed.markAsInteracted(notificationId = "n_id")

// Archive notification
feed.markAsArchived(notificationId = "n_id")
```

Read more about [seen, read, and interacted](#notification-states).

**Response** returned by the action methods (`markAsRead`, `changeActiveStore`, etc.):

```kotlin
class APIResponse(
    val status: ResponseStatus,     // SUCCESS or ERROR
    val statusCode: Int?,           // HTTP status code
    val body: Map<String, String>?,
    val error: ResponseError?       // type and message, when status == ERROR
)
```

## Notification Structure

```kotlin
data class IRemoteNotification(
    val n_id: String,                          // unique notif id
    val n_category: String,
    val created_on: Double,                    // milliseconds since epoch
    val seen_on: Double?,
    val read_on: Double?,
    val interacted_on: Double?,
    val archived: Boolean?,
    val tags: List<String>?,
    val expiry: Double?,                       // milliseconds since epoch to expiry
    val is_expiry_visible: Boolean,
    val is_pinned: Boolean,
    val can_user_unpin: Boolean?,
    val message: IRemoteNotificationMessage
)

data class IRemoteNotificationMessage(
    val header: String?,
    val schema: String,
    val text: String,
    val url: String?,
    val open_in_new_tab: Boolean?,
    val extra_data: String?,
    val actions: List<IActionObject>?,
    val avatar: IAvatarObject?,
    val subtext: ISubTextObject?
)
```

JSON field names are snake_case (`n_id`, `created_on`, `store_id`, …) matching iOS and the feed API.

## Notification States

- **Seen**: `seen_on` flag is used to check if the notification has been seen in SuprSend analytics. If it's null, the notification has not been seen yet — call `markBulkAsSeen` when the notification enters the viewport and `seen_on` is null.
- **Read**: `read_on` flag is used to check if the notification has been read. This doesn't update SuprSend analytics and is only for visual purposes. If `read_on` is null, show a dot on the notification indicating it's unread. Call `markAsRead` when `read_on` is null, or `markAsUnread` when `read_on` has a timestamp. Marking as read will also set `seen_on` if not already set.
- **Interacted**: `interacted_on` flag is used to set the notification as clicked in SuprSend analytics. If `interacted_on` is null and the user clicks on the notification, call `markAsInteracted`. This will also set `read_on` and `seen_on` if not already set.

## Stores (`IStore`)

```kotlin
class IStore(
    val storeId: String,
    val label: String,
    val query: IStoreQuery? = null
)

class IStoreQuery(
    val tags: List<String>? = null,
    val categories: List<String>? = null,
    val read: Boolean? = null,
    val archived: Boolean? = null
)
```

Default store id when no stores are passed: `$suprsend_default_store`.

## HTTP endpoints

Base: `{apiHost}/v1/feed/{path}` (default `https://inboxs.live`)

| Operation | Method | Path |
| :--- | :--- | :--- |
| List | GET | `notifications` |
| Count / meta | GET | `notifications_count` |
| Detail | GET | `notifications/{id}` |
| Mark seen | PATCH | `notifications/{id}/seen` |
| Mark read | PATCH | `notifications/{id}/read` |
| Mark unread | PATCH | `notifications/{id}/unread` |
| Archive | PATCH | `notifications/{id}/archive` |
| Interacted | PATCH | `notifications/{id}/interacted` |
| Bulk seen | POST | `bulk/notifications/seen` |
| Reset bell | PATCH | `reset_bell_count` |
| Mark all read | PATCH | `mark_all_read` |

Common query params: `distinct_id`, `tenant_id`, `page_size`, `search_after`, `store`.

Auth: `Authorization` (public key) + `x-ss-signature` (user JWT), same as the rest of the SDK.
