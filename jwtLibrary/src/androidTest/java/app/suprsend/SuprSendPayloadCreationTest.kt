package app.suprsend

import app.suprsend.base.BaseTest
import app.suprsend.base.SSConstants
import app.suprsend.base.TestConstants
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class SuprSendPayloadCreationTest : BaseTest() {

    @Test
    fun verifyTrackEventPayload() {
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            
           baseUrl =  "https://collector-staging.suprsend.workers.dev"
        )
        val payloadJO = SSInternal.buildTrackEventPayload(
            distinctId = "D1",
            eventName = "product_viewed",
            properties = JSONObject().apply {
                put("key1", "value1")
                put("key2", "value2")
            }
        )
        Assert.assertEquals(true, payloadJO.has(SSConstants.EVENT))
        Assert.assertEquals(true, payloadJO.has(SSConstants.DISTINCT_ID))
        Assert.assertEquals(true, payloadJO.has(SSConstants.INSERT_ID))
        Assert.assertEquals(true, payloadJO.has(SSConstants.TIME))
        Assert.assertEquals(true, payloadJO.has(SSConstants.PROPERTIES))
        Assert.assertEquals(5, payloadJO.length())
    }

    @Test
    fun verifyBuildOperatorPayloadJO() {
        val payloadJO = SSInternal.buildOperatorPayload(
            distinctId = "d1",
            operator = SSConstants.ADD,
            properties = JSONObject().apply {
                put("key1", "value1")
                put("key2", "value2")
            }
        )
        Assert.assertEquals(true, payloadJO.has(SSConstants.DISTINCT_ID))
        Assert.assertEquals(true, payloadJO.has(SSConstants.INSERT_ID))
        Assert.assertEquals(true, payloadJO.has(SSConstants.TIME))
        Assert.assertEquals(true, payloadJO.has(SSConstants.ADD))
        Assert.assertEquals(4, payloadJO.length())
    }

    /**
     * For unset operator we send JA
     */
    @Test
    fun verifyBuildOperatorPayloadJA() {
        val payloadJO = SSInternal.buildOperatorPayload(
            distinctId = "d1",
            operator = SSConstants.REMOVE,
            propertiesJA = JSONArray().apply {
                put("a")
                put("b")
            }
        )
        Assert.assertEquals(true, payloadJO.has(SSConstants.DISTINCT_ID))
        Assert.assertEquals(true, payloadJO.has(SSConstants.INSERT_ID))
        Assert.assertEquals(true, payloadJO.has(SSConstants.TIME))
        Assert.assertEquals(true, payloadJO.has(SSConstants.REMOVE))
        Assert.assertEquals(4, payloadJO.length())
    }
}