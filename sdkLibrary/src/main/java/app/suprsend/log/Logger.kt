package app.suprsend.log

import android.util.Log
import app.suprsend.SuprSendInternal
internal object Logger {

    var logLevel = LogLevel.VERBOSE

    fun i(tag: String, message: String) {
        SuprSendInternal.loggerCallback?.i(tag, message)
        if (isLogAllowed(LogLevel.INFO.num))
            Log.i(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        SuprSendInternal.loggerCallback?.e(tag, message, throwable)
        if (isLogAllowed(LogLevel.ERROR.num))
            Log.e(tag, message, throwable)
    }

    fun e(tag: String, throwable: Throwable? = null) {
        SuprSendInternal.loggerCallback?.e(tag, "", throwable)
        if (isLogAllowed(LogLevel.ERROR.num))
            Log.e(tag, "", throwable)
    }

    private fun isLogAllowed(level: Int): Boolean {
        return logLevel.num <= level
    }

}
