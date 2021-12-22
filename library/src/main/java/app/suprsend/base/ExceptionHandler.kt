package app.suprsend.base

import android.os.Process
import app.suprsend.SSApi
import kotlin.system.exitProcess

class ExceptionHandler(
    private val suprSendApi: SSApi
) {
    fun track() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exc ->

            Logger.i(TAG, "Exception handler flush")
            suprSendApi.flush()

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, exc)
            } else
                killProcessAndExit()
        }
    }

    private fun killProcessAndExit() {
        try {
            Thread.sleep(400)
        } catch (e: InterruptedException) {
            Logger.e(TAG, "", e)
        }
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }
    companion object {
        const val TAG = "exc"
    }
}
