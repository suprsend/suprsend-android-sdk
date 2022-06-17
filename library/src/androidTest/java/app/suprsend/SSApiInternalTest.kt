package app.suprsend

import app.suprsend.base.BaseTest
import app.suprsend.base.SSConstants
import app.suprsend.base.size
import app.suprsend.base.toKotlinJsonObject
import app.suprsend.event.EventLocalDatasource
import app.suprsend.sprop.SuperPropertiesLocalDataSource
import app.suprsend.user.UserLocalDatasource
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class SSApiInternalTest : BaseTest() {

    @Test
    fun testIdentityWithoutPushRegistered() {
        SSApiInternal.identify("U1")
        Assert.assertEquals("U1", UserLocalDatasource().getIdentity())
        val eventsList = EventLocalDatasource().getEvents(10)
        Assert.assertEquals(2, eventsList.size)
        Assert.assertEquals(SSConstants.IDENTIFY, eventsList[0].value.toKotlinJsonObject().getString(SSConstants.EVENT))
        Assert.assertEquals(SSConstants.S_EVENT_USER_LOGIN, eventsList[1].value.toKotlinJsonObject().getString(SSConstants.EVENT))
    }

    @Test
    fun testIgnoreIdentifyIfAlreadyIdentified() {
        //Initially identify got called
        SSApiInternal.identify("U1")
        Assert.assertEquals("U1", UserLocalDatasource().getIdentity())
        val eventLocalDatasource = EventLocalDatasource()

        //Cleaning just to fake like event got flush
        deleteAllEvents()

        //Again identify is called with same user id
        SSApiInternal.identify("U1")
        Assert.assertEquals("U1", UserLocalDatasource().getIdentity())
        val eventsList = eventLocalDatasource.getEvents(10)
        //Verify no events are generated as identify is ignored
        Assert.assertEquals(0, eventsList.size)
    }

    @Test
    fun testIdentityWithFCMPush() {
        SSApiInternal.setDeviceId("DEV1")
        SSApiInternal.setXiaomiToken("XIAOMI_TOKEN")
        SSApiInternal.identify("U1")

        Assert.assertEquals("U1", UserLocalDatasource().getIdentity())

        val eventsList = EventLocalDatasource().getEvents(10)

        Assert.assertEquals(3, eventsList.size)
        Assert.assertEquals(SSConstants.IDENTIFY, eventsList[0].value.toKotlinJsonObject().getString(SSConstants.EVENT))

        val fcmPayload = eventsList[1].value.toKotlinJsonObject()
        Assert.assertTrue(fcmPayload.has(SSConstants.APPEND))
        Assert.assertEquals("XIAOMI_TOKEN", fcmPayload.getJSONObject(SSConstants.APPEND).getString(SSConstants.PUSH_ANDROID_TOKEN))
        Assert.assertEquals("DEV1", fcmPayload.getJSONObject(SSConstants.APPEND).getString(SSConstants.DEVICE_ID))
        Assert.assertEquals(SSConstants.PUSH_VENDOR_XIAOMI, fcmPayload.getJSONObject(SSConstants.APPEND).getString(SSConstants.PUSH_VENDOR))
        Assert.assertEquals(SSConstants.S_EVENT_USER_LOGIN, eventsList[2].value.toKotlinJsonObject().getString(SSConstants.EVENT))
    }

    @Test
    fun testIdentityWithXiaomiPush() {


        SSApiInternal.setDeviceId("DEV1")
        SSApiInternal.setFcmToken("FCM_TOKEN")
        SSApiInternal.identify("U1")

        Assert.assertEquals("U1", UserLocalDatasource().getIdentity())

        val eventsList = EventLocalDatasource().getEvents(10)

        Assert.assertEquals(3, eventsList.size)
        Assert.assertEquals(SSConstants.IDENTIFY, eventsList[0].value.toKotlinJsonObject().getString(SSConstants.EVENT))

        val fcmPayload = eventsList[1].value.toKotlinJsonObject()
        Assert.assertTrue(fcmPayload.has(SSConstants.APPEND))
        Assert.assertEquals("FCM_TOKEN", fcmPayload.getJSONObject(SSConstants.APPEND).getString(SSConstants.PUSH_ANDROID_TOKEN))
        Assert.assertEquals("DEV1", fcmPayload.getJSONObject(SSConstants.APPEND).getString(SSConstants.DEVICE_ID))
        Assert.assertEquals(SSConstants.PUSH_VENDOR_FCM, fcmPayload.getJSONObject(SSConstants.APPEND).getString(SSConstants.PUSH_VENDOR))
        Assert.assertEquals(SSConstants.S_EVENT_USER_LOGIN, eventsList[2].value.toKotlinJsonObject().getString(SSConstants.EVENT))
    }

    @Test
    fun testSetSuperPropertyKeyValue() {


        SSApiInternal.setSuperProperty("Product Name", "Cycle 123")
        SSApiInternal.track("ABC")

        val eventsList = EventLocalDatasource().getEvents(10)
        val eventPayload = eventsList[0].value.toKotlinJsonObject()
        Assert.assertEquals("ABC", eventPayload.getString(SSConstants.EVENT))
        Assert.assertEquals("Cycle 123", eventPayload.getJSONObject(SSConstants.PROPERTIES).get("Product Name"))
    }

    @Test
    fun testSetSuperPropertiesJson() {
        SSApiInternal.setSuperProperties(JSONObject().apply {
            put("Product Name 1", "Cycle 1")
            put("Product Name 2", "Cycle 2")
        })

        SSApiInternal.track("ABC")

        val eventsList = EventLocalDatasource().getEvents(10)
        val eventPayload = eventsList[0].value.toKotlinJsonObject()
        Assert.assertEquals("ABC", eventPayload.getString(SSConstants.EVENT))
        Assert.assertEquals("Cycle 1", eventPayload.getJSONObject(SSConstants.PROPERTIES).get("Product Name 1"))
        Assert.assertEquals("Cycle 2", eventPayload.getJSONObject(SSConstants.PROPERTIES).get("Product Name 2"))
    }


    @Test
    fun testRemoveSuperProperty() {


        SSApiInternal.setSuperProperties(JSONObject().apply {
            put("Product Name 1", "Cycle 1")
            put("Product Name 2", "Cycle 2")
        })

        SSApiInternal.removeSuperProperty("Product Name 1")

        SSApiInternal.track("ABC")

        val eventsList = EventLocalDatasource().getEvents(10)
        val eventPayload = eventsList[0].value.toKotlinJsonObject()
        Assert.assertEquals("ABC", eventPayload.getString(SSConstants.EVENT))
        Assert.assertEquals(false, eventPayload.getJSONObject(SSConstants.PROPERTIES).has("Product Name 1"))
        Assert.assertEquals(true, eventPayload.getJSONObject(SSConstants.PROPERTIES).has("Product Name 2"))

    }

    @Test
    fun testTrackEvent() {
        SSApiInternal.track("ABC")

        val eventsList = EventLocalDatasource().getEvents(10)
        val eventPayload = eventsList[0].value.toKotlinJsonObject()
        Assert.assertEquals("ABC", eventPayload.getString(SSConstants.EVENT))
    }

    @Test
    fun testTrackEventWithDollar() {
        SSApiInternal.track("\$some_event_name")

        val eventsList = EventLocalDatasource().getEvents(10)
        Assert.assertEquals(0, eventsList.size)
    }

    @Test
    fun testTrackEventWithPrefixSS() {
        SSApiInternal.track("ss_some_event_name")

        val eventsList = EventLocalDatasource().getEvents(10)
        Assert.assertEquals(0, eventsList.size)
    }

    @Test
    fun testTrackEventWithPrefixSSInBetweenText() {
        SSApiInternal.track("business_event")

        val eventsList = EventLocalDatasource().getEvents(10)
        Assert.assertEquals(1, eventsList.size)
    }

    @Test
    fun testTrackEventWithReservedKeys() {
        SSApiInternal.track("ABC", properties = JSONObject().apply {
            put("Product ID", 1)
            put("Product Name", "Abc Title")
            put("Product Quantity", 10)
            put("Product Price", 43.1)
            put("\$abc", 43.1)
            put("\$Product Price", 43.1)
        })

        val eventsList = EventLocalDatasource().getEvents(10)

        val eventPayload = eventsList[0].value.toKotlinJsonObject()

        val propertiesPayload = eventPayload.getJSONObject(SSConstants.PROPERTIES)
        Assert.assertEquals("ABC", eventPayload.getString(SSConstants.EVENT))
        Assert.assertEquals(4, propertiesPayload.size() - 11) // Removed device properties keys
        Assert.assertEquals(1, propertiesPayload.getInt("Product ID"))
        Assert.assertEquals("Abc Title", propertiesPayload.getString("Product Name"))
        Assert.assertEquals(10, propertiesPayload.getInt("Product Quantity"))
        Assert.assertEquals(43.1, propertiesPayload.getDouble("Product Price"), 0.0)
    }

    @Test
    fun testTrackEventWithProperties() {
        SSApiInternal.track("ABC", properties = JSONObject().apply {
            put("Product ID", 1)
            put("Product Name", "Abc Title")
            put("Product Quantity", 10)
            put("Product Price", 43.1)
        })

        val eventsList = EventLocalDatasource().getEvents(10)
        val eventPayload = eventsList[0].value.toKotlinJsonObject()
        Assert.assertEquals("ABC", eventPayload.getString(SSConstants.EVENT))

        val propertiesPayload = eventPayload.getJSONObject(SSConstants.PROPERTIES)

        Assert.assertEquals(1, propertiesPayload.get("Product ID"))
        Assert.assertEquals("Abc Title", propertiesPayload.get("Product Name"))
        Assert.assertEquals(10, propertiesPayload.get("Product Quantity"))
        Assert.assertEquals(43.1, propertiesPayload.get("Product Price"))
    }

    @Test
    fun testPurchaseMadeProperties() {
        SSApiInternal.purchaseMade(properties = JSONObject().apply {
            put("Product ID", 1)
            put("Product Name", "Abc Title")
            put("Product Quantity", 10)
            put("Product Price", 43.1)
        })

        val eventsList = EventLocalDatasource().getEvents(10)
        val eventPayload = eventsList[0].value.toKotlinJsonObject()

        Assert.assertEquals(1, eventsList.size)
        Assert.assertEquals(SSConstants.S_EVENT_PURCHASE_MADE, eventPayload.getString(SSConstants.EVENT))

        val propertiesPayload = eventPayload.getJSONObject(SSConstants.PROPERTIES)

        Assert.assertEquals(1, propertiesPayload.get("Product ID"))
        Assert.assertEquals("Abc Title", propertiesPayload.get("Product Name"))
        Assert.assertEquals(10, propertiesPayload.get("Product Quantity"))
        Assert.assertEquals(43.1, propertiesPayload.get("Product Price"))
    }

    @Test
    fun testReset() {
        SSApiInternal.identify("U1")
        Assert.assertEquals("U1", UserLocalDatasource().getIdentity())

        SSApiInternal.reset()
        Assert.assertTrue("U1" != UserLocalDatasource().getIdentity())

        val eventsList = EventLocalDatasource().getEvents(10)
        Assert.assertEquals(3, eventsList.size)
        Assert.assertEquals(SSConstants.S_EVENT_USER_LOGOUT, eventsList[2].value.toKotlinJsonObject().getString(SSConstants.EVENT))
    }
}