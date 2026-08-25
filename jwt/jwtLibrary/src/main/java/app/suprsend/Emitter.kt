package app.suprsend

import app.suprsend.user.preference.PreferenceAPIResponse
import java.util.concurrent.CopyOnWriteArrayList

class Emitter {

    enum class Event {
        preferencesUpdated,
        preferencesError
    }

    private val listeners = mutableMapOf<Event, CopyOnWriteArrayList<(PreferenceAPIResponse?) -> Unit>>()

    fun on(event: Event, callback: (PreferenceAPIResponse?) -> Unit) {
        synchronized(listeners) {
            listeners.getOrPut(event) { CopyOnWriteArrayList() }.add(callback)
        }
    }

    internal fun emit(event: Event, data: PreferenceAPIResponse) {
        val callbacks = synchronized(listeners) {
            listeners[event]?.toList() ?: emptyList()
        }
        callbacks.forEach { callback ->
            callback(data)
        }
    }

    internal fun clear() {
        synchronized(listeners) {
            listeners.clear()
        }
    }
}
