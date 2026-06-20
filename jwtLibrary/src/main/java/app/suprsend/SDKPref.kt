package app.suprsend

import app.suprsend.base.LocalStorage
import app.suprsend.base.SSConstants
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal object SDKPref {
    var distinctId: String? by pref(SSConstants.CONFIG_DISTINCT_ID)
    var distinctIdTry: String? by pref(SSConstants.CONFIG_DISTINCT_ID_TRY)
    var publicKey: String? by pref(SSConstants.CONFIG_PUBLIC_KEY)
    var host: String? by pref(SSConstants.CONFIG_HOST)
    var fcmToken: String? by pref(SSConstants.CONFIG_FCM_PUSH_TOKEN)
    var fcmTokenSyncedToServer: Boolean? by prefBoolean(SSConstants.CONFIG_FCM_TOKEN_SYNC_STATUS,false)
}

private fun pref(key: String): ReadWriteProperty<Any?, String?> = object : ReadWriteProperty<Any?, String?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String? = LocalStorage.getValue(key)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
            if (value == null) {
                LocalStorage.remove(key)
            } else {
                LocalStorage.setValue(key, value)
            }
        }
    }

private fun prefBoolean(key: String, defaultValue: Boolean?): ReadWriteProperty<Any?, Boolean?> = object : ReadWriteProperty<Any?, Boolean?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean? {
        return LocalStorage.getValue(key)?.ifBlank { null }?.toBoolean() ?: defaultValue
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean?) {
        if (value == null) {
            LocalStorage.remove(key)
        } else {
            LocalStorage.setValue(key, value.toString())
        }
    }
}