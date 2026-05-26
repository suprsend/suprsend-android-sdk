package app.suprsend

import app.suprsend.base.BaseTest
import app.suprsend.base.NetworkClient
import app.suprsend.base.TestConstants
import app.suprsend.model.ApiResponse
import app.suprsend.model.ResponseStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that `X-Suprsend-User-Agent` and `X-Suprsend-Client-User-Agent` are
 * - computed and cached at [SuprSend.initialize] time,
 * - returned by [SSInternal.addSSSignature],
 * - actually shipped on outgoing event requests.
 *
 * Canonical sample UA shipped by these tests:
 *   suprsend-android-sdk/1.0.0 (kotlin/disabled; android) (test/1.0.0)
 */
class SuprSendClientUserAgentHeaderTest : BaseTest() {

    private val networkClient = mockk<NetworkClient>()

    /**
     * Fully-deterministic [ClientInfo] so every header value can be asserted as a literal.
     */
    private fun sampleClientInfo(): ClientInfo = ClientInfo(
        sdkVersion = "1.0.0",
        osVersion = "14",
        deviceModel = "test/device",
        appInfo = AppInfo(name = "test", version = "1.0.0")
    )

    @Test
    fun initialize_populatesUserAgent_withCanonicalFormat() {
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            clientInfo = sampleClientInfo(),
            baseUrl = TestConstants.SS_BASE_URL
        )

        assertEquals(
            "suprsend-android-sdk/1.0.0 (kotlin/disabled; android) (test/1.0.0)",
            SSInternal.suprSendData.userAgent
        )
    }

    @Test
    fun initialize_populatesClientUserAgentJson_withCanonicalFields() {
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            clientInfo = sampleClientInfo(),
            baseUrl = TestConstants.SS_BASE_URL
        )

        val json = JSONObject(SSInternal.suprSendData.clientUserAgentJson!!)
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
    fun initialize_omitsAppInfoFromJson_whenAppInfoMissing() {
        val infoWithoutApp = ClientInfo(
            sdkVersion = "1.0.0",
            osVersion = "14",
            deviceModel = "test/device",
            appInfo = null
        )

        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            clientInfo = infoWithoutApp,
            baseUrl = TestConstants.SS_BASE_URL
        )

        val json = JSONObject(SSInternal.suprSendData.clientUserAgentJson!!)
        assertFalse(json.has("app_info"))
    }

    @Test
    fun addSSSignature_emitsBothHeaders_withCanonicalValues() {
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            clientInfo = sampleClientInfo(),
            baseUrl = TestConstants.SS_BASE_URL
        )

        val headers = SSInternal.addSSSignature()
        assertNotNull(headers)

        assertEquals(
            "suprsend-android-sdk/1.0.0 (kotlin/disabled; android) (test/1.0.0)",
            headers!!["X-Suprsend-User-Agent"]
        )

        val json = JSONObject(headers["X-Suprsend-Client-User-Agent"]!!)
        assertEquals("suprsend-android-sdk", json.getString("sdk"))
        assertEquals("1.0.0", json.getString("sdk_version"))
        assertEquals("kotlin", json.getString("lang"))
        assertEquals("disabled", json.getString("lang_version"))
        assertEquals("android", json.getString("os"))
        assertEquals("14", json.getString("os_version"))
        assertEquals("test/device", json.getString("device_model"))
    }

    @Test
    fun addSSSignature_preservesExistingCallerHeaders() {
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            clientInfo = sampleClientInfo(),
            baseUrl = TestConstants.SS_BASE_URL
        )

        val callerHeaders = mutableMapOf("X-Test" to "value")
        val headers = SSInternal.addSSSignature(callerHeaders)

        assertNotNull(headers)
        assertEquals("value", headers!!["X-Test"])
        assertEquals(
            "suprsend-android-sdk/1.0.0 (kotlin/disabled; android) (test/1.0.0)",
            headers["X-Suprsend-User-Agent"]
        )
        assertTrue(headers.containsKey("X-Suprsend-Client-User-Agent"))
    }

    @Test
    fun trackEvent_forwardsBothHeadersToNetworkClient() {
        val headerSlot = slot<Map<String, String>>()
        every {
            networkClient.httpCall(
                url = any(),
                authorization = any(),
                requestJson = any(),
                headers = capture(headerSlot)
            )
        } returns ApiResponse(status = ResponseStatus.SUCCESS, statusCode = 200)

        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            clientInfo = sampleClientInfo(),
            baseUrl = TestConstants.SS_BASE_URL
        )
        SSInternal.networkClient = networkClient
        SuprSend.setUserTokenFetcher(null)

        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)

        // First call drives the $identify event - reset the slot to focus on the next event.
        suprsend.identify("D1")
        headerSlot.clear()
        suprsend.trackEvent("home_viewed")

        assertTrue(headerSlot.isCaptured)
        val captured = headerSlot.captured

        assertEquals(
            "suprsend-android-sdk/1.0.0 (kotlin/disabled; android) (test/1.0.0)",
            captured["X-Suprsend-User-Agent"]
        )

        val json = JSONObject(captured["X-Suprsend-Client-User-Agent"]!!)
        assertEquals("suprsend-android-sdk", json.getString("sdk"))
        assertEquals("1.0.0", json.getString("sdk_version"))
        assertEquals("test/device", json.getString("device_model"))
        assertEquals("test", json.getJSONObject("app_info").getString("name"))
        assertEquals("1.0.0", json.getJSONObject("app_info").getString("version"))
    }
}
