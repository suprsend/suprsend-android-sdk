package app.suprsend.base

import android.annotation.SuppressLint
import android.content.Context
import app.suprsend.SSInternal

internal object LocalStorage {
    private const val SUPRSEND_PREF = "suprsend_pref"

    fun getValue(key: String): String? {
        return SSInternal.context.getSharedPreferences(SUPRSEND_PREF, Context.MODE_PRIVATE)
            .getString(key, "")
    }

    @SuppressLint("ApplySharedPref")
    fun setValue(key: String, value: String) {
        SSInternal.context.getSharedPreferences(SUPRSEND_PREF, Context.MODE_PRIVATE).edit().apply {
            putString(key, value)
            commit()
        }
    }

    @SuppressLint("ApplySharedPref")
    fun setValue(key: String, value: Int) {
        SSInternal.context.getSharedPreferences(SUPRSEND_PREF, Context.MODE_PRIVATE).edit().apply {
            putInt(key, value)
            commit()
        }
    }

    fun getIntValue(key: String, defaultValue: Int = -1): Int? {
        val pref = SSInternal.context.getSharedPreferences(SUPRSEND_PREF, Context.MODE_PRIVATE)
        return if (!pref.contains(key)) {
            return null
        } else {
            pref.getInt(key, defaultValue)
        }
    }

    fun remove(key: String) {
        SSInternal.context.getSharedPreferences(SUPRSEND_PREF, Context.MODE_PRIVATE).edit().apply {
            remove(key)
            commit()
        }
    }

}