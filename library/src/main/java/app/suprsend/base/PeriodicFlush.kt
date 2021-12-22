package app.suprsend.base

import android.os.Handler
import android.os.HandlerThread
import app.suprsend.SSApiInternal

internal object PeriodicFlush {

    private var startPeriodicFlush = false

    private val handlerThread = HandlerThread("PeriodicFlushThread")
    private lateinit var handler: Handler

    init {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    fun start() {
        if (startPeriodicFlush) {
            return
        }
        startPeriodicFlush = true
        startInternal()
    }

    fun stop() {
        startPeriodicFlush = false
        handler.removeCallbacks(runnable)
    }


    private val runnable = Runnable {
        SSApiInternal.flush()
        if (startPeriodicFlush) {
            startInternal()
        }
    }

    private fun startInternal() {
        val time = SSConstants.PERIODIC_FLUSH_EVENT_IN_SEC * 1000L
        handler.postDelayed(runnable, time)
    }


}