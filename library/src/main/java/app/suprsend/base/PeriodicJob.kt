package app.suprsend.base

import android.os.Handler
import android.os.HandlerThread

class PeriodicJob(
    private val periodInSec: Int,
    private val jobName: String,
    job: () -> Unit
) {
    private val handlerThread = HandlerThread(jobName + "Thread")
    private var handler: Handler
    var isScheduled = false

    init {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }


    fun start() {
        if (isScheduled) {
            return
        }
        Logger.i(SSConstants.TAG_SUPRSEND, "$jobName - Periodic Job started every $periodInSec sec")
        isScheduled = true
        startInternal()
    }

    fun stop() {
        isScheduled = false
        handler.removeCallbacks(runnable)
        Logger.i(SSConstants.TAG_SUPRSEND, "$jobName - Periodic Job stopped")
    }

    private val runnable = Runnable {
        job.invoke()
        if (isScheduled) {
            startInternal()
        }
    }

    private fun startInternal() {
        val time = periodInSec * 1000L
        handler.postDelayed(runnable, time)
    }


}