package app.suprsend.user.preference

import app.suprsend.SuprSend
import app.suprsend.base.AssetHelper
import app.suprsend.base.BaseTest
import app.suprsend.base.NetworkClient
import app.suprsend.base.TestConstants
import app.suprsend.base.assertIsSuccess
import app.suprsend.model.ApiResponse
import app.suprsend.model.ResponseStatus
import app.suprsend.model.SuprSendOptions
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

class PreferencesDefaultConfigTest : BaseTest() {

    private val networkClient: NetworkClient = mockk(relaxed = true)

    @Test
    fun verifyIfConfigIsNotPresent() {
        UserPreferenceRemote.networkClient = networkClient
        every {
            networkClient.httpCall(
                url = "https://collector-staging.suprsend.workers.dev/v2/subscriber/U1/full_preference?&tenant_id=T1&show_opt_out_channels=false",
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
            options = SuprSendOptions(
                "https://collector-staging.suprsend.workers.dev"
            )
        )
        val suprSend = SuprSend.getInstance()
        suprSend.reset(true)
        val action = suprSend.identify("U1")
        action.assertIsSuccess()
        val preferences = SuprSend.getInstance().user.getPreferences()
        //Verifying for tenantId T1 and showOptOutChannels false
        preferences.setPreferenceConfig(
            tenantId = "T1",
            showOptOutChannels = false
        )
        val data = preferences.fetchUserPreference().getData()

        Assert.assertEquals(5, data?.sections?.size)
        Assert.assertEquals(5, data?.channelPreferences?.size)

        Assert.assertEquals("refund-promotion", data?.sections?.get(0)?.subCategories?.get(0)?.category)
        Assert.assertEquals(PreferenceOptions.OPT_OUT, data?.sections?.get(0)?.subCategories?.get(0)?.preferenceOptions)
        Assert.assertEquals(true, data?.sections?.get(0)?.subCategories?.get(0)?.channels?.all { it.preferenceOptions == PreferenceOptions.OPT_OUT })

    }
}