# Notification preferences

Subscriber notification preference APIs in the JWT library. Use these to build opt-in/opt-out UI for categories and channels.

**Entry point:** `SuprSend.getInstance().user.getPreferences()` → `Preferences`

See also: [README.md](README.md) (setup & auth), [INBOX.md](INBOX.md) (in-app inbox).

---

## Prerequisites

1. Call `SuprSend.initialize(...)` and identify the user (`identityAsync` / `identify`).
2. If using JWT mode, configure `SuprSend.setUserTokenFetcher(...)` — the same JWT flow applies as for events and inbox.
3. Configure tenant and UI options once per session:

```kotlin
SuprSend.getInstance().user.getPreferences().setPreferenceConfig(
    tenantId = "your_tenant",      // optional, multi-tenant workspaces
    showOptOutChannels = true      // default true
)
```

---

## Environment URLs

Preferences use the same base URL as events (`SuprSend.initialize` → `baseUrl`):

| Environment | Base URL |
|-------------|----------|
| Production | `https://hub.suprsend.com` |
| Staging | `https://collector-staging.suprsend.workers.dev` |

---

## SDK API — `Preferences`

Methods marked **sync** use `@WorkerThread`. Prefer calling from a background thread or use patterns that delegate off the main thread.

| Method | Description |
|--------|-------------|
| `setPreferenceConfig(tenantId?, showOptOutChannels)` | Tenant id and whether to include opt-out channels in responses |
| `registerCallback(PreferenceCallback)` | Listen for preference updates and errors |
| `unRegisterCallback()` | Remove listener |
| `fetchUserPreference(fetchRemote)` | Full preference tree → `PreferenceData` |
| `fetchCategories(limit?, offset?)` | Paginated categories → `JSONObject` |
| `fetchCategory(category)` | Single category |
| `fetchOverallChannelPreferences()` | Channel-level preferences |
| `updateCategoryPreference(category, PreferenceOptions)` | Category opt-in/out |
| `updateChannelPreferenceInCategory(category, channel, PreferenceOptions)` | Channel within a category |
| `updateOverallChannelPreference(channel, ChannelPreferenceOptions)` | Global channel restriction |

### Enums

**`PreferenceOptions`**

| Value | Meaning |
|-------|---------|
| `OPT_IN` | User opted in |
| `OPT_OUT` | User opted out |

**`ChannelPreferenceOptions`**

| Value | Meaning |
|-------|---------|
| `ALL` | All messages for the channel |
| `REQUIRED` | Required / restricted channel handling |

### `PreferenceCallback`

```kotlin
interface PreferenceCallback {
    fun onUpdate(preferenceData: PreferenceData)
    fun onError(response: Response<JSONObject>)
}
```

### `PreferenceData`

Parsed model returned by `fetchUserPreference`:

```kotlin
data class PreferenceData(
    val sections: List<Section> = listOf(),
    val channelPreferences: List<ChannelPreference> = listOf()
)
```

Related types: `Section`, `SubCategory`, `Channel`, `ChannelPreference` in `app.suprsend.user.preference`.

### Example — load and listen

```kotlin
val preferences = SuprSend.getInstance().user.getPreferences()

preferences.setPreferenceConfig(tenantId = AppCreator.tenantId)
preferences.registerCallback(object : PreferenceCallback {
    override fun onUpdate(preferenceData: PreferenceData) {
        // Refresh UI
    }
    override fun onError(response: Response<JSONObject>) {
        // Show error
    }
})

// On a background thread
val result = preferences.fetchUserPreference(fetchRemote = true)
when (result) {
    is Response.Success -> { /* use result.data */ }
    is Response.Error -> { /* handle */ }
}
```

### Example — update category

```kotlin
preferences.updateCategoryPreference(
    category = "marketing",
    preference = PreferenceOptions.OPT_OUT
)
```

Category updates are debounced (~2s) per category to avoid rapid duplicate server updates.

---

## Notes

- User must be identified before preference APIs succeed; distinct id comes from `SuprSend.getInstance().getDistinctId()`.
- `reset()` clears cached preference data via `SSPreferenceInternal.clearUserPreference()`.
- No network: `updateCategoryPreference` returns `Response.Error(NoInternetException())` and may still invoke callback `onUpdate` with cached data.

---

## Sample app

See **`jwtApp`** → `UserPreferenceActivity` for a full preference screen integration.
