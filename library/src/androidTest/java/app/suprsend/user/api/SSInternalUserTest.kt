package app.suprsend.user.api

import app.suprsend.SSApiInternal
import app.suprsend.base.BaseTest
import app.suprsend.base.SSConstants
import app.suprsend.base.size
import app.suprsend.base.toKotlinJsonObject
import app.suprsend.event.EventLocalDatasource
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SSInternalUserTest : BaseTest() {

    @Test
    fun testSetOperator() {

        val operator = SSConstants.SET

        getTestProductData().forEach { (propertyName, propertyValue) ->

            clean()

            SSInternalUser.set(propertyName, propertyValue)

            val eventsList = EventLocalDatasource().getEvents(10)
            val payload = eventsList.first().value.toKotlinJsonObject()
            val propertiesPayload = payload.getJSONObject(operator)

            assertEquals(1, eventsList.size)
            assertTrue(payload.has(operator))
            assertEquals(1, propertiesPayload.size())
            assertEquals(propertyValue, propertiesPayload.get(propertyName))
        }
    }

    @Test
    fun testSetReservedOperator() {
        SSInternalUser.set("\$abc", "123")
        var eventsList = EventLocalDatasource().getEvents(10)
        assertEquals(0, eventsList.size)

        SSInternalUser.set("ss_abc", "123")
        eventsList = EventLocalDatasource().getEvents(10)
        assertEquals(0, eventsList.size)
    }

    @Test
    fun testSetJsonOperator() {

        val operator = SSConstants.SET
        SSInternalUser.set(getTestProductJsonObject())

        val eventsList = EventLocalDatasource().getEvents(10)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONObject(operator)

        assertEquals(1, eventsList.size)
        assertTrue(payload.has(operator))
        assertEquals(5, propertiesPayload.size())
        verifyProductProperties(propertiesPayload)
    }

    @Test
    fun testSetJsonReservedOperator() {
        SSInternalUser.set(JSONObject().apply {
            put("\$abc", "123")
            put("ss_abc", "123")
        })
        val eventsList = EventLocalDatasource().getEvents(10)
        assertEquals(0, eventsList.size)
    }

    @Test
    fun testSetOnceOperator() {

        val operator = SSConstants.SET_ONCE

        getTestProductData().forEach { (propertyName, propertyValue) ->

            clean()

            SSInternalUser.setOnce(propertyName, propertyValue)

            val eventsList = EventLocalDatasource().getEvents(10)
            val payload = eventsList.first().value.toKotlinJsonObject()
            val propertiesPayload = payload.getJSONObject(operator)

            assertEquals(1, eventsList.size)
            assertTrue(payload.has(operator))
            assertEquals(1, propertiesPayload.size())
            assertEquals(propertyValue, propertiesPayload.get(propertyName))
        }
    }

    @Test
    fun testSetOnceReservedOperator() {
        SSInternalUser.setOnce("\$abc", "123")
        var eventsList = EventLocalDatasource().getEvents(10)
        assertEquals(0, eventsList.size)

        SSInternalUser.setOnce("ss_abc", "123")
        eventsList = EventLocalDatasource().getEvents(10)
        assertEquals(0, eventsList.size)
    }

    @Test
    fun testSetOnceJsonOperator() {

        val operator = SSConstants.SET_ONCE

        SSInternalUser.setOnce(getTestProductJsonObject())

        val eventsList = EventLocalDatasource().getEvents(10)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONObject(operator)

        assertEquals(1, eventsList.size)
        assertTrue(payload.has(operator))
        assertEquals(5, propertiesPayload.size())
        verifyProductProperties(propertiesPayload)
    }

    @Test
    fun testSetOnceJsonReservedOperator() {
        SSInternalUser.setOnce(JSONObject().apply {
            put("\$abc", "123")
            put("ss_abc", "123")
        })
        val eventsList = EventLocalDatasource().getEvents(10)
        assertEquals(0, eventsList.size)
    }

    @Test
    fun testIncrementOperator() {

        val operator = SSConstants.ADD

        val testData: Map<String, Number> = mutableMapOf(
            "Product Price" to 43.1,
            "Product Quantity" to 10,
            "Product Sold" to 9999999999999L
        )

        testData.forEach { (propertyName, propertyValue) ->

            clean()

            SSInternalUser.increment(propertyName, propertyValue)

            val eventsList = EventLocalDatasource().getEvents(10)
            val payload = eventsList.first().value.toKotlinJsonObject()
            val propertiesPayload = payload.getJSONObject(operator)

            assertEquals(1, eventsList.size)
            assertTrue(payload.has(operator))
            assertEquals(1, propertiesPayload.size())
            assertEquals(propertyValue, propertiesPayload.get(propertyName))
        }
    }

    @Test
    fun testIncrementReservedOperator() {
        SSInternalUser.increment("\$abc", 123)
        var eventsList = EventLocalDatasource().getEvents(10)
        assertEquals(0, eventsList.size)

        SSInternalUser.increment("ss_abc", 123)
        eventsList = EventLocalDatasource().getEvents(10)
        assertEquals(0, eventsList.size)
    }

    @Test
    fun testIncrementJsonOperator() {

        val operator = SSConstants.ADD

        val testData: Map<String, Number> = mutableMapOf(
            "Product Price" to 43.1,
            "Product Quantity" to 10,
            "Product Sold" to 9999999999999L
        )

        SSInternalUser.increment(testData)

        val eventsList = EventLocalDatasource().getEvents(10)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONObject(operator)

        assertEquals(1, eventsList.size)
        assertTrue(payload.has(operator))
        assertEquals(3, propertiesPayload.size())
        assertEquals(43.1, propertiesPayload.getDouble("Product Price"), 0.0)
        assertEquals(10, propertiesPayload.getInt("Product Quantity"))
        assertEquals(9999999999999L, propertiesPayload.getLong("Product Sold"))
    }

    @Test
    fun testIncrementJsonReservedOperator() {
        SSInternalUser.increment(
            mapOf(
                "\$abc" to 123,
                "ss_abc" to 123
            )
        )
        val eventsList = EventLocalDatasource().getEvents(10)
        assertEquals(0, eventsList.size)
    }

    @Test
    fun testAppendOperator() {

        val operator = SSConstants.APPEND

        getTestProductData().forEach { (propertyName, propertyValue) ->

            clean()

            SSInternalUser.append(propertyName, propertyValue)

            val eventsList = EventLocalDatasource().getEvents(10)
            val payload = eventsList.first().value.toKotlinJsonObject()
            val propertiesPayload = payload.getJSONObject(operator)

            assertEquals(1, eventsList.size)
            assertTrue(payload.has(operator))
            assertEquals(1, propertiesPayload.size())
            assertEquals(propertyValue, propertiesPayload.get(propertyName))
        }
    }

    @Test
    fun testAppendReservedOperator() {
        SSInternalUser.append("\$abc", "123")
        var eventsList = EventLocalDatasource().getEvents(10)
        assertEquals(0, eventsList.size)

        SSInternalUser.append("ss_abc", "123")
        eventsList = EventLocalDatasource().getEvents(10)
        assertEquals(0, eventsList.size)
    }

    @Test
    fun testAppendJsonOperator() {

        val operator = SSConstants.APPEND

        SSInternalUser.append(getTestProductJsonObject())

        val eventsList = EventLocalDatasource().getEvents(10)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONObject(operator)

        assertEquals(1, eventsList.size)
        assertTrue(payload.has(operator))
        assertEquals(5, propertiesPayload.size())
        verifyProductProperties(propertiesPayload)
    }

    @Test
    fun testAppendJsonReservedOperator() {
        SSInternalUser.append(JSONObject().apply {
            put("\$abc", "123")
            put("ss_abc", "123")
        })
        val eventsList = EventLocalDatasource().getEvents(10)
        assertEquals(0, eventsList.size)
    }

    @Test
    fun testRemoveOperator() {

        val operator = SSConstants.REMOVE

        getTestProductData().forEach { (propertyName, propertyValue) ->

            clean()

            SSInternalUser.remove(propertyName, propertyValue)

            val eventsList = EventLocalDatasource().getEvents(10)
            val payload = eventsList.first().value.toKotlinJsonObject()
            val propertiesPayload = payload.getJSONObject(operator)

            assertEquals(1, eventsList.size)
            assertTrue(payload.has(operator))
            assertEquals(1, propertiesPayload.size())
            assertEquals(propertyValue, propertiesPayload.get(propertyName))
        }
    }

    @Test
    fun testRemoveReservedOperator() {
        SSInternalUser.remove("\$abc", "123")
        var eventsList = EventLocalDatasource().getEvents(10)
        assertEquals(0, eventsList.size)

        SSInternalUser.remove("ss_abc", "123")
        eventsList = EventLocalDatasource().getEvents(10)
        assertEquals(0, eventsList.size)
    }

    @Test
    fun testRemoveJsonOperator() {

        val operator = SSConstants.REMOVE

        SSInternalUser.remove(getTestProductJsonObject())

        val eventsList = EventLocalDatasource().getEvents(10)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONObject(operator)

        assertEquals(1, eventsList.size)
        assertTrue(payload.has(operator))
        assertEquals(5, propertiesPayload.size())
        verifyProductProperties(propertiesPayload)
    }

    @Test
    fun testRemoveJsonReservedOperator() {
        SSInternalUser.remove(JSONObject().apply {
            put("\$abc", "123")
            put("ss_abc", "123")
        })
        val eventsList = EventLocalDatasource().getEvents(10)
        assertEquals(0, eventsList.size)
    }

    @Test
    fun testUnSetOperator() {

        val operator = SSConstants.UNSET

        SSInternalUser.unSet("Product Name")

        val eventsList = EventLocalDatasource().getEvents(10)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONArray(operator)

        assertEquals(1, eventsList.size)
        assertTrue(payload.has(operator))
        assertEquals(1, propertiesPayload.length())
        assertEquals("Product Name", propertiesPayload.get(0))
    }

    @Test
    fun testUnSetListOperator() {

        val operator = SSConstants.UNSET

        SSInternalUser.unSet(listOf("P1", "P2"))

        val eventsList = EventLocalDatasource().getEvents(10)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONArray(operator)

        assertEquals(1, eventsList.size)
        assertTrue(payload.has(operator))
        assertEquals(2, propertiesPayload.length())
        assertEquals("P1", propertiesPayload.get(0))
        assertEquals("P2", propertiesPayload.get(1))
    }

    @Test
    fun testSetEmailOperator() {

        val testValue = "abc@abc.com"
        val operator = SSConstants.APPEND

        SSInternalUser.setEmail(testValue)

        val eventsList = EventLocalDatasource().getEvents(10)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONObject(operator)

        assertEquals(1, eventsList.size)
        assertTrue(payload.has(operator))
        assertEquals(1, propertiesPayload.size())
        assertEquals(testValue, propertiesPayload.get(SSConstants.EMAIL))
    }

    @Test
    fun testUnSetEmailOperator() {

        val testValue = "abc@abc.com"
        val operator = SSConstants.REMOVE

        SSInternalUser.unSetEmail(testValue)

        val eventsList = EventLocalDatasource().getEvents(10)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONObject(operator)

        assertEquals(1, eventsList.size)
        assertTrue(payload.has(operator))
        assertEquals(1, propertiesPayload.size())
        assertEquals(testValue, propertiesPayload.get(SSConstants.EMAIL))
    }

    @Test
    fun testSetSmsOperator() {

        val testValue = "+919999999999"
        val operator = SSConstants.APPEND

        SSInternalUser.setSms(testValue)

        val eventsList = EventLocalDatasource().getEvents(10)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONObject(operator)

        assertEquals(1, eventsList.size)
        assertTrue(payload.has(operator))
        assertEquals(1, propertiesPayload.size())
        assertEquals(testValue, propertiesPayload.get(SSConstants.SMS))
    }

    @Test
    fun testUnSetSmsOperator() {

        val testValue = "+919999999999"
        val operator = SSConstants.REMOVE

        SSInternalUser.unSetSms(testValue)

        val eventsList = EventLocalDatasource().getEvents(10)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONObject(operator)

        assertEquals(1, eventsList.size)
        assertTrue(payload.has(operator))
        assertEquals(1, propertiesPayload.size())
        assertEquals(testValue, propertiesPayload.get(SSConstants.SMS))
    }

    @Test
    fun testSetWhatsAppOperator() {

        val testValue = "+919999999999"
        val operator = SSConstants.APPEND

        SSInternalUser.setWhatsApp(testValue)

        val eventsList = EventLocalDatasource().getEvents(10)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONObject(operator)

        assertEquals(1, eventsList.size)
        assertTrue(payload.has(operator))
        assertEquals(1, propertiesPayload.size())
        assertEquals(testValue, propertiesPayload.get(SSConstants.WHATS_APP))
    }

    @Test
    fun testUnSetWhatsAppOperator() {

        val testValue = "+919999999999"
        val operator = SSConstants.REMOVE

        SSInternalUser.unSetWhatsApp(testValue)

        val eventsList = EventLocalDatasource().getEvents(10)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONObject(operator)

        assertEquals(1, eventsList.size)
        assertTrue(payload.has(operator))
        assertEquals(1, propertiesPayload.size())
        assertEquals(testValue, propertiesPayload.get(SSConstants.WHATS_APP))
    }

    @Test
    fun testSetAndroidFcmPushOperator() {

        SSApiInternal.setDeviceId("D1")
        val testValue = "Token1"
        val operator = SSConstants.APPEND

        SSInternalUser.setAndroidFcmPush(testValue)

        val eventsList = EventLocalDatasource().getEvents(10)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONObject(operator)

        assertEquals(1, eventsList.size)
        assertTrue(payload.has(operator))
        assertEquals(3, propertiesPayload.size())
        assertEquals(testValue, propertiesPayload.get(SSConstants.PUSH_ANDROID_TOKEN))
        assertEquals(SSConstants.PUSH_VENDOR_FCM, propertiesPayload.get(SSConstants.PUSH_VENDOR))
        assertEquals("D1", propertiesPayload.get(SSConstants.DEVICE_ID))
    }

    @Test
    fun testUnsetAndroidFcmPushOperator() {

        SSApiInternal.setDeviceId("D1")
        val testValue = "Token1"
        val operator = SSConstants.REMOVE

        SSInternalUser.unSetAndroidFcmPush(testValue)

        val eventsList = EventLocalDatasource().getEvents(10)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONObject(operator)

        assertEquals(1, eventsList.size)
        assertTrue(payload.has(operator))
        assertEquals(3, propertiesPayload.size())
        assertEquals(testValue, propertiesPayload.get(SSConstants.PUSH_ANDROID_TOKEN))
        assertEquals(SSConstants.PUSH_VENDOR_FCM, propertiesPayload.get(SSConstants.PUSH_VENDOR))
        assertEquals("D1", propertiesPayload.get(SSConstants.DEVICE_ID))
    }

    @Test
    fun testSetAndroidXiaomiPushPushOperator() {

        SSApiInternal.setDeviceId("D1")
        val testValue = "Token1"
        val operator = SSConstants.APPEND

        SSInternalUser.setAndroidXiaomiPush(testValue)

        val eventsList = EventLocalDatasource().getEvents(10)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONObject(operator)

        assertEquals(1, eventsList.size)
        assertTrue(payload.has(operator))
        assertEquals(3, propertiesPayload.size())
        assertEquals(testValue, propertiesPayload.get(SSConstants.PUSH_ANDROID_TOKEN))
        assertEquals(SSConstants.PUSH_VENDOR_XIAOMI, propertiesPayload.get(SSConstants.PUSH_VENDOR))
        assertEquals("D1", propertiesPayload.get(SSConstants.DEVICE_ID))
    }

    @Test
    fun testUnSetAndroidXiaomiPushOperator() {

        SSApiInternal.setDeviceId("D1")
        val testValue = "Token1"
        val operator = SSConstants.REMOVE

        SSInternalUser.unSetAndroidXiaomiPush(testValue)

        val eventsList = EventLocalDatasource().getEvents(10)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONObject(operator)

        assertEquals(1, eventsList.size)
        assertTrue(payload.has(operator))
        assertEquals(3, propertiesPayload.size())
        assertEquals(testValue, propertiesPayload.get(SSConstants.PUSH_ANDROID_TOKEN))
        assertEquals(SSConstants.PUSH_VENDOR_XIAOMI, propertiesPayload.get(SSConstants.PUSH_VENDOR))
        assertEquals("D1", propertiesPayload.get(SSConstants.DEVICE_ID))
    }

    @Test
    fun testCorrectSetPreferredLanguage() {
        val preferredLanguage = "en"
        val operator = SSConstants.SET
        SSInternalUser.setPreferredLanguage(preferredLanguage)
        val eventsList = EventLocalDatasource().getEvents(10)
        assertEquals(1, eventsList.size)
        val payload = eventsList.first().value.toKotlinJsonObject()
        val propertiesPayload = payload.getJSONObject(operator)
        assertEquals(preferredLanguage, propertiesPayload.get(SSConstants.PREFERRED_LANGUAGE))
    }

    @Test
    fun testInCorrectSetPreferredLanguage() {
        SSInternalUser.setPreferredLanguage("abc")
        val eventsList = EventLocalDatasource().getEvents(10)
        assertEquals(0, eventsList.size)
    }

}