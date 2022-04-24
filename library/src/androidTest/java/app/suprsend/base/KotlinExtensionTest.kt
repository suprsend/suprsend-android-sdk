package app.suprsend.base

import junit.framework.Assert.assertEquals
import org.junit.Test

class KotlinExtensionTest {

    @Test
    fun testBlankString() {
        assertEquals("{}", "".toKotlinJsonObject().toString())
    }

    @Test
    fun testJsonProperties() {
        val jsonObject = """
            {
              "title": "Product Title",
              "quantity": 5,
              "price": 340.45
            }
        """.trimIndent().toKotlinJsonObject()

        assertEquals("Product Title", jsonObject["title"])
        assertEquals(5, jsonObject["quantity"])
        assertEquals(340.45, jsonObject["price"])
    }

    @Test
    fun testPropertiesUpdate() {
        val jsonObject = """
            {
              "title": "Product Title",
              "quantity": 5,
              "price": 340.45
            }
        """.trimIndent().toKotlinJsonObject()

        assertEquals("Product Title", jsonObject["title"])
        assertEquals(5, jsonObject["quantity"])
        assertEquals(340.45, jsonObject["price"])

        val updateJsonObject = """
            {
              "title": "Product Title 1",
              "quantity": 51,
              "price": 341.45,
              "discount": 721.45
            }
        """.trimIndent().toKotlinJsonObject()

        val newJsonObject = jsonObject.addUpdateJsoObject(updateJsonObject)

        assertEquals("Product Title 1", newJsonObject["title"])
        assertEquals(51, newJsonObject["quantity"])
        assertEquals(341.45, newJsonObject["price"])
        assertEquals(721.45, newJsonObject["discount"])
    }


    @Test
    fun testRandomFunctionLength() {
        assertEquals(1, getRandomString(1).length)
        assertEquals(10, getRandomString(10).length)
        assertEquals(100, getRandomString(100).length)
    }

    @Test
    fun testFilterSSReservedKeys() {
        val key = "\$"
        val sampleJson = """
            {
                "someKeyString": "Product Title 1",
                "someKeyInt": 1,
                "someKeyDouble": 1.2,
                "someKeyBoolean": false,
                "jO": { "someKeyString": "Product Title 1" },
                "ja": [{ "someKeyString": "Product Title 1"}],
                "${key}someKey": 51,
                "ss_someKey": 341.45,
                "SS_someKey2": 341.45
            }
        """.trimIndent().toKotlinJsonObject().filterSSReservedKeys()
        assertEquals(6, sampleJson.size())
        assertEquals("Product Title 1", sampleJson["someKeyString"] ?: "")
        assertEquals(1, sampleJson["someKeyInt"])
        assertEquals(1.2, sampleJson["someKeyDouble"])
        assertEquals(false, sampleJson["someKeyBoolean"])
    }
}
