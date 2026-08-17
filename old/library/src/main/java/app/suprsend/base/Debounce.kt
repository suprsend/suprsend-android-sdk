package app.suprsend.base

import android.os.Handler
import android.os.Looper

class Debounce {
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable?

    init {
        runnable = null
    }

    fun debounceLast(action: () -> Unit) {
        if (runnable != null) {
            handler.removeCallbacks(runnable!!)
        }
        val runnable = Runnable {
            action.invoke()
            runnable = null
        }
        this.runnable = runnable
        handler.postDelayed(runnable, DEBOUNCE_DELAY.toLong())
    }

    fun debounceFirst(action: () -> Unit) {
        if (runnable != null) {
            return  // Ignore subsequent actions within the debounce delay
        }
        action.invoke()
        val runnable = Runnable {
            this.runnable = null
        }
        this.runnable = runnable
        handler.postDelayed(runnable, DEBOUNCE_DELAY.toLong())
    }

    companion object {
        private const val DEBOUNCE_DELAY = 400 // Set your desired debounce delay in milliseconds
    }
}