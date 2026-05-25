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

class SuprsendInboxTenantIdTest : BaseTest() {

    val distinctId = "n@s.c"

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
            
            baseUrl = TestConstants.SS_BASE_URL
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
            
            baseUrl = TestConstants.SS_BASE_URL
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
            
            baseUrl = TestConstants.SS_BASE_URL
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
            
            baseUrl = TestConstants.SS_BASE_URL
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
    fun testMarkAsSeen() {
        val userTokenFetcher = UserTokenFetcherImpl()
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            
            baseUrl = TestConstants.SS_BASE_URL
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

//    @Test
//    fun testBellCountForTenantId() {
//        val userTokenFetcher = mockk<UserTokenFetcher>(relaxed = true)
//        every { userTokenFetcher.getToken(any()) } returns "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJlbnRpdHlfdHlwZSI6InN1YnNjcmliZXIiLCJlbnRpdHlfaWQiOiJuQHMuYyIsImV4cCI6MTczODY0ODk1NSwiaWF0IjoxNzM4NjQ4Nzc1fQ.wdjzoCfnydjCyEuWbHVv9j1wJmT59XtEdANyAxCqk3l5AyAmdobisg9zfqONVDVHi6DT8haWIA1K03JU9g1o-A"
//        SuprSend.initialize(
//            context = context,
//            publicApiKey = TestConstants.PUBLIC_API_KEY,
//    
//            ),
//        )
//    SuprSend.setUserTokenFetcher(userTokenFetcher)
//        SuprsendInbox.initialize(
//            baseUrl = TestConstants.SS_INBOX_BASE_URL,
//            tenantId = "karthick_qa_new"
//        )
//
//        val action = SuprSend.getInstance().identify(distinctId = "n@s.c")
//        action.assertIsSuccess()
//
//        val inbox = SuprsendInbox.getInstance()
//        val response = inbox.getBellCount()
//        Assert.assertEquals(1, response.getData())
//    }
}