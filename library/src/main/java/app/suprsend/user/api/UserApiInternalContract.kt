package app.suprsend.user.api

import app.suprsend.user.preference.Preferences
import org.json.JSONObject

interface UserApiInternalContract {

    fun set(key: String, value: Any)
    fun set(properties: JSONObject)
    fun unSet(key: String)
    fun unSet(keys: List<String>)

    fun setOnce(key: String, value: Any)
    fun setOnce(properties: JSONObject)

    fun increment(key: String, value: Number)
    fun increment(properties: Map<String, Number>)

    fun append(key: String, value: Any)
    fun append(properties: JSONObject)

    fun remove(key: String, value: Any)
    fun remove(properties: JSONObject)

    fun setEmail(email: String)
    fun unSetEmail(email: String)

    fun setSms(mobile: String)
    fun unSetSms(mobile: String)

    fun setWhatsApp(mobile: String)
    fun unSetWhatsApp(mobile: String)

    fun setAndroidFcmPush(token: String)
    fun unSetAndroidFcmPush(token: String)

    fun setAndroidXiaomiPush(token: String)
    fun unSetAndroidXiaomiPush(token: String)

    fun setPreferredLanguage(languageCode: String)
    fun getPreferences(): Preferences

    fun notificationClicked(id: String, actionId: String? = null)
}
