package app.suprsend.event

import app.suprsend.base.toKotlinJsonObject
import junit.framework.Assert.assertEquals
import org.json.JSONObject
import org.junit.Test

class PayloadCreatorTest {

    @Test
    fun testIdentityEventPayload() {

        val eventName = "\$identify"
        val identifiedId = "\$identified_id"
        val anonId = "\$anon_id"
        val payload =
            """
                {
                  "event": "$eventName",
                  "env": "ENV_API_KEY",
                  "properties": {
                    "$identifiedId": "unique_id",
                    "$anonId": "old_anonymous_id"
                  }
                }
            """.trimIndent().toKotlinJsonObject().toString()

        assertEquals(
            payload,
            PayloadCreator
                .buildIdentityEventPayload(
                    identifiedId = "unique_id",
                    anonymousId = "old_anonymous_id",
                    apiKey = "ENV_API_KEY"
                )
                .keepKeys(arrayListOf("event", "env", "properties")).toString()
        )
    }

    @Test
    fun testTrackEventPayload() {
        val payload =
            """
                {
                  "event": "Product Delete",
                  "distinct_id": "13793",
                  "env": "ENV_API_KEY",
                  "properties": {
                    "App Version": "11.3",
                    "App Build Number": "123",
                    "Brand": "New Model",
                    "Product Title": "Book 121",
                    "Quantity": 30
                  }
                }
            """.trimIndent().toKotlinJsonObject().toString()

        val outputPayload = PayloadCreator
            .buildTrackEventPayload(
                eventName = "Product Delete",
                distinctId = "13793",
                superProperties = JSONObject().apply {
                    put("App Version", "11.3")
                    put("App Build Number", "123")
                    put("Brand", "New Model")
                },
                userProperties = JSONObject().apply {
                    put("Product Title", "Book 121")
                    put("Quantity", 30)
                },
                defaultProperties = JSONObject().apply { },
                apiKey = "ENV_API_KEY"
            )
            .keepKeys(arrayListOf("event", "distinct_id", "env", "properties")).toString()

        assertEquals(payload, outputPayload)
    }

    @Test
    fun testUserOperatorEventPayload() {
        val set = "\$set"

        val payload =
            """
                {
                  "distinct_id": "13793",
                  "env": "ENV_API_KEY",
                  "$set": {
                    "Privileged Customer": true,
                    "Bought Super Coin": 4567.87
                  }
                }
            """.trimIndent().toKotlinJsonObject().toString()

        assertEquals(
            payload,
            PayloadCreator
                .buildUserOperatorPayload(
                    distinctId = "13793",
                    setProperties = JSONObject().apply {
                        put("Privileged Customer", true)
                        put("Bought Super Coin", 4567.87)
                    },
                    operator = "\$set",
                    apiKey = "ENV_API_KEY"
                )
                .keepKeys(arrayListOf("distinct_id", "env", "\$set")).toString()
        )
    }
}

private fun JSONObject.keepKeys(keys: List<String>): JSONObject {
    val mainJo = this
    return JSONObject().apply {
        keys.forEach { key ->
            put(key, mainJo[key])
        }
    }
}
