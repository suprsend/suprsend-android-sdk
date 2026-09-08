package app.suprsend.feed

import app.suprsend.SuprSend
import app.suprsend.model.ErrorType
import app.suprsend.model.ResponseStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class FeedTest : FeedTestHelper() {

    @Before
    fun setup() {
        identifyUser()
    }

    @Test
    fun initializeTracksFeedInstance() {
        val feeds = SuprSend.getInstance().feeds
        Assert.assertEquals(0, feeds.feedInstances.size)

        val feed = createFeed()
        Assert.assertEquals(1, feeds.feedInstances.size)
        Assert.assertTrue(feeds.feedInstances.contains(feed))

        feeds.removeInstance(feed)
        Assert.assertEquals(0, feeds.feedInstances.size)
    }

    @Test
    fun fetchCountUpdatesBadgeMeta() = runBlocking {
        val feed = createFeed()
        val response = feed.fetchCount()

        Assert.assertEquals(ResponseStatus.SUCCESS, response.status)
        Assert.assertEquals(3, response.body?.badge)
        Assert.assertEquals(1, response.body?.storeCounts?.get("Read"))
        Assert.assertEquals("3", feed.data.meta["badge"])
        Assert.assertEquals("1", feed.data.meta["Read"])
    }

    @Test
    fun fetchPopulatesNotificationsAndPageInfo() = runBlocking {
        val feed = createFeed()
        val response = feed.fetch()

        Assert.assertEquals(ResponseStatus.SUCCESS, response.status)
        Assert.assertEquals(2, response.body?.results?.size)
        Assert.assertEquals(2, feed.data.notifications.size)
        Assert.assertEquals("n-unread-1", feed.data.notifications[0].n_id)
        Assert.assertEquals("Unread header", feed.data.notifications[0].message.header)
        Assert.assertEquals(2, feed.data.pageInfo.total)
        Assert.assertEquals(false, feed.data.pageInfo.hasMore)
        Assert.assertEquals(APIResponseStatus.SUCCESS, feed.data.apiStatus)
    }

    @Test
    fun fetchNextPageWithoutMorePagesReturnsValidationError() = runBlocking {
        val feed = createFeed()
        feed.fetch()

        val response = feed.fetchNextPage()
        Assert.assertEquals(ResponseStatus.ERROR, response.status)
        Assert.assertEquals(ErrorType.VALIDATION_ERROR, response.error?.type)
        Assert.assertEquals("No more pages to fetch", response.error?.message)
    }

    @Test
    fun changeActiveStoreRejectsUnknownStore() = runBlocking {
        val feed = createFeed(stores = storesFromAsset())
        val response = feed.changeActiveStore("missing")

        Assert.assertEquals(ResponseStatus.ERROR, response.status)
        Assert.assertEquals(ErrorType.VALIDATION_ERROR, response.error?.type)
        Assert.assertTrue(response.error?.message?.contains("missing") == true)
    }

    @Test
    fun changeActiveStoreSwitchesAndRefetches() = runBlocking {
        val stores = storesFromAsset()
        val feed = createFeed(stores = stores)

        Assert.assertEquals("Read", feed.data.store.storeId)

        val response = feed.changeActiveStore("Unread")
        Assert.assertEquals(ResponseStatus.SUCCESS, response.status)
        Assert.assertEquals("Unread", feed.data.store.storeId)
        Assert.assertEquals(2, feed.data.notifications.size)
    }

    @Test
    fun markAsReadSetsReadOnOptimistically() = runBlocking {
        val feed = createFeed()
        feed.fetch()

        val response = feed.markAsRead("n-unread-1")
        Assert.assertEquals(ResponseStatus.SUCCESS, response.status)
        val updated = feed.data.notifications.first { it.n_id == "n-unread-1" }
        Assert.assertNotNull(updated.read_on)
        Assert.assertNotNull(updated.seen_on)
    }

    @Test
    fun markBulkAsSeenSetsSeenOn() = runBlocking {
        val feed = createFeed()
        feed.fetch()

        val response = feed.markBulkAsSeen(listOf("n-unread-1"))
        Assert.assertEquals(ResponseStatus.SUCCESS, response.status)
        Assert.assertNotNull(feed.data.notifications.first { it.n_id == "n-unread-1" }.seen_on)
    }

    @Test
    fun resetBadgeCountZerosBadge() = runBlocking {
        val feed = createFeed()
        feed.fetchCount()
        Assert.assertEquals("3", feed.data.meta["badge"])

        val response = feed.resetBadgeCount()
        Assert.assertEquals(ResponseStatus.SUCCESS, response.status)
        Assert.assertEquals("0", feed.data.meta["badge"])
    }

    @Test
    fun markAllAsReadSetsReadOnAndZerosBadge() = runBlocking {
        val feed = createFeed()
        feed.fetch()

        val response = feed.markAllAsRead()
        Assert.assertEquals(ResponseStatus.SUCCESS, response.status)
        Assert.assertTrue(feed.data.notifications.all { it.read_on != null })
    }

    @Test
    fun fetchDetailsReturnsNotification() = runBlocking {
        val feed = createFeed()
        val response = feed.fetchDetails("n-unread-1")

        Assert.assertEquals(ResponseStatus.SUCCESS, response.status)
        Assert.assertEquals("n-unread-1", response.body?.n_id)
        Assert.assertEquals("Unread header", response.body?.message?.header)
    }

    @Test
    fun tenantIdFromOptionsIsSentOnFetchCount() = runBlocking {
        val feed = createFeed(tenantId = "T1")
        val response = feed.fetchCount()
        Assert.assertEquals(ResponseStatus.SUCCESS, response.status)

        verify {
            networkClient.httpCall(
                url = match { url ->
                    url.contains("notifications_count") && url.contains("tenant_id=T1")
                },
                authorization = any(),
                date = any(),
                requestJson = any(),
                requestMethod = any(),
                headers = any()
            )
        }
    }

    @Test
    fun resetRemovesAllFeedInstances() {
        createFeed()
        createFeed()
        Assert.assertEquals(2, SuprSend.getInstance().feeds.feedInstances.size)

        SuprSend.getInstance().reset(true)
        Assert.assertEquals(0, SuprSend.getInstance().feeds.feedInstances.size)
    }

    @Test
    fun parsesStoresFromStoreIdKey() {
        val stores = storesFromAsset()
        Assert.assertEquals(4, stores.size)
        Assert.assertEquals("Read", stores[0].storeId)
        Assert.assertEquals(true, stores[0].query?.read)
        Assert.assertEquals("All", stores[3].storeId)
        Assert.assertNull(stores[3].query)
    }
}
