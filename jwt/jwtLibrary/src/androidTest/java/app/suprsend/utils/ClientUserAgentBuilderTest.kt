package app.suprsend.utils

import app.suprsend.AppInfo
import app.suprsend.ClientInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Pure tests for [ClientUserAgentBuilder]. These cover the wire format that ends up
 * in the `X-Suprsend-Client-User-Agent` (JSON) and `X-Suprsend-User-Agent` (compact)
 * headers, independent of SuprSend.initialize() side effects.
 *
 * Sample canonical UA string under test:
 *   suprsend-android-sdk/1.0.0 (kotlin/disabled; android) (test/1.0.0)
 */
class ClientUserAgentBuilderTest {

    /**
     * Builds a fully-deterministic [ClientInfo] - every runtime-derived field
     * (sdk_version, os_version, device_model) is locked to a literal value so
     * test assertions can use string literals.
     */
    private fun sampleClientInfo(): ClientInfo = ClientInfo(
        sdkVersion = "1.0.0",
        osVersion = "14",
        deviceModel = "test/device",
        appInfo = AppInfo(name = "test", version = "1.0.0")
    )

    @Test
    fun toJson_returnsAllFields_whenFullyPopulated() {
        val json = ClientUserAgentBuilder.toJson(sampleClientInfo())

        assertEquals("suprsend-android-sdk", json.getString("sdk"))
        assertEquals("1.0.0", json.getString("sdk_version"))
        assertEquals("kotlin", json.getString("lang"))
        assertEquals("disabled", json.getString("lang_version"))
        assertEquals("android", json.getString("platform"))
        assertEquals("mobile", json.getString("environment"))
        assertEquals("android", json.getString("os"))
        assertEquals("14", json.getString("os_version"))
        assertEquals("test/device", json.getString("device_model"))

        val appJson = json.getJSONObject("app_info")
        assertEquals("test", appJson.getString("name"))
        assertEquals("1.0.0", appJson.getString("version"))
    }

    @Test
    fun toJson_omitsAppInfo_whenNull() {
        val info = ClientInfo(
            sdkVersion = "1.0.0",
            osVersion = "14",
            deviceModel = "test/device",
            appInfo = null
        )

        val json = ClientUserAgentBuilder.toJson(info)

        assertFalse(json.has("app_info"))
    }

    @Test
    fun toJson_omitsRuntimeFields_whenBlank() {
        val info = ClientInfo(
            sdkVersion = "",
            osVersion = "",
            deviceModel = "",
            appInfo = AppInfo(name = "test", version = "1.0.0")
        )

        val json = ClientUserAgentBuilder.toJson(info)

        assertFalse(json.has("sdk_version"))
        assertFalse(json.has("os_version"))
        assertFalse(json.has("device_model"))
        assertEquals("suprsend-android-sdk", json.getString("sdk"))
        assertEquals("kotlin", json.getString("lang"))
        assertEquals("disabled", json.getString("lang_version"))
        assertEquals("android", json.getString("os"))
        assertEquals("android", json.getString("platform"))
        assertEquals("mobile", json.getString("environment"))
    }

    @Test
    fun toUserAgentString_returnsCanonicalFormat() {
        val ua = ClientUserAgentBuilder.toUserAgentString(sampleClientInfo())

        assertEquals(
            "suprsend-android-sdk/1.0.0 (kotlin/disabled; android) (test/1.0.0)",
            ua
        )
    }
}
