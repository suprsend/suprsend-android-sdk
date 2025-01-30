package app.suprsend.base

import android.annotation.SuppressLint
import android.content.Context
import app.suprsend.SuprSendInternal

internal object LocalStorage {
    private const val SUPRSEND_PREF = "suprsend_pref"

    fun getValue(key: String): String? {
        return SuprSendInternal.context.getSharedPreferences(SUPRSEND_PREF, Context.MODE_PRIVATE)
            .getString(key, "")
    }

    @SuppressLint("ApplySharedPref")
    fun setValue(key: String, value: String) {
        SuprSendInternal.context.getSharedPreferences(SUPRSEND_PREF, Context.MODE_PRIVATE).edit().apply {
            putString(key, value)
            commit()
        }
    }

    fun remove(key: String) {
        SuprSendInternal.context.getSharedPreferences(SUPRSEND_PREF, Context.MODE_PRIVATE).edit().apply {
            remove(key)
            commit()
        }
    }

}