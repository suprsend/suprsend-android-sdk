package app.suprsend.feed

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Publishes [InboxEmitterEvents] of a single [Feed] instance. Events are always delivered on
 * the main thread so listeners can update the UI directly.
 */
class FeedEmitter {

    private val listeners = CopyOnWriteArrayList<(InboxEmitterEvents) -> Unit>()

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    /**
     * Registers [listener] for feed updates.
     *
     * @return a function which removes [listener] again.
     */
    fun subscribe(listener: (InboxEmitterEvents) -> Unit): () -> Unit {
        listeners.add(listener)
        return { listeners.remove(listener) }
    }

    internal fun send(event: InboxEmitterEvents) {
        if (listeners.isEmpty()) return
        mainHandler.post {
            listeners.forEach { it(event) }
        }
    }

    internal fun complete() {
        listeners.clear()
    }
}
