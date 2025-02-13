package app.suprsend.android

import android.annotation.SuppressLint
import android.content.Context
import app.suprsend.SuprSend
import app.suprsend.android.AppCreator.context
import app.suprsend.android.AppCreator.getValue
import app.suprsend.inbox.InboxStore
import app.suprsend.inbox.SuprsendInbox
import app.suprsend.log.LogLevel
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONArray
import org.json.JSONObject

object CommonAnalyticsHandler {

    private lateinit var suprSend: SuprSend

    @SuppressLint("StaticFieldLeak")
    private lateinit var mixpanelAPI: MixpanelAPI

    fun initialize(context: Context) {
        if (this::suprSend.isInitialized)
            return
        suprSend = SuprSend.getInstance()
        suprSend.setLogLevel(LogLevel.VERBOSE)
        mixpanelAPI = MixpanelAPI.getInstance(context, BuildConfig.MX_TOKEN)
    }

    fun initializeInbox() {

        val subscriberId = getValue(AppConstants.PREF_INBOX_SUBSCRIBER_ID, BuildConfig.SS_INBOX_SUBSCRIBER_ID)
        val tenantId = getValue(AppConstants.PREF_TENANT_ID, "").ifBlank { null }

        val inboxStoreJson = getValue(AppConstants.PREF_INBOX_STORE_JSON, AppCreator.getInboxStoreJson(context))
        val inboxStoreList = if (inboxStoreJson.isBlank()) null else InboxStore.from(JSONArray(inboxStoreJson))

        val inboxThemeConfig = InboxThemeConfig(JSONObject(context.readStringFromAsset("inbox_screen_theme.json")))
        AppCreator.inboxThemeConfig = inboxThemeConfig

        SuprsendInbox.setBaseUrl("https://inbox-staging.inboxs.workers.dev")
        SuprsendInbox.setInboxSocketUrl("https://staging-inbox-api.suprsend.com")
        SuprsendInbox.setSubscriberId(subscriberId)
        SuprsendInbox.setTenantId(tenantId)
        SuprsendInbox.setInboxStores(inboxStoreList)
    }

    fun identify(identity: String) {
        suprSend.identityAsync(identity)
        suprSend.user.addEmailAsync(identity)
        mixpanelAPI.identify(identity)
    }

    fun track(eventName: String) {
        suprSend.trackEventAsync(eventName)
        mixpanelAPI.track(eventName)
    }

    fun track(eventName: String, properties: JSONObject) {
        suprSend.trackEventAsync(eventName, properties)
        mixpanelAPI.track(eventName, properties)
    }

    fun set(key: String, value: String) {
        suprSend.user.setAsync(key, value)
        mixpanelAPI.people.set(key, value)
    }

    fun set(properties: JSONObject) {
        suprSend.user.setAsync(properties)
        mixpanelAPI.people.set(properties)
    }

    fun increment(key: String, value: Number) {
        suprSend.user.incrementAsync(key, value)
        mixpanelAPI.people.increment(key, value.toDouble())
    }

    fun increment(properties: Map<String, Number>) {
        suprSend.user.incrementAsync(properties)
        mixpanelAPI.people.increment(properties)
    }

    fun append(key: String, value: String) {
        suprSend.user.appendAsync(key, value)
        mixpanelAPI.people.append(key, value)
    }

    fun remove(key: String, value: String) {
        suprSend.user.removeAsync(key, value)
        mixpanelAPI.people.remove(key, value)
    }

    fun setEmail(email: String) {
        suprSend.user.addEmailAsync(email)
    }

    fun unSetEmail(email: String) {
        suprSend.user.removeEmailAsync(email)
    }

    fun setSms(email: String) {
        suprSend.user.addSmsAsync(email)
    }

    fun unSetSms(email: String) {
        suprSend.user.removeSmsAsync(email)
    }

    fun setWhatsApp(email: String) {
        suprSend.user.addWhatsappAsync(email)
    }

    fun unSetWhatsApp(email: String) {
        suprSend.user.removeWhatsappAsync(email)
    }

    fun unset(key: String) {
        suprSend.user.unSetAsync(key)
        mixpanelAPI.people.unset(key)
    }

    fun reset(unSubscribeNotification: Boolean) {
        suprSend.resetAsync(unSubscribeNotification)
        mixpanelAPI.reset()
    }

    fun setOnce(key: String, value: Any) {
        suprSend.user.setOnceAsync(key, value)
        mixpanelAPI.people.setOnce(key, value)
    }

    fun setOnce(properties: JSONObject) {
        suprSend.user.setOnceAsync(properties)
        mixpanelAPI.people.setOnce(properties)
    }

    fun setSuperProperties(key: String, value: Any) {
        // Suprsend do not support super properties now
        // suprSend.setSuperProperty(key, value)
        mixpanelAPI.registerSuperPropertiesMap(hashMapOf(key to value))
    }

    fun setSuperProperties(jsonObject: JSONObject) {
        // Suprsend do not support super properties now
        // suprSend.setSuperProperty(key, value)
        mixpanelAPI.registerSuperProperties(jsonObject)
    }

    fun unSetSuperProperties(key: String) {
        // Suprsend do not support super properties now
        // suprSend.unSetSuperProperty(key)
        mixpanelAPI.unregisterSuperProperty(key)
    }

    fun setPreferredLanguage(languageCode: String) {
        suprSend.user.setPreferredLanguageAsync(languageCode)
    }

    fun purchaseMade(properties: JSONObject) {
        suprSend.trackEventAsync("purchase_made", properties)
    }
}
