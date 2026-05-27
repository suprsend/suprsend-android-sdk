# In-app inbox

SDK for the Suprsend in-app notification inbox: badge counts, notification feeds, read/seen/archive state, and real-time updates over a socket connection.

**Entry point:** `SuprsendInbox.getInstance()`

See also: [README.md](README.md) (setup & auth), [PREFERENCE.md](PREFERENCE.md) (notification preferences).

---

## Prerequisites

1. `SuprSend.initialize(...)` and user **`identify`** (inbox uses `distinct_id` from the SDK).
2. JWT mode: `SuprSend.setUserTokenFetcher(...)` if your workspace requires subscriber tokens.
3. **Subscriber id** from the Suprsend dashboard (not the same as `distinct_id` in all setups — use the value your workspace provides for inbox).
4. Configure inbox **before** `getInstance()`:

```kotlin
SuprsendInbox.setBaseUrl("https://inboxs.live")
SuprsendInbox.setInboxSocketUrl("https://betainbox.suprsend.com")
SuprsendInbox.setSubscriberId("<subscriber_id_from_dashboard>")
SuprsendInbox.setTenantId("optional_tenant")           // optional
SuprsendInbox.setInboxStores(listOf(InboxStore(...)))  // optional multi-store

val inbox = SuprsendInbox.getInstance()
inbox.registerCallback(inboxStoreListener)
inbox.openConnection()  // real-time connection + expired-message handler
```

`getInstance()` throws if `setSubscriberId` was not called or `setBaseUrl` was not set.

---

## Environment URLs

| Environment | Inbox (`setBaseUrl`) | Socket (`setInboxSocketUrl`) |
|-------------|----------------------|------------------------------|
| Production | `https://inboxs.live` | `https://betainbox.suprsend.com` |
| Staging | `https://inbox-staging.inboxs.workers.dev` | `https://staging-inbox-api.suprsend.com` |

Constants: `SSConstants.DEFAULT_INBOX_BASE_API_URL`, `DEFAULT_INBOX_SOCKET_API_URL`.

---

Configuration methods are invoked on `SuprsendInbox`. Inbox methods are invoked on `SuprsendInbox.getInstance()` after configuration.

#### Configuration API

| Method | Description |
|--------|-------------|
| `setBaseUrl(baseUrl)` | Inbox service base URL (required) |
| `setInboxSocketUrl(socketBaseUrl)` | Real-time inbox socket URL |
| `setSubscriberId(subscriberId)` | Inbox subscriber identifier (required) |
| `setTenantId(tenantId?)` | Multi-tenant filter; `null` clears |
| `setInboxStores(inboxStoreList?)` | Multi-store feeds; empty/null uses default store only |
| `getInstance()` | Singleton after configuration |

#### Inbox API

| Method | Description |
|--------|-------------|
| `getBellCount()` | Cached global badge count |
| `fetchBellCount()` | Refresh badge from server → `Response<Int>` |
| `fetchBellCountAsync(callback?)` | Async refresh |
| `resetBellCount()` / `resetBellCountAsync` | Reset badge to zero |
| `getStore(storeId?)` | `InboxStore` (`default` if omitted) |
| `getStores()` | All configured stores |
| `getStoreCount()` | Number of stores |
| `getNotificationDetails(notificationId, storeId?)` | Single `InboxNotification` |
| `markAllRead()` / `markAllReadAsync` | Mark all notifications read |
| `markAsRead(notificationId)` / `Async` | Mark one read |
| `markAsUnread(notificationId)` / `Async` | Mark unread |
| `markAsSeen(notificationId)` / `markAsSeen(ids)` / `Async` | Mark seen (bulk supported) |
| `markAsInteracted(notificationId)` / `Async` | Mark interacted |
| `markAsArchived(notificationId)` / `Async` | Archive |
| `getSocketConnectionState()` | `ConnectionState` enum |
| `openConnection()` | Open real-time connection; start `SSInboxExpiredMessages` |
| `closeConnection()` | Reset inbox internal state (disconnect flow) |
| `registerCallback(listener)` / `unRegisterCallback` | `InboxStoreListener` |

---

## `InboxStore`

Per-feed state and pagination. Default store id: `"default"` (`InboxStore.DEFAULT_STORE`).

| Member / method | Description |
|-----------------|-------------|
| `storeId` | Store identifier |
| `label` | Display label (optional) |
| `query` | `InboxQuery` filters (optional) |
| `load()` | Fetch next page → `Response<InboxStore>` |
| `hasNextPage()` | Whether more pages exist |
| `getCurrentPageNo()` / `getTotalPages()` | Pagination |
| `inboxMessagesList` | `ArrayList<InboxNotification>` in memory |
| `unseenCount` | Unseen count for this store |
| `toJSONObject()` | Store descriptor for multi-store configuration |

**Multi-store example:**

```kotlin
SuprsendInbox.setInboxStores(
    listOf(
        InboxStore(
            storeId = "orders",
            label = "Orders",
            query = InboxQuery(/* filters */)
        )
    )
)
```

When only non-default stores are configured, bell count and feed loading account for all configured stores.

---

## `InboxStoreListener`

Implement to drive UI:

| Callback | When |
|----------|------|
| `bellCount(count)` | Global badge updated |
| `loading(storeId, isLoading)` | Store fetch started/finished |
| `onUpdate(inboxStore)` | Store list or metadata changed |
| `onError(id, errorType, message, exception)` | `InBoxErrorType.NOTIFICATION` or `STORE` |
| `socket(connectionState)` | Real-time connection state changed |
| `newNotification(notification)` | New item received in real time |

**`ConnectionState`:** `DISCONNECTED`, `CONNECTING`, `CONNECTED`, `FAILED`

---

## `InboxNotification`

Defined in `SSInboxItem.kt` — includes id, message payload, read/seen flags, timestamps, etc. Use `getNotificationDetails` to refresh a single notification from the server.

---

## Real-time updates

Call `openConnection()` after identify and inbox setup. The SDK maintains a socket connection (configured via `setInboxSocketUrl`) and delivers updates through `InboxStoreListener` — for example `newNotification` and `bellCount`.

Call `closeConnection()` on logout (also cleared indirectly via `SuprSend.reset()`).

---

## Typical integration flow

```kotlin
// After login + identify
CommonAnalyticsHandler.initializeInbox()  // setBaseUrl, subscriberId, etc.

val inbox = SuprsendInbox.getInstance()
inbox.registerCallback(myListener)
inbox.fetchBellCountAsync()
inbox.openConnection()

// In a fragment
val store = inbox.getStore("default") // or custom storeId
store.load()

// User actions
inbox.markAsReadAsync(notificationId) { /* update UI */ }
```

On logout:

```kotlin
inbox.unRegisterCallback(listener)
inbox.closeConnection()
SuprSend.getInstance().reset(unSubscribeNotification = true)
```

---

## Notes

- `fetchBellCount` includes an internal delay (~1s) before the server request in the current implementation.
- When JWT mode is enabled, inbox operations refresh the subscriber token when needed.
- Prefer `SuprsendInbox.setBaseUrl` over `SuprSend.setInboxBaseUrl` for inbox configuration.

---

## Sample app

See **`jwtApp`**:

- `CommonAnalyticsHandler.initializeInbox()`
- `HomeActivity` — bell count, socket, callbacks
- `SSInboxMessageListFragment` — store `load()` and list UI
