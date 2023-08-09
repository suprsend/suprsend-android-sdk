package app.suprsend

import app.suprsend.base.executorService
import app.suprsend.user.api.SSInternalUser
import app.suprsend.user.api.UserApiInternalContract
import app.suprsend.user.preference.Preferences
import app.suprsend.user.preference.PreferencesImpl
import org.json.JSONObject

class SSUserApi : UserApiInternalContract {

    val preference = PreferencesImpl()

    override fun set(key: String, value: Any) {
        executorService.execute {
            SSInternalUser.set(key, value)
        }
    }

    override fun set(properties: JSONObject) {
        executorService.execute {
            SSInternalUser.set(properties)
        }
    }

    override fun setOnce(key: String, value: Any) {
        executorService.execute {
            SSInternalUser.setOnce(key, value)
        }
    }

    override fun setOnce(properties: JSONObject) {
        executorService.execute {
            SSInternalUser.setOnce(properties)
        }
    }

    override fun increment(key: String, value: Number) {
        executorService.execute {
            SSInternalUser.increment(key, value)
        }
    }

    override fun increment(properties: Map<String, Number>) {
        executorService.execute {
            SSInternalUser.increment(properties)
        }
    }

    override fun append(key: String, value: Any) {
        executorService.execute {
            SSInternalUser.append(key, value)
        }
    }

    override fun append(properties: JSONObject) {
        executorService.execute {
            SSInternalUser.append(properties)
        }
    }

    override fun remove(key: String, value: Any) {
        executorService.execute {
            SSInternalUser.remove(key, value)
        }
    }

    override fun remove(properties: JSONObject) {
        executorService.execute {
            SSInternalUser.remove(properties)
        }
    }

    override fun unSet(key: String) {
        executorService.execute {
            SSInternalUser.unSet(key)
        }
    }

    override fun unSet(keys: List<String>) {
        executorService.execute {
            SSInternalUser.unSet(keys)
        }
    }

    override fun setEmail(email: String) {
        executorService.execute {
            SSInternalUser.setEmail(email)
        }
    }

    override fun unSetEmail(email: String) {
        executorService.execute {
            SSInternalUser.unSetEmail(email)
        }
    }

    override fun setSms(mobile: String) {
        executorService.execute {
            SSInternalUser.setSms(mobile)
        }
    }

    override fun unSetSms(mobile: String) {
        executorService.execute {
            SSInternalUser.unSetSms(mobile)
        }
    }

    override fun setWhatsApp(mobile: String) {
        executorService.execute {
            SSInternalUser.setWhatsApp(mobile)
        }
    }

    override fun unSetWhatsApp(mobile: String) {
        executorService.execute {
            SSInternalUser.unSetWhatsApp(mobile)
        }
    }

    override fun setAndroidFcmPush(token: String) {
        executorService.execute {
            SSInternalUser.setAndroidFcmPush(token)
            SSApiInternal.flush()
        }
    }

    override fun unSetAndroidFcmPush(token: String) {
        executorService.execute {
            SSInternalUser.unSetAndroidFcmPush(token)
            SSApiInternal.flush()
        }
    }

    override fun setAndroidXiaomiPush(token: String) {
        executorService.execute {
            SSInternalUser.setAndroidXiaomiPush(token)
            SSApiInternal.flush()
        }
    }

    override fun unSetAndroidXiaomiPush(token: String) {
        executorService.execute {
            SSInternalUser.unSetAndroidXiaomiPush(token)
            SSApiInternal.flush()
        }
    }

    override fun setPreferredLanguage(languageCode: String) {
        executorService.execute {
            SSInternalUser.setPreferredLanguage(languageCode)
        }
    }

    override fun getPreferences(): Preferences {
        return preference
    }
}
