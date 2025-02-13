package app.suprsend.inbox

import app.suprsend.SuprSend
import app.suprsend.base.AssetHelper
import app.suprsend.base.BaseTest
import app.suprsend.base.TestConstants
import app.suprsend.base.UserTokenFetcherImpl
import app.suprsend.base.assertIsSuccess
import org.json.JSONArray
import org.junit.Assert
import org.junit.Test

class SuprsendInboxTest : BaseTest() {

    val distinctId = "n@s.c"
    val notificationId = "01JKSW0PYTRMKX0H392YYMFBK4"

    @Test
    fun testBellCount() {
        val userTokenFetcher = UserTokenFetcherImpl()
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            
            baseUrl = TestConstants.SS_BASE_URL,
        )
        SuprSend.setUserTokenFetcher(userTokenFetcher)
        SuprsendInbox.setBaseUrl(baseUrl = TestConstants.SS_INBOX_BASE_URL)
        SuprsendInbox.setSubscriberId(TestConstants.SUBSCRIBER_ID)

        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        val action = suprsend.identify(distinctId)
        action.assertIsSuccess()

        val inbox = SuprsendInbox.getInstance()
        val response = inbox.fetchBellCount()
        Assert.assertEquals(true, response.isSuccess())
    }

    @Test
    fun testResetBellCount() {
        val userTokenFetcher = UserTokenFetcherImpl()
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            
            baseUrl = TestConstants.SS_BASE_URL,
        )
        SuprSend.setUserTokenFetcher(userTokenFetcher)
        SuprsendInbox.setBaseUrl(baseUrl = TestConstants.SS_INBOX_BASE_URL)
        SuprsendInbox.setSubscriberId(TestConstants.SUBSCRIBER_ID)
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        val action = suprsend.identify(distinctId)
        action.assertIsSuccess()

        val inbox = SuprsendInbox.getInstance()
        val response = inbox.resetBellCount()
        response.assertIsSuccess()
    }

    @Test
    fun testNotifications() {
        val userTokenFetcher = UserTokenFetcherImpl()
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            
            baseUrl = TestConstants.SS_BASE_URL,
        )
        SuprSend.setUserTokenFetcher(userTokenFetcher)
        SuprsendInbox.setBaseUrl(baseUrl = TestConstants.SS_INBOX_BASE_URL)
        SuprsendInbox.setSubscriberId(TestConstants.SUBSCRIBER_ID)
        SuprsendInbox.setInboxStores(listOf())
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        val action = suprsend.identify(distinctId)
        action.assertIsSuccess()

        val inbox = SuprsendInbox.getInstance()
        val store = inbox.getStore()
        Assert.assertNotNull(store)
        store!!

        val response = store.load()
        response.assertIsSuccess()
        val messages = store.inboxMessagesList
        Assert.assertNotNull(messages)
    }

    @Test
    fun testNotificationsWithStore() {
        val userTokenFetcher = UserTokenFetcherImpl()
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            
            baseUrl = TestConstants.SS_BASE_URL,
        )
        SuprSend.setUserTokenFetcher(userTokenFetcher)
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        val action = suprsend.identify(distinctId)
        action.assertIsSuccess()

        val inbox = SuprsendInbox.getInstance()
        val inboxStoreJson = AssetHelper.readAssetFileToString("inbox/stores.json")
        val inboxStoreList = InboxStore.from(JSONArray(inboxStoreJson))
        SuprsendInbox.setBaseUrl(baseUrl = TestConstants.SS_INBOX_BASE_URL)
        SuprsendInbox.setSubscriberId(TestConstants.SUBSCRIBER_ID)
        SSInboxInternal.setInboxStores(inboxStoreList)
        val store = inbox.getStore(storeId = "All")
        Assert.assertNotNull(store)
        store!!
        val response = store.load()
        response.assertIsSuccess()
        val messages = store.inboxMessagesList
        Assert.assertNotNull(messages)
    }

    @Test
    fun testNotificationDetails() {
        val userTokenFetcher = UserTokenFetcherImpl()
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            
            baseUrl = TestConstants.SS_BASE_URL,
        )
        SuprSend.setUserTokenFetcher(userTokenFetcher)
        SuprsendInbox.setBaseUrl(baseUrl = TestConstants.SS_INBOX_BASE_URL)
        SuprsendInbox.setSubscriberId(TestConstants.SUBSCRIBER_ID)
        SuprsendInbox.setInboxStores(listOf())
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        val action = suprsend.identify(distinctId)
        action.assertIsSuccess()

        val inbox = SuprsendInbox.getInstance()
        val response = inbox.getNotificationDetails(notificationId)
        Assert.assertEquals(true, response.isSuccess())
    }

    @Test
    fun testMarkAllRead() {
        val userTokenFetcher = UserTokenFetcherImpl()
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            
            baseUrl = TestConstants.SS_BASE_URL,
        )
        SuprSend.setUserTokenFetcher(userTokenFetcher)
        SuprsendInbox.setBaseUrl(baseUrl = TestConstants.SS_INBOX_BASE_URL)
        SuprsendInbox.setSubscriberId(TestConstants.SUBSCRIBER_ID)
        SuprsendInbox.setInboxStores(listOf())
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        val action = suprsend.identify(distinctId)
        action.assertIsSuccess()

        val inbox = SuprsendInbox.getInstance()
        val response = inbox.markAllRead()
        Assert.assertEquals(true, response.isSuccess())
    }

    @Test
    fun testMarkAsInteracted() {
        val userTokenFetcher = UserTokenFetcherImpl()
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            
            baseUrl = TestConstants.SS_BASE_URL,
        )
        SuprSend.setUserTokenFetcher(userTokenFetcher)
        SuprsendInbox.setBaseUrl(baseUrl = TestConstants.SS_INBOX_BASE_URL)
        SuprsendInbox.setSubscriberId(TestConstants.SUBSCRIBER_ID)
        SuprsendInbox.setInboxStores(listOf())
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        val action = suprsend.identify(distinctId)
        action.assertIsSuccess()

        val inbox = SuprsendInbox.getInstance()
        val response = inbox.markAsInteracted(notificationId)
        Assert.assertEquals(true, response.isSuccess())
    }

    @Test
    fun testMarkAsUnread() {
        val userTokenFetcher = UserTokenFetcherImpl()
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            
            baseUrl = TestConstants.SS_BASE_URL,
        )
        SuprSend.setUserTokenFetcher(userTokenFetcher)
        SuprsendInbox.setBaseUrl(baseUrl = TestConstants.SS_INBOX_BASE_URL)
        SuprsendInbox.setSubscriberId(TestConstants.SUBSCRIBER_ID)
        SuprsendInbox.setInboxStores(listOf())
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        val action = suprsend.identify(distinctId)
        action.assertIsSuccess()

        val inbox = SuprsendInbox.getInstance()
        val response = inbox.markAsUnread(notificationId)
        Assert.assertEquals(true, response.isSuccess())
    }

    @Test
    fun testMarkAsRead() {
        val userTokenFetcher = UserTokenFetcherImpl()
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            
            baseUrl = TestConstants.SS_BASE_URL,
        )
        SuprSend.setUserTokenFetcher(userTokenFetcher)
        SuprsendInbox.setBaseUrl(baseUrl = TestConstants.SS_INBOX_BASE_URL)
        SuprsendInbox.setSubscriberId(TestConstants.SUBSCRIBER_ID)
        SuprsendInbox.setInboxStores(listOf())
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        val action = suprsend.identify(distinctId)
        action.assertIsSuccess()

        val inbox = SuprsendInbox.getInstance()
        val response = inbox.markAsRead(notificationId)
        Assert.assertEquals(true, response.isSuccess())
    }

    @Test
    fun testMarkAsArchived() {
        val userTokenFetcher = UserTokenFetcherImpl()
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            
            baseUrl = TestConstants.SS_BASE_URL,
        )
        SuprSend.setUserTokenFetcher(userTokenFetcher)
        SuprsendInbox.setBaseUrl(baseUrl = TestConstants.SS_INBOX_BASE_URL)
        SuprsendInbox.setSubscriberId(TestConstants.SUBSCRIBER_ID)
        SuprsendInbox.setInboxStores(listOf())
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        val action = suprsend.identify(distinctId)
        action.assertIsSuccess()

        val inbox = SuprsendInbox.getInstance()
        val response = inbox.markAsArchived(notificationId)
        Assert.assertEquals(true, response.isSuccess())
    }

    @Test
    fun testMarkAsSeen() {
        val userTokenFetcher = UserTokenFetcherImpl()
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            
            baseUrl = TestConstants.SS_BASE_URL,
        )
        SuprSend.setUserTokenFetcher(userTokenFetcher)
        SuprsendInbox.setBaseUrl(baseUrl = TestConstants.SS_INBOX_BASE_URL)
        SuprsendInbox.setSubscriberId(TestConstants.SUBSCRIBER_ID)
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        val action = suprsend.identify(distinctId)
        action.assertIsSuccess()

        val inbox = SuprsendInbox.getInstance()
        val response = inbox.markAsSeen("01JK65EKD6VPQAAA26W31X3WGV")
        response.assertIsSuccess()
    }

    @Test
    fun testMarkNotificationBulkSeen() {
        val userTokenFetcher = UserTokenFetcherImpl()
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            
            baseUrl = TestConstants.SS_BASE_URL,
        )
        SuprSend.setUserTokenFetcher(userTokenFetcher)
        SuprsendInbox.setBaseUrl(baseUrl = TestConstants.SS_INBOX_BASE_URL)
        SuprsendInbox.setSubscriberId(TestConstants.SUBSCRIBER_ID)
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        val action = suprsend.identify(distinctId)
        action.assertIsSuccess()

        val inbox = SuprsendInbox.getInstance()
        val response = inbox.markAsSeen(listOf("01JK65EKD6VPQAAA26W31X3WGV", notificationId))
        response.assertIsSuccess()
    }

}