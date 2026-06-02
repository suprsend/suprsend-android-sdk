package app.suprsend.event

import app.suprsend.SSInternal
import app.suprsend.base.BaseTest
import app.suprsend.base.LocalStorage
import app.suprsend.base.SSConstants
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class PayloadOfflineStoreTest : BaseTest() {

    @Before
    fun setup() {
        SSInternal.context = context.applicationContext
        PayloadOfflineStore.clear()
    }

    private fun userPayload(eventName: String, idTag: String): JSONObject {
        return JSONObject().apply {
            put(SSConstants.EVENT, eventName)
            put(SSConstants.DISTINCT_ID, "user-1")
            put(SSConstants.INSERT_ID, "insert-$idTag")
            put(SSConstants.TIME, System.currentTimeMillis())
            put(SSConstants.PROPERTIES, JSONObject().apply { put("tag", idTag) })
        }
    }

    /**
     * White-box read of the on-disk SP entry. The store's public surface is intentionally
     * narrow (`store / peek / removeById / size / clear`), so tests verify state by parsing
     * the underlying JSON array directly rather than draining via peek+remove.
     */
    private fun rawEnvelopes(): List<Payload> {
        val raw = LocalStorage.getValue(SSConstants.OFFLINE_NOTIFICATION_EVENTS) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        val array = JSONArray(raw)
        val result = ArrayList<Payload>(array.length())
        for (i in 0 until array.length()) {
            val entry = array.optJSONObject(i) ?: continue
            val id = entry.optString("id")
            val payload = entry.optJSONObject("payload") ?: continue
            if (id.isNullOrBlank()) continue
            result.add(Payload(id, payload.toString()))
        }
        return result
    }

    private fun tagOf(payload: Payload): String =
        JSONObject(payload.payloadJson).getJSONObject(SSConstants.PROPERTIES).getString("tag")

    // ---------------------------- basic ----------------------------

    @Test
    fun emptyStore_sizeIsZeroAndPeekIsNull() {
        Assert.assertEquals(0, PayloadOfflineStore.size())
        Assert.assertNull(PayloadOfflineStore.peek())
    }

    @Test
    fun store_returnsFreshUuidPerInsert() {
        val id1 = PayloadOfflineStore.store(userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "M1"))
        val id2 = PayloadOfflineStore.store(userPayload(SSConstants.S_EVENT_NOTIFICATION_CLICKED, "M2"))

        Assert.assertTrue(id1.isNotBlank())
        Assert.assertTrue(id2.isNotBlank())
        Assert.assertNotEquals(id1, id2)
        // Ids returned by store must match envelope ids on disk, in insertion order.
        Assert.assertEquals(listOf(id1, id2), rawEnvelopes().map { it.id })
    }

    @Test
    fun store_wrapsUserPayloadInEnvelope() {
        val user = userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "M1")
        val id = PayloadOfflineStore.store(user)

        val raw = LocalStorage.getValue(SSConstants.OFFLINE_NOTIFICATION_EVENTS) ?: ""
        val array = JSONArray(raw)
        Assert.assertEquals(1, array.length())
        val envelope = array.getJSONObject(0)
        Assert.assertEquals(id, envelope.getString("id"))
        // The user payload nested inside the envelope is intact.
        Assert.assertEquals(
            user.getString(SSConstants.EVENT),
            envelope.getJSONObject("payload").getString(SSConstants.EVENT)
        )
        Assert.assertEquals(
            "M1",
            envelope.getJSONObject("payload").getJSONObject(SSConstants.PROPERTIES).getString("tag")
        )
    }

    @Test
    fun peek_returnsPayloadJsonAsWireReadyString() {
        val user = userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "M1")
        PayloadOfflineStore.store(user)

        val head = PayloadOfflineStore.peek()
        Assert.assertNotNull(head)
        Assert.assertTrue(head!!.payloadJson.isNotBlank())
        // Re-parsing must yield the original user payload structure.
        val reparsed = JSONObject(head.payloadJson)
        Assert.assertEquals(user.getString(SSConstants.EVENT), reparsed.getString(SSConstants.EVENT))
        Assert.assertEquals(user.getString(SSConstants.DISTINCT_ID), reparsed.getString(SSConstants.DISTINCT_ID))
        Assert.assertEquals(
            user.getJSONObject(SSConstants.PROPERTIES).getString("tag"),
            reparsed.getJSONObject(SSConstants.PROPERTIES).getString("tag")
        )
    }

    @Test
    fun peek_returnsOldestPayloadWithItsEnvelopeId() {
        val id1 = PayloadOfflineStore.store(userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "M1"))
        PayloadOfflineStore.store(userPayload(SSConstants.S_EVENT_NOTIFICATION_CLICKED, "M2"))

        val head = PayloadOfflineStore.peek()
        Assert.assertNotNull(head)
        Assert.assertEquals(id1, head!!.id)
        Assert.assertEquals("M1", tagOf(head))
        // Peeking is idempotent; queue size unchanged.
        Assert.assertEquals(id1, PayloadOfflineStore.peek()!!.id)
        Assert.assertEquals(2, PayloadOfflineStore.size())
    }

    // ---------------------------- removeById ----------------------------

    @Test
    fun removeById_removesTargetAndPreservesOrder() {
        val id1 = PayloadOfflineStore.store(userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "M1"))
        val id2 = PayloadOfflineStore.store(userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "M2"))
        val id3 = PayloadOfflineStore.store(userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "M3"))

        Assert.assertTrue(PayloadOfflineStore.removeById(id2))

        // M1 should now be the head and M3 should be next; M2 is gone.
        val head = PayloadOfflineStore.peek()
        Assert.assertEquals(id1, head!!.id)
        PayloadOfflineStore.removeById(id1)
        Assert.assertEquals(id3, PayloadOfflineStore.peek()!!.id)
        Assert.assertEquals(1, PayloadOfflineStore.size())
    }

    @Test
    fun removeById_unknownIdReturnsFalse() {
        PayloadOfflineStore.store(userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "M1"))

        Assert.assertFalse(PayloadOfflineStore.removeById(UUID.randomUUID().toString()))
        Assert.assertEquals(1, PayloadOfflineStore.size())
    }

    @Test
    fun removeById_blankIdIsNoop() {
        PayloadOfflineStore.store(userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "M1"))

        Assert.assertFalse(PayloadOfflineStore.removeById(""))
        Assert.assertFalse(PayloadOfflineStore.removeById("   "))
        Assert.assertEquals(1, PayloadOfflineStore.size())
    }

    @Test
    fun removeById_lastEntryClearsUnderlyingKey() {
        val id = PayloadOfflineStore.store(userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "M1"))

        Assert.assertTrue(PayloadOfflineStore.removeById(id))

        Assert.assertEquals(0, PayloadOfflineStore.size())
        val raw = LocalStorage.getValue(SSConstants.OFFLINE_NOTIFICATION_EVENTS) ?: ""
        Assert.assertTrue(raw.isBlank())
    }

    // ---------------------------- capacity ----------------------------

    @Test
    fun store_dropsOldestWhenAtCapacity() {
        val capacity = SSConstants.OFFLINE_NOTIFICATION_EVENTS_MAX_COUNT
        val firstId = PayloadOfflineStore.store(userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "M0"))
        repeat(capacity - 1) { i ->
            PayloadOfflineStore.store(userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "M${i + 1}"))
        }
        Assert.assertEquals(capacity, PayloadOfflineStore.size())

        val newId = PayloadOfflineStore.store(userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "M_NEW"))

        Assert.assertEquals(capacity, PayloadOfflineStore.size())
        // The freshly-evicted id (the very first one) must be gone.
        Assert.assertFalse(PayloadOfflineStore.removeById(firstId))
        // The newest id must still be present.
        Assert.assertTrue(PayloadOfflineStore.removeById(newId))
    }

    // ---------------------------- recovery ----------------------------

    @Test
    fun store_recoversFromCorruptedUnderlyingValue() {
        LocalStorage.setValue(SSConstants.OFFLINE_NOTIFICATION_EVENTS, "{not-a-json-array}")

        val id = PayloadOfflineStore.store(userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "M1"))

        Assert.assertEquals(1, PayloadOfflineStore.size())
        Assert.assertEquals(id, PayloadOfflineStore.peek()!!.id)
    }

    @Test
    fun peek_skipsMalformedEnvelopesAtHead() {
        // Manually craft a mix of malformed and valid envelopes on disk.
        val onDisk = JSONArray()
        // Envelope without id — should be skipped.
        onDisk.put(JSONObject().apply { put("payload", JSONObject()) })
        // Envelope without payload — should be skipped.
        onDisk.put(JSONObject().apply { put("id", "garbage-1") })
        // A valid envelope.
        val validId = UUID.randomUUID().toString()
        onDisk.put(JSONObject().apply {
            put("id", validId)
            put("payload", userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "valid"))
        })
        LocalStorage.setValue(SSConstants.OFFLINE_NOTIFICATION_EVENTS, onDisk.toString())

        val head = PayloadOfflineStore.peek()
        Assert.assertNotNull(head)
        Assert.assertEquals(validId, head!!.id)
        Assert.assertEquals("valid", tagOf(head))
    }

    // ---------------------------- consumer race scenario ----------------------------

    /**
     * Sequential reproducer of the multi-producer / single-consumer race that motivated the
     * envelope-id design. The consumer peeks an entry; before it can finish its HTTP call,
     * producers fill the queue and evict that very entry. With removeById, the consumer's
     * subsequent remove is a no-op and no other payload is harmed.
     */
    @Test
    fun removeById_isSafeWhenProducerEvictedPeekedEntryDuringFlush() {
        val capacity = SSConstants.OFFLINE_NOTIFICATION_EVENTS_MAX_COUNT
        val peekedId = PayloadOfflineStore.store(userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "peeked"))
        repeat(capacity - 1) { i ->
            PayloadOfflineStore.store(userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "old-$i"))
        }
        Assert.assertEquals(capacity, PayloadOfflineStore.size())

        val peeked = PayloadOfflineStore.peek()
        Assert.assertNotNull(peeked)
        Assert.assertEquals(peekedId, peeked!!.id)

        // Producer fills queue → eviction of the head (the entry the consumer peeked).
        val freshId = PayloadOfflineStore.store(userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "fresh"))
        Assert.assertEquals(capacity, PayloadOfflineStore.size())

        // Consumer attempts to dequeue the peeked entry by id. It's already gone, so the call
        // is a no-op — crucially, the fresh entry must remain intact.
        Assert.assertFalse(PayloadOfflineStore.removeById(peekedId))
        Assert.assertTrue("fresh id must not be dropped", rawEnvelopes().any { it.id == freshId })
    }

    // ---------------------------- concurrency stress ----------------------------

    @Test
    fun concurrentProducers_storeAllUniqueIdsBelowCapacity() {
        val producers = 10
        val perProducer = 5
        val executor = Executors.newFixedThreadPool(producers)
        val startGate = CountDownLatch(1)
        val doneGate = CountDownLatch(producers)
        val returnedIds = java.util.Collections.synchronizedList(ArrayList<String>())

        try {
            repeat(producers) { p ->
                executor.submit {
                    try {
                        startGate.await()
                        repeat(perProducer) { i ->
                            returnedIds.add(
                                PayloadOfflineStore.store(
                                    userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "p$p-i$i")
                                )
                            )
                        }
                    } finally {
                        doneGate.countDown()
                    }
                }
            }
            startGate.countDown()
            Assert.assertTrue(doneGate.await(10, TimeUnit.SECONDS))
        } finally {
            executor.shutdownNow()
        }

        val expected = producers * perProducer
        Assert.assertEquals(expected, PayloadOfflineStore.size())
        // Store-returned ids and on-disk envelope ids must be the same set; no torn writes.
        val onDiskIds = rawEnvelopes().map { it.id }.toSet()
        Assert.assertEquals(expected, onDiskIds.size)
        Assert.assertEquals(returnedIds.toSet(), onDiskIds)
    }

    @Test
    fun concurrentProducerAndConsumer_consumerNeverDropsExtraEntries() {
        val producers = 8
        val perProducer = 200 // 1600 stores -> well above capacity, forces evictions
        val capacity = SSConstants.OFFLINE_NOTIFICATION_EVENTS_MAX_COUNT

        val executor = Executors.newFixedThreadPool(producers + 1)
        val stop = AtomicBoolean(false)
        val flushedCount = AtomicInteger(0)
        val consumerErrors = AtomicInteger(0)
        val startGate = CountDownLatch(1)
        val producerDone = CountDownLatch(producers)

        try {
            repeat(producers) { p ->
                executor.submit {
                    try {
                        startGate.await()
                        repeat(perProducer) { i ->
                            PayloadOfflineStore.store(
                                userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "p$p-i$i-${UUID.randomUUID()}")
                            )
                        }
                    } finally {
                        producerDone.countDown()
                    }
                }
            }

            executor.submit {
                try {
                    startGate.await()
                    while (!stop.get()) {
                        val head = PayloadOfflineStore.peek() ?: continue
                        if (PayloadOfflineStore.removeById(head.id)) {
                            flushedCount.incrementAndGet()
                        }
                    }
                } catch (e: Exception) {
                    consumerErrors.incrementAndGet()
                }
            }

            startGate.countDown()
            Assert.assertTrue(producerDone.await(15, TimeUnit.SECONDS))
            Thread.sleep(100)
            stop.set(true)
        } finally {
            executor.shutdownNow()
            executor.awaitTermination(2, TimeUnit.SECONDS)
        }

        Assert.assertEquals("consumer should not encounter errors", 0, consumerErrors.get())
        val finalSize = PayloadOfflineStore.size()
        Assert.assertTrue("size must be >= 0", finalSize >= 0)
        Assert.assertTrue("size must be <= capacity", finalSize <= capacity)
        // All remaining ids must still be unique — no torn writes / duplicates.
        val remainingIds = rawEnvelopes().map { it.id }
        Assert.assertEquals(remainingIds.size, remainingIds.toSet().size)
    }

    @Test
    fun concurrentProducers_capacityInvariantHolds() {
        val producers = 6
        val perProducer = SSConstants.OFFLINE_NOTIFICATION_EVENTS_MAX_COUNT
        val executor = Executors.newFixedThreadPool(producers)
        val startGate = CountDownLatch(1)
        val doneGate = CountDownLatch(producers)

        try {
            repeat(producers) { p ->
                executor.submit {
                    try {
                        startGate.await()
                        repeat(perProducer) { i ->
                            PayloadOfflineStore.store(
                                userPayload(SSConstants.S_EVENT_NOTIFICATION_DELIVERED, "p$p-i$i")
                            )
                        }
                    } finally {
                        doneGate.countDown()
                    }
                }
            }
            startGate.countDown()
            Assert.assertTrue(doneGate.await(15, TimeUnit.SECONDS))
        } finally {
            executor.shutdownNow()
        }

        Assert.assertEquals(SSConstants.OFFLINE_NOTIFICATION_EVENTS_MAX_COUNT, PayloadOfflineStore.size())
        // Every retained entry on disk should still be a valid envelope (id + payload).
        val all = rawEnvelopes()
        Assert.assertEquals(SSConstants.OFFLINE_NOTIFICATION_EVENTS_MAX_COUNT, all.size)
        Assert.assertTrue(all.all { it.id.isNotBlank() })
    }
}
