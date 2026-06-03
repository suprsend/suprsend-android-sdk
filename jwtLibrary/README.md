# Suprsend Android SDK (JWT Library)

Android library (`jwtLibrary`) for integrating [Suprsend](https://suprsend.com) in mobile apps. It covers user identity, event tracking, subscriber profile updates, push notifications (FCM), notification preferences, and the in-app notification inbox.

This variant adds **JWT-based subscriber authentication**: pass a `RefreshTokenCallback` (or `userToken`) to `identify` / `identityAsync` and the SDK caches and refreshes the subscriber JWT for later API calls.

**Package:** `app.suprsend`  
**Maven coordinates:** `com.suprsend:native:<version>` (see `buildSrc/Deps.kt` for the current version)

| Document | Description |
|----------|-------------|
| [README.md](README.md) | Setup, auth, core SDK (`SuprSend`, `User`), events API |
| [docs/Integration.mdx](docs/Integration.mdx) | SDK integration guide (Mintlify) |
| [docs/Push.mdx](docs/Push.mdx) | FCM push setup (Mintlify) |

---

## Preview documentation

Mintlify sources for public docs live under `jwtLibrary/docs/`. From the repository root:

1. **Install** the Mintlify CLI:
   ```bash
   npm i -g mint
   ```
2. **Go to** the docs directory:
   ```bash
   cd jwtLibrary/docs
   ```
3. **Run** the local preview server:
   ```bash
   mint dev
   ```

Open the URL printed in the terminal (typically `http://localhost:3000`) to preview **Integration** and **Push** pages.

---

## Requirements

| Item | Version |
|------|---------|
| Min SDK | 19 |
| Target / Compile SDK | 33 |
| Kotlin | 1.3.72+ |
| JVM target | 1.8 |
| Firebase Cloud Messaging | Required for push |

---

## Installation

### Gradle (project module)

```kotlin
dependencies {
    implementation("com.suprsend:native:2.0.1") // use version from your release
    implementation("com.google.firebase:firebase-messaging:<version>")
}
```

### Local development

```kotlin
implementation(project(":jwtLibrary"))
```

### AndroidManifest (FCM)

Firebase allows **one** `FirebaseMessagingService` per app. Choose either the SDK default or your own service (see [Push notifications — application-managed](#push-notifications--application-managed)).

**Option A — SDK service (simplest)**

```xml
<service
    android:name="app.suprsend.fcm.SSFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

**Option B — your own service**

Register your class instead and **do not** declare `SSFirebaseMessagingService` (only one handler may receive `MESSAGING_EVENT`).

```xml
<service
    android:name=".fcm.AppFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

See `jwtApp` for a full integration example (`AppFirebaseMessagingService`).

---

## Quick start

```kotlin
// Application.onCreate()
SuprSend.initialize(
    context = this,
    publicApiKey = "SS.PUBK.xxx",
    host = "https://hub.suprsend.com", // optional; default shown
    appInfo = AppInfo(name = "MyApp", version = BuildConfig.VERSION_NAME)
)

// After login — pass JWT callback or token on identify (see Identify subscriber)
val tokenCallback = object : RefreshTokenCallback {
    override fun getToken(distinctId: String): String {
        // Fetch JWT from YOUR backend — never embed secrets in the app
        return yourBackend.getSubscriberJwt(distinctId)
    }
}

SuprSend.getInstance().identityAsync(
    distinctId = "user@example.com",
    refreshTokenCallback = tokenCallback,
    actionStatusCallback = object : ActionStatusCallback {
        override fun onComplete(response: ApiResponse) {
            if (response.isSuccess()) {
                SuprSend.getInstance().user.addEmailAsync("user@example.com")
            }
        }
    }
)
```

---
## Authentication

### Public API key

Pass your workspace public API key to `SuprSend.initialize(publicApiKey = ...)`.

### JWT (this library)

Enable JWT by passing `refreshTokenCallback` and/or `userToken` to `identify` / `identityAsync`. The callback is stored on the SDK for the session and used for all subsequent authenticated calls.

1. Before events, profile updates, preferences, and inbox operations, the SDK calls `getToken(distinctId)` if no valid JWT is cached.
2. Expired tokens are refreshed automatically (up to 3 retries). Invalid tokens are cleared and fetched again.
3. `isIdentified(checkUserToken = true)` requires both `distinctId` and a non-blank JWT.

Implement `RefreshTokenCallback.getToken()` to return a subscriber JWT from **your backend** (never issue JWTs inside the app). There is no global `setRefreshTokenCallback` — supply the callback on each login via `identify`, or pass a pre-fetched `userToken` when you already have one.

### Client metadata

Pass `appInfo` (or `clientInfo`) in `initialize()` so the SDK can attach app and device metadata with outbound requests.

---

## Identify subscriber

Call `identify` (or `identityAsync`) after login to bind the device to a subscriber. The SDK sends a `$identify` event, persists `distinctId` locally on success, and re-attaches any cached FCM token.

### Signatures

```kotlin
// Sync — do not call on the main thread
fun identify(
    distinctId: String,
    userToken: String? = null,
    refreshTokenCallback: RefreshTokenCallback? = null
): ApiResponse

// Async — runs on a background executor; callback on the main thread
fun identityAsync(
    distinctId: String,
    userToken: String? = null,
    refreshTokenCallback: RefreshTokenCallback? = null,
    actionStatusCallback: ActionStatusCallback? = null
)
```

### Parameters

| Parameter | Description |
|-----------|-------------|
| `distinctId` | Required. Your subscriber identifier (email, user id, etc.). Must not be blank. |
| `userToken` | Optional. Subscriber JWT to cache before the `$identify` request. Use when your app already has a token from your backend. |
| `refreshTokenCallback` | Optional. If non-null, stores this callback on the SDK for token fetch/refresh on later API calls. Pass on login; required for JWT mode unless you only use `userToken`. |
| `actionStatusCallback` | **Async only.** `ActionStatusCallback` invoked on the main thread with the `ApiResponse` from `identify`. |

### JWT options

You can supply a token in two ways (they can be combined):

1. **Refresh callback** — pass `refreshTokenCallback` to `identify` / `identityAsync`; the SDK fetches/refreshes the JWT when needed and keeps the callback for the session.
2. **Pre-fetched token** — pass `userToken` to cache a JWT before `$identify`; pair with `refreshTokenCallback` so expired tokens can still be renewed.

When a callback is set, `identify` runs `refreshTokenIfRequired` before sending `$identify`. Ensure network connectivity and a valid token from your backend.

### Behavior

| Case | Result |
|------|--------|
| `distinctId` is blank | `ApiResponse` error (`VALIDATION_ERROR`) |
| Another `distinctId` is already active | Error — call `reset()` before switching users |
| Same `distinctId` as last successful identify | Success immediately (no new `$identify` request) |
| `$identify` succeeds | `distinctId` saved; FCM token re-registered if available |
| `$identify` fails (e.g. network, 401) | `distinctId` not updated; SDK may retry identify after a later token refresh |

### Examples

```kotlin
// Async with JWT callback on login
SuprSend.getInstance().identityAsync(
    distinctId = "user@example.com",
    refreshTokenCallback = tokenCallback,
    actionStatusCallback = myCallback
)

// Sync with token from your login API (optional callback for refresh)
val response = SuprSend.getInstance().identify(
    distinctId = userId,
    userToken = loginResponse.subscriberJwt,
    refreshTokenCallback = tokenCallback
)

// Callback only — SDK fetches JWT before $identify
SuprSend.getInstance().identify(
    distinctId = userId,
    refreshTokenCallback = object : RefreshTokenCallback {
        override fun getToken(distinctId: String) = yourBackend.getSubscriberJwt(distinctId)
    }
)
```

Use `isIdentified()` to check whether a `distinctId` is set; use `isIdentified(checkUserToken = true)` when JWT mode is on and you need a cached token as well.

---

## SDK API reference

Methods marked **sync** use `@WorkerThread` and must not run on the main thread. **Async** variants run on a background executor and invoke callbacks on the main thread where noted.

### `SuprSend` — initialization & core

Configuration methods are invoked on the `SuprSend` class. Subscriber methods are invoked on `SuprSend.getInstance()` after initialization.

#### Configuration API

Methods for one-time SDK setup and global options. Most can be called before `getInstance()`.

| Method | Description |
|--------|-------------|
| `initialize(context, publicApiKey, appInfo?, clientInfo?, host?)` | One-time setup. Restores `distinctId` from local storage. Starts offline notification event flush. |
| `getInstance()` | Returns singleton instance. Throws if not initialized. |
| `setTenantId(tenantId)` | Multi-tenant workspace ID (optional). |
| `setInboxBaseUrl(url)` | Sets inbox URL on internal config (inbox feature uses `SuprsendInbox.setBaseUrl` — see [INBOX.md](INBOX.md)). |
| `setLogger(callback)` | Route SDK logs to your `LoggerCallback`. |
| `setNotificationCallback(listener)` | Called when a push payload is received (after SDK handling). |

#### Subscriber API

Methods for the active subscriber session (identity, events, reset). Requires `initialize()` and typically `identify()`.

| Method / property | Description |
|-----------------|-------------|
| `user` | `User` instance for profile operators and preferences. |
| `identify(distinctId, userToken?, refreshTokenCallback?)` **sync** | Binds device to subscriber via `$identify`. See [Identify subscriber](#identify-subscriber). |
| `identityAsync(distinctId, userToken?, refreshTokenCallback?, actionStatusCallback?)` | Background `identify`; `actionStatusCallback` on main thread. |
| `isIdentified(checkUserToken?)` | `true` if `distinctId` is set; pass `checkUserToken = true` to also require a non-blank JWT. |
| `getDistinctId()` | Current distinct ID or `null`. |
| `trackEvent(eventName)` **sync** | Track a custom event. |
| `trackEventAsync(eventName, callback?)` | Async track. |
| `trackEvent(eventName, properties)` **sync** | Track with `JSONObject` properties. |
| `trackEventAsync(eventName, properties, callback?)` | Async track with properties. |
| `reset(unSubscribeNotification)` **sync** | Clears user, JWT, preferences cache, inbox state. Optionally removes FCM token from subscriber. |
| `resetAsync(unSubscribeNotification, callback?)` | Async reset. |
| `notificationClicked(notificationActionVo)` | Tracks `$notification_clicked` with notification id / button label. |
| `setLogLevel(level)` | `LogLevel`: `VERBOSE`, `DEBUG`, `INFO`, `ERROR`, `OFF`. |

### `User` — subscriber profile operators

Profile updates use Suprsend operator keys such as `$set`, `$append`, `$unset`, etc.

| Method | Operator | Notes |
|--------|----------|-------|
| `setPreferredLanguage(language)` | `$set` | `$preferred_language` |
| `setTimezone(timezone)` | `$set` | `$timezone` |
| `set(key, value)` / `set(properties)` | `$set` | Custom properties; reserved keys filtered |
| `unSet(key)` / `unSet(keys)` | `$unset` | Remove properties |
| `setOnce(key, value)` / `setOnce(properties)` | `$set_once` | Set only if not already set |
| `increment(key, value)` / `increment(map)` | `$add` | Numeric properties; use negative to decrement |
| `append(key, value)` / `append(properties)` | `$append` | Append to list properties |
| `remove(key, value)` / `remove(properties)` | `$remove` | Remove from list properties |
| `addEmail` / `removeEmail` | `$append` / `$remove` | `$email`; validated |
| `addSms` / `removeSms` | `$append` / `$remove` | `$sms`; E.164-style validation |
| `addWhatsapp` / `removeWhatsapp` | `$append` / `$remove` | `$whatsapp` |
| `addSlack` / `removeSlack` | `$append` / `$remove` | `$slack` (JSONObject) |
| `addMSTeams` / `removeMSTeams` | `$append` / `$remove` | `$ms_teams` (JSONObject) |
| `setAndroidFcmPush(token)` | `$append` | Registers `$androidpush` with FCM vendor |
| `getPreferences()` | — | Returns `Preferences` — see [PREFERENCE.md](PREFERENCE.md) |

Each **sync** method has a matching `*Async(..., ActionStatusCallback?)` variant.

### Feature guides

| Guide | Topics |
|-------|--------|
| [PREFERENCE.md](PREFERENCE.md) | Category/channel opt-in, `Preferences` API |
| [INBOX.md](INBOX.md) | `SuprsendInbox`, stores, real-time inbox updates |

### Push notifications

| Component | Role |
|-----------|------|
| `SSFirebaseMessagingService` | Default handler: registers FCM token, displays Suprsend notifications, invokes `NotificationCallbackListener` |
| `SSNotificationHelper.showFCMNotification` | Renders a Suprsend FCM payload (delivery/click/dismiss analytics included) |
| `RemoteMessage.isSuprSendRemoteMessage()` | Returns `true` when the data map contains the Suprsend payload key (`supr_send_n_pl`) |
| `notificationClicked(NotificationActionVo)` | Track notification tap analytics |
| `NotificationRedirectionActivity` | Handles deep links from notifications (declared in library manifest) |

Notification lifecycle events (`$notification_delivered`, `$notification_clicked`, `$notification_dismiss`) are queued offline when there is no network and flushed periodically.

#### SDK-managed (default)

If you declare `SSFirebaseMessagingService` in the manifest, the SDK registers the device token on refresh and displays incoming Suprsend pushes. Optionally observe payloads:

```kotlin
SuprSend.setNotificationCallback(object : NotificationCallbackListener {
    override fun onPushPayloadReceived(data: Map<String, String>) {
        // Invoked after the SDK processes the message
    }
})
```

#### Push notifications — application-managed

Use this approach when your app must own **FCM token registration** and **notification rendering** (for example, to mix Suprsend campaigns with your own push types, silent/data messages, or custom UI).

1. Declare **your** `FirebaseMessagingService` in the manifest (not `SSFirebaseMessagingService`).
2. On token refresh, send the token to Suprsend so the subscriber can receive pushes.
3. In `onMessageReceived`, branch on payload type: Suprsend vs everything else.

Reference implementation: `jwtApp` → `AppFirebaseMessagingService`.

```kotlin
import app.suprsend.SuprSend
import app.suprsend.notification.SSNotificationHelper
import app.suprsend.notification.isSuprSendRemoteMessage
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Register the device token with the current subscriber ($androidpush / FCM vendor).
        // Call after identify(); safe to call again when the token rotates.
        SuprSend.getInstance().user.setAndroidFcmPushAsync(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.isSuprSendRemoteMessage()) {
            // Suprsend campaign: use the SDK helper for rendering, channels, grouping,
            // deep links, and delivery/click/dismiss tracking.
            SSNotificationHelper.showFCMNotification(applicationContext, remoteMessage)

            // Optional: read extra data keys you attached in the Suprsend template
            // (exclude supr_send_n_pl — that key holds the structured notification JSON).
            // remoteMessage.data["your_custom_key"]?.let { ... }
        } else {
            // Non-Suprsend push: build your own notification or handle data-only payloads.
            val title = remoteMessage.notification?.title ?: remoteMessage.data["title"]
            val body = remoteMessage.notification?.body ?: remoteMessage.data["body"]
            // showYourNotification(title, body, remoteMessage.data)
        }
    }
}
```

| Step | API | Notes |
|------|-----|-------|
| Detect Suprsend payload | `remoteMessage.isSuprSendRemoteMessage()` | Checks for `supr_send_n_pl` in `RemoteMessage.data` |
| Render Suprsend UI | `SSNotificationHelper.showFCMNotification(context, remoteMessage)` | No-op if the message is not a Suprsend payload |
| Register FCM token | `user.setAndroidFcmPushAsync(token)` | Sync variant: `setAndroidFcmPush(token)` |
| Track notification tap | `SuprSend.getInstance().notificationClicked(...)` | After user opens a Suprsend notification |

### Callbacks & models

| Type | Purpose |
|------|---------|
| `ActionStatusCallback` | `onComplete(ApiResponse)` for async SDK calls |
| `ApiResponse` | `status`, `statusCode`, `body`, `message`, `errorType`, `exception`; `isSuccess()` |
| `Response<T>` | Inbox/preference results: `Success` / `Error` |
| `RefreshTokenCallback` | `getToken(distinctId: String): String` — pass to `identify` / `identityAsync`, not a standalone setter |
| `NotificationCallbackListener` | `onPushPayloadReceived(data: Map<String, String>)` |
| `LoggerCallback` | `v`, `i`, `e` log hooks |

---

## System events

Reserved event names tracked by the SDK:

| Event | Constant |
|-------|----------|
| `$app_installed` | `S_EVENT_APP_INSTALLED` |
| `$app_launched` | `S_EVENT_APP_LAUNCHED` |
| `$notification_delivered` | `S_EVENT_NOTIFICATION_DELIVERED` |
| `$notification_clicked` | `S_EVENT_NOTIFICATION_CLICKED` |
| `$notification_dismiss` | `S_EVENT_NOTIFICATION_DISMISS` |
| `$purchase_made` | `S_EVENT_PURCHASE_MADE` |
| `$notification_subscribed` | `S_EVENT_NOTIFICATION_SUBSCRIBED` |
| `$notification_unsubscribed` | `S_EVENT_NOTIFICATION_UNSUBSCRIBED` |
| `$page_visited` | `S_EVENT_PAGE_VISITED` |

---

## Property & validation notes

- Custom property keys starting with `$` or `ss_` are filtered from user payloads (unless `ignoreFilter` is used internally).
- Event names: max **120** characters; property values: max **512** characters.
- Identify fails if another user is already logged in — call `reset()` first.
- Offline notification events are stored locally (max **100**) and flushed every **10** seconds when online.

---

## Sample app

The **`jwtApp`** module demonstrates:

- `SuprSend.initialize` + JWT via `refreshTokenCallback` on `identify`
- `RefreshTokenCallback` fetching tokens from your backend on login
- Inbox UI with `SuprsendInbox` and `InboxStoreListener`
- User preference screens via `user.getPreferences()`
- Application-managed FCM (`AppFirebaseMessagingService`: token registration + `isSuprSendRemoteMessage` / `showFCMNotification`)

---

## Related docs

- [PREFERENCE.md](PREFERENCE.md) — notification preferences
- [INBOX.md](INBOX.md) — in-app inbox
- Workspace-level notes: [../README.md](../README.md)
- Suprsend product documentation: [https://docs.suprsend.com](https://docs.suprsend.com)
