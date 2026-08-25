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
import org.junit.Before
import org.junit.Test

class PreferencesTest : BaseTest() {

    private val networkClient: NetworkClient = mockk(relaxed = true)

    @Before
    fun setup() {
        Preferences.debounceDelayMs = 60_000L
        UserPreferenceRemote.networkClient = networkClient
        SSInternal.networkClient = networkClient

        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            host = TestConstants.SS_BASE_URL
        )

        val suprSend = SuprSend.getInstance()
        suprSend.reset(true)

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
        val action = suprSend.identify("U1")

        every {
            networkClient.httpCall(
                url = "${TestConstants.SS_BASE_URL}/v1/user/U1/preference/?show_opt_out_channels=true",
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

        action.assertIsSuccess()
    }

    @Test
    fun verifyFetchUserPreference() {
        val preferences = SuprSend.getInstance().preferences
        val data = preferences.getPreferences().body

        Assert.assertEquals(5, data?.sections?.size)
        Assert.assertEquals(5, data?.channelPreferences?.size)

        Assert.assertEquals("refund-promotion", data?.sections?.get(0)?.subcategories?.get(0)?.category)
        Assert.assertEquals(PreferenceOptions.optOut, data?.sections?.get(0)?.subcategories?.get(0)?.preference)
        Assert.assertEquals(true, data?.sections?.get(0)?.subcategories?.get(0)?.channels?.all { it.preference == PreferenceOptions.optOut })
    }

    /**
     * Covers below cases
     *  Category
     *      - optIn and Verify
     *      - optOut and Verify
     *  Category Channel
     *      - optIn and Verify
     *      - optOut and Verify
     */
    @Test
    fun verifyUpdateCategoryAndChannelPreference() {
        val preferences = SuprSend.getInstance().preferences
        var data = preferences.getPreferences().body

        var subCategory = data?.sections?.get(0)?.subcategories?.get(0)
        Assert.assertEquals("refund-promotion", subCategory?.category)
        Assert.assertEquals(PreferenceOptions.optOut, subCategory?.preference)

        var response = preferences.updateCategoryPreference(
            category = "refund-promotion",
            preference = PreferenceOptions.optIn
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.data
        subCategory = data?.sections?.get(0)?.subcategories?.get(0)
        Assert.assertEquals("refund-promotion", subCategory?.category)
        Assert.assertEquals(PreferenceOptions.optIn, subCategory?.preference)

        response = preferences.updateChannelPreferenceInCategory(
            channel = "whatsapp",
            preference = PreferenceOptions.optOut,
            category = "refund-promotion"
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.data
        subCategory = data?.sections?.get(0)?.subcategories?.get(0)
        Assert.assertEquals("refund-promotion", subCategory?.category)
        Assert.assertEquals(PreferenceOptions.optIn, subCategory?.preference)
        var channel = subCategory?.channels?.last()
        Assert.assertEquals("whatsapp", channel?.channel)
        Assert.assertEquals(PreferenceOptions.optOut, channel?.preference)

        response = preferences.updateChannelPreferenceInCategory(
            channel = "whatsapp",
            preference = PreferenceOptions.optIn,
            category = "refund-promotion"
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.data
        subCategory = data?.sections?.get(0)?.subcategories?.get(0)
        Assert.assertEquals("refund-promotion", subCategory?.category)
        Assert.assertEquals(PreferenceOptions.optIn, subCategory?.preference)
        channel = subCategory?.channels?.last()
        Assert.assertEquals("whatsapp", channel?.channel)
        Assert.assertEquals(PreferenceOptions.optIn, channel?.preference)

        response = preferences.updateCategoryPreference(
            category = "refund-promotion",
            preference = PreferenceOptions.optOut
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.data
        subCategory = data?.sections?.get(0)?.subcategories?.get(0)
        Assert.assertEquals("refund-promotion", subCategory?.category)
        Assert.assertEquals(PreferenceOptions.optOut, subCategory?.preference)
    }

    /**
     * Covers below cases
     * all and verify
     * required and verify
     */
    @Test
    fun verifyChannelPreferenceRestricted() {
        val preferences = SuprSend.getInstance().preferences
        var data = preferences.getPreferences().body

        var channel = data?.channelPreferences?.get(0)
        Assert.assertEquals("androidpush", channel?.channel)
        Assert.assertEquals(false, channel?.isRestricted)

        var response = preferences.updateOverallChannelPreference(
            "androidpush",
            ChannelLevelPreferenceOptions.all
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.data
        channel = data?.channelPreferences?.get(0)
        Assert.assertEquals("androidpush", channel?.channel)
        Assert.assertEquals(false, channel?.isRestricted)

        response = preferences.updateOverallChannelPreference(
            "androidpush",
            ChannelLevelPreferenceOptions.required
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.data
        channel = data?.channelPreferences?.get(0)
        Assert.assertEquals("androidpush", channel?.channel)
        Assert.assertEquals(true, channel?.isRestricted)
    }
}
