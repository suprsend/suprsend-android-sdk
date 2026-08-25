package app.suprsend.inbox

import app.suprsend.base.AssetHelper
import app.suprsend.base.BaseTest
import app.suprsend.base.assertIsSuccess
import org.json.JSONArray
import org.junit.Assert
import org.junit.Test

class SuprsendInboxTest : BaseTest() {

    val distinctId = "n@s.c"
    val notificationId = "01JKSW0PYTRMKX0H392YYMFBK4"

    @Test
    fun testBellCount() {
        val inbox = InboxTestHelper.identifiedInbox(context, distinctId)
        val response = inbox.fetchBellCount()
        Assert.assertEquals(true, response.isSuccess())
    }

    @Test
    fun testResetBellCount() {
        val inbox = InboxTestHelper.identifiedInbox(context, distinctId)
        inbox.resetBellCount().assertIsSuccess()
    }

    @Test
    fun testNotifications() {
        val inbox = InboxTestHelper.identifiedInbox(context, distinctId) {
            SuprsendInbox.setInboxStores(listOf())
        }
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
        val inboxStoreJson = AssetHelper.readAssetFileToString("inbox/stores.json")
        val inboxStoreList = InboxStore.from(JSONArray(inboxStoreJson))
        val inbox = InboxTestHelper.identifiedInbox(context, distinctId) {
            SSInboxInternal.setInboxStores(inboxStoreList)
        }
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
        val inbox = InboxTestHelper.identifiedInbox(context, distinctId) {
            SuprsendInbox.setInboxStores(listOf())
        }
        val response = inbox.getNotificationDetails(notificationId)
        Assert.assertEquals(true, response.isSuccess())
    }

    @Test
    fun testMarkAllRead() {
        val inbox = InboxTestHelper.identifiedInbox(context, distinctId) {
            SuprsendInbox.setInboxStores(listOf())
        }
        val response = inbox.markAllRead()
        Assert.assertEquals(true, response.isSuccess())
    }

    @Test
    fun testMarkAsInteracted() {
        val inbox = InboxTestHelper.identifiedInbox(context, distinctId) {
            SuprsendInbox.setInboxStores(listOf())
        }
        val response = inbox.markAsInteracted(notificationId)
        Assert.assertEquals(true, response.isSuccess())
    }

    @Test
    fun testMarkAsUnread() {
        val inbox = InboxTestHelper.identifiedInbox(context, distinctId) {
            SuprsendInbox.setInboxStores(listOf())
        }
        val response = inbox.markAsUnread(notificationId)
        Assert.assertEquals(true, response.isSuccess())
    }

    @Test
    fun testMarkAsRead() {
        val inbox = InboxTestHelper.identifiedInbox(context, distinctId) {
            SuprsendInbox.setInboxStores(listOf())
        }
        val response = inbox.markAsRead(notificationId)
        Assert.assertEquals(true, response.isSuccess())
    }

    @Test
    fun testMarkAsArchived() {
        val inbox = InboxTestHelper.identifiedInbox(context, distinctId) {
            SuprsendInbox.setInboxStores(listOf())
        }
        val response = inbox.markAsArchived(notificationId)
        Assert.assertEquals(true, response.isSuccess())
    }

    @Test
    fun testMarkAsSeen() {
        val inbox = InboxTestHelper.identifiedInbox(context, distinctId)
        inbox.markAsSeen("01JK65EKD6VPQAAA26W31X3WGV").assertIsSuccess()
    }

    @Test
    fun testMarkNotificationBulkSeen() {
        val inbox = InboxTestHelper.identifiedInbox(context, distinctId)
        inbox.markAsSeen(listOf("01JK65EKD6VPQAAA26W31X3WGV", notificationId)).assertIsSuccess()
    }
}
