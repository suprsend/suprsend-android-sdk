package app.suprsend.event

import app.suprsend.SSInternal
import app.suprsend.base.NetworkInfo
import app.suprsend.base.PeriodicJob
import app.suprsend.base.SSConstants
import app.suprsend.log.Logger

/**
 * Periodically flushes notification event payloads that were stored offline by
 * [PayloadOfflineStore].
 *
 * Events are drained one at a time in FIFO order:
 *  - Peek the oldest [Payload], POST its `payloadJson` to `/v2/event`
 *  - On success, drop it from local storage by its envelope id and continue
 *  - On failure or loss of connectivity, stop so the remaining events can retry on the next tick
 *
 * A single periodic job ticks every [SSConstants.OFFLINE_NOTIFICATION_EVENTS_FLUSH_PERIOD_IN_SEC]
 * seconds and is the only entry point for draining the queue.
 */
internal object EventFlushHandler {

    const val TAG = "offline_event_flush"

    private val periodicJob = PeriodicJob(
        periodInSec = SSConstants.OFFLINE_NOTIFICATION_EVENTS_FLUSH_PERIOD_IN_SEC,
        jobName = TAG
    ) { tick() }

    fun start() {
        periodicJob.start()
    }

    fun stop() {
        periodicJob.stop()
    }

    private fun tick() {
        try {
            if (!NetworkInfo.isConnected()) return
            if (!SSInternal.isSuprSendDataInitialized()) return
            if (PayloadOfflineStore.size() == 0) return
            flushInternal()
        } catch (e: Exception) {
            Logger.e(TAG, "Error while flushing offline payloads", e)
        }
    }

    private fun flushInternal() {
        var flushedCount = 0
        while (true) {
            if (!NetworkInfo.isConnected()) {
                Logger.i(TAG, "Internet lost while flushing, will retry on next tick")
                return
            }

            // The envelope id was assigned at store time and is owned by the store.
            // Producers running concurrently may evict this entry from the queue
            // (capacity-driven drop) while the HTTP call is in flight; removing by id
            // keeps the consumer safe — if the entry was already evicted, removeById is a
            // no-op and no unrelated payload is harmed.
            val entry = PayloadOfflineStore.peek() ?: break

            val response = SSInternal.networkClient.httpCall(
                url = "${SSInternal.suprSendData.baseUrl}/v2/event",
                authorization = SSInternal.suprSendData.publicApiKey ?: "",
                requestJson = entry.payloadJson,
                headers = SSInternal.addSSSignature()
            )
            if (!response.isSuccess()) {
                Logger.i(TAG, "Flush paused after $flushedCount payloads, will retry on next tick")
                return
            }

            PayloadOfflineStore.removeById(entry.id)
            flushedCount++
        }
        Logger.i(TAG, "Offline payload flush completed, flushed=$flushedCount")
    }
}
