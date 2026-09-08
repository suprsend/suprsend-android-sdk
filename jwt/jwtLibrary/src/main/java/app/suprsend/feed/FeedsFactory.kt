package app.suprsend.feed

import app.suprsend.SuprSend
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Creates and keeps track of [Feed] instances.
 *
 * Reachable as `SuprSend.getInstance().feeds`. Every call to [initialize] creates an independent
 * feed with its own state and socket connection, so remove the previous instance before creating
 * a new one for the same view.
 */
class FeedsFactory internal constructor(private val config: SuprSend) {

    private val instances = CopyOnWriteArrayList<Feed>()

    val feedInstances: List<Feed>
        get() = instances.toList()

    fun initialize(options: IFeedOptions? = null): Feed {
        val feed = Feed(config = config, options = options)
        instances.add(feed)
        return feed
    }

    fun removeInstance(feedClient: Feed) {
        val removed = instances.remove(feedClient)
        if (removed) {
            feedClient.remove()
        }
    }

    fun removeAll() {
        val copy = instances.toList()
        instances.clear()
        copy.forEach { it.remove() }
    }
}
