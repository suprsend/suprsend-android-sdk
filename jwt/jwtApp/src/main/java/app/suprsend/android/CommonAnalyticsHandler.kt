package app.suprsend.android

import app.suprsend.SuprSend
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject

object CommonAnalyticsHandler {

    private val suprSend: SuprSend by lazy { SuprSend.getInstance() }

    private val mixpanelAPI: MixpanelAPI by lazy { MixpanelAPI.getInstance(AppCreator.context, BuildConfig.MX_TOKEN) }

    fun identify(identity: String, tenantId: String? = null) {
        suprSend.identityAsync(identity, userToken = null, tenantId = tenantId)
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
        suprSend.user.unsetAsync(key)
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
