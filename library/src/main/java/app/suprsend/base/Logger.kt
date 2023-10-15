package app.suprsend.base

import android.util.Log
import app.suprsend.SSApiInternal

internal object Logger {

    var logLevel = LogLevel.OFF

    fun i(tag: String, message: String) {
        SSApiInternal.loggerCallback?.i(tag, message)
        if (isLogAllowed(LogLevel.INFO.num))
            Log.i(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        SSApiInternal.loggerCallback?.e(tag, message, throwable)
        if (isLogAllowed(LogLevel.ERROR.num))
            Log.e(tag, message, throwable)
    }

    private fun isLogAllowed(level: Int): Boolean {
        return logLevel.num <= level
    }
}
