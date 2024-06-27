package app.suprsend.inbox

import app.suprsend.inbox.model.NotificationStoreConfig
import app.suprsend.inbox.model.NotificationStoreQuery
import org.junit.Assert
import org.junit.Test

class SSInboxInternalTest {
    @Test
    fun validateDefaultValues() {

        val notificationStoreConfig = NotificationStoreConfig(
            storeId = SSInbox.DEFAULT_STORE_ID,
            query = NotificationStoreQuery()
        )


        val ssInbox = SSInbox.initialize(
            subscriberId = "S1",
            distinctId = "D1"
        )

        //Mandatory Supplied Values
        Assert.assertEquals("S1", ssInbox.getData().subscriberId)
        Assert.assertEquals("D1", ssInbox.getData().distinctId)
        Assert.assertEquals(SSInbox.DEFAULT_PAGINATION_LIMIT, ssInbox.getData().pageSize)

        //Default Values
        Assert.assertEquals(SSInbox.DEFAULT_TENANT_ID, ssInbox.getData().tenantId)

        //Store
        val store = ssInbox.getData().notificationStores.first()
        Assert.assertEquals(notificationStoreConfig, store.notificationStoreConfig)
        Assert.assertEquals(true, store.notifications.isEmpty())
        Assert.assertEquals(false, store.isLoading)
        Assert.assertEquals(0, store.currentPageNumber)
        Assert.assertEquals(0, store.totalPages)
        Assert.assertEquals(-1, store.total)
        Assert.assertEquals(-1, store.unseenCount)
        Assert.assertEquals(-1, store.initialFetchTime)

    }

    @Test
    fun validateProvidedValues() {
        val notificationStoreConfig = NotificationStoreConfig(
            "S1", "Abc",
            NotificationStoreQuery(
                tags = listOf("t1"),
                categories = listOf("c1"),
                read = true
            )
        )
        val ssInbox = SSInbox.initialize(
            subscriberId = "S1",
            distinctId = "D1",
            tenantId = "Abc",
            pageSize = 10,
            notificationStoreConfigs = listOf(notificationStoreConfig)
        )
        Assert.assertEquals("S1", ssInbox.getData().subscriberId)
        Assert.assertEquals("D1", ssInbox.getData().distinctId)
        Assert.assertEquals(10, ssInbox.getData().pageSize)

        //Store
        val store = ssInbox.getData().notificationStores.first()
        Assert.assertEquals(notificationStoreConfig, store.notificationStoreConfig)
        Assert.assertEquals(true, store.notifications.isEmpty())
        Assert.assertEquals(false, store.isLoading)
        Assert.assertEquals(0, store.currentPageNumber)
        Assert.assertEquals(0, store.totalPages)
        Assert.assertEquals(-1, store.total)
        Assert.assertEquals(-1, store.unseenCount)
        Assert.assertEquals(-1, store.initialFetchTime)
    }
}