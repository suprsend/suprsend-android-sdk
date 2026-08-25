package app.suprsend.user.preference

import app.suprsend.SSInternal
import app.suprsend.SuprSend
import app.suprsend.base.AssetHelper
import app.suprsend.base.BaseTest
import app.suprsend.base.NetworkClient
import app.suprsend.base.TestConstants
import app.suprsend.base.assertIsSuccess
import app.suprsend.model.ApiResponse
import app.suprsend.model.ResponseStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

class PreferencesDefaultConfigTest : BaseTest() {

    private val networkClient: NetworkClient = mockk(relaxed = true)

    @Test
    fun verifyIfConfigIsNotPresent() {
        Preferences.debounceDelayMs = 60_000L
        UserPreferenceRemote.networkClient = networkClient
        SSInternal.networkClient = networkClient
        every {
            networkClient.httpCall(
                url = "${TestConstants.SS_BASE_URL}/v2/event",
                authorization = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(
            status = ResponseStatus.SUCCESS,
            statusCode = 200,
            body = AssetHelper.readAssetFileToString("event_and_operator_response.json")
        )
        every {
            networkClient.httpCall(
                url = "${TestConstants.SS_BASE_URL}/v1/user/U1/preference/?tenant_id=T1&show_opt_out_channels=false",
                authorization = any(),
                requestMethod = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(
            ResponseStatus.SUCCESS,
            200,
            body = AssetHelper.readAssetFileToString("preference/full_preference_1.json")
        )
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            host = TestConstants.SS_BASE_URL
        )
        val suprSend = SuprSend.getInstance()
        suprSend.reset(true)
        val action = suprSend.identify("U1")
        action.assertIsSuccess()
        val preferences = SuprSend.getInstance().preferences
        val data = preferences.getPreferences(
            args = Preferences.Args(
                tenantId = "T1",
                showOptOutChannels = false
            )
        ).body

        Assert.assertEquals(5, data?.sections?.size)
        Assert.assertEquals(5, data?.channelPreferences?.size)

        Assert.assertEquals("refund-promotion", data?.sections?.get(0)?.subcategories?.get(0)?.category)
        Assert.assertEquals(PreferenceOptions.optOut, data?.sections?.get(0)?.subcategories?.get(0)?.preference)
        Assert.assertEquals(true, data?.sections?.get(0)?.subcategories?.get(0)?.channels?.all { it.preference == PreferenceOptions.optOut })
    }
}
