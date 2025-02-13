package app.suprsend.log

import android.util.Log
import app.suprsend.SSInternal

internal object Logger {

    var logLevel = LogLevel.OFF

    fun i(tag: String, message: String) {
        SSInternal.loggerCallback?.i(tag, message)
        if (isLogAllowed(LogLevel.INFO.num))
            Log.i(tag, message)
    }

    fun v(tag: String, message: String) {
        SSInternal.loggerCallback?.v(tag, message)
        if (isLogAllowed(LogLevel.VERBOSE.num))
            Log.v(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        SSInternal.loggerCallback?.e(tag, message, throwable)
        if (isLogAllowed(LogLevel.ERROR.num))
            Log.e(tag, message, throwable)
    }

    fun e(tag: String, throwable: Throwable? = null) {
        SSInternal.loggerCallback?.e(tag, "", throwable)
        if (isLogAllowed(LogLevel.ERROR.num))
            Log.e(tag, "", throwable)
    }

    private fun isLogAllowed(level: Int): Boolean {
        return logLevel.num <= level
    }

}
