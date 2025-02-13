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
        UserPreferenceRemote.networkClient = networkClient
        SSInternal.networkClient = networkClient

        SuprSend.initialize(
            context = context,
            
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            baseUrl = "https://collector-staging.suprsend.workers.dev"
        )

        val suprSend = SuprSend.getInstance()
        suprSend.reset(true)

        every {
            networkClient.httpCall(
                url = "https://collector-staging.suprsend.workers.dev/v2/event",
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
                url = "https://collector-staging.suprsend.workers.dev/v2/subscriber/U1/full_preference?&show_opt_out_channels=true",
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
        SuprSend.getInstance().user.getPreferences().setPreferenceConfig(
            tenantId = null,
            showOptOutChannels = true
        )

        action.assertIsSuccess()
    }

    @Test
    fun verifyFetchUserPreference() {

        val preferences = SuprSend.getInstance().user.getPreferences()
        val data = preferences.fetchUserPreference().getData()

        Assert.assertEquals(5, data?.sections?.size)
        Assert.assertEquals(5, data?.channelPreferences?.size)

        Assert.assertEquals("refund-promotion", data?.sections?.get(0)?.subCategories?.get(0)?.category)
        Assert.assertEquals(PreferenceOptions.OPT_OUT, data?.sections?.get(0)?.subCategories?.get(0)?.preferenceOptions)
        Assert.assertEquals(true, data?.sections?.get(0)?.subCategories?.get(0)?.channels?.all { it.preferenceOptions == PreferenceOptions.OPT_OUT })

    }

    /**
     * Covers below cases
     *  Category
     *      - OPT_IN and Verify
     *      - OPT_OUT and Verify
     *  Category Channel
     *      - OPT_IN and Verify
     *      - OPT_OUT and Verify
     */
    @Test
    fun verifyUpdateCategoryAndChannelPreference() {
        val preferences = SuprSend.getInstance().user.getPreferences()
        var data = preferences.fetchUserPreference().getData()

        var subCategory = data?.sections?.get(0)?.subCategories?.get(0)
        Assert.assertEquals("refund-promotion", subCategory?.category)
        Assert.assertEquals(PreferenceOptions.OPT_OUT, subCategory?.preferenceOptions)


        //Update Category - OPT_IN and Verify
        every {
            networkClient.httpCall(
                url = "https://collector-staging.suprsend.workers.dev/v2/subscriber/U1/category/refund-promotion?&show_opt_out_channels=true",
                authorization = any(),
                requestMethod = any(),
                requestJson = "{\"preference\":\"opt_in\"}",
                headers = any()
            )
        } returns ApiResponse(
            status = ResponseStatus.SUCCESS,
            statusCode = 200,
            body = AssetHelper.readAssetFileToString("preference/category_update_opt_in.json")
        )
        var response = preferences.updateCategoryPreference(
            category = "refund-promotion",
            preference = PreferenceOptions.OPT_IN
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.fetchUserPreference(fetchRemote = false).getData()
        subCategory = data?.sections?.get(0)?.subCategories?.get(0)
        Assert.assertEquals("refund-promotion", subCategory?.category)
        Assert.assertEquals(PreferenceOptions.OPT_IN, subCategory?.preferenceOptions)


        //Update Channel - whatsapp preference opt_out and Verify
        every {
            networkClient.httpCall(
                url = "https://collector-staging.suprsend.workers.dev/v2/subscriber/U1/category/refund-promotion?&show_opt_out_channels=true",
                authorization = any(),
                requestMethod = any(),
                requestJson = "{\"preference\":\"opt_in\",\"opt_out_channels\":[\"androidpush\",\"email\",\"webpush\",\"whatsapp\"]}",
                headers = any()
            )
        } returns ApiResponse(
            status = ResponseStatus.SUCCESS,
            statusCode = 200,
            body = AssetHelper.readAssetFileToString("preference/category_channel_whatsapp_update_opt_out.json")
        )
        response = preferences.updateChannelPreferenceInCategory(
            category = "refund-promotion",
            preference = PreferenceOptions.OPT_OUT,
            channel = "whatsapp"
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.fetchUserPreference(fetchRemote = false).getData()
        subCategory = data?.sections?.get(0)?.subCategories?.get(0)
        Assert.assertEquals("refund-promotion", subCategory?.category)
        Assert.assertEquals(PreferenceOptions.OPT_IN, subCategory?.preferenceOptions)
        var channel = subCategory?.channels?.last()
        Assert.assertEquals("whatsapp", channel?.channel)
        Assert.assertEquals(PreferenceOptions.OPT_OUT, channel?.preferenceOptions)

        //Update Channel - whatsapp preference opt_in and Verify
        every {
            networkClient.httpCall(
                url = "https://collector-staging.suprsend.workers.dev/v2/subscriber/U1/category/refund-promotion?&show_opt_out_channels=true",
                authorization = any(),
                requestMethod = any(),
                requestJson = "{\"preference\":\"opt_in\",\"opt_out_channels\":[\"androidpush\",\"email\",\"webpush\"]}",
                headers = any()
            )
        } returns ApiResponse(
            status = ResponseStatus.SUCCESS,
            statusCode = 200,
            body = AssetHelper.readAssetFileToString("preference/category_channel_whatsapp_update_opt_in.json")
        )
        response = preferences.updateChannelPreferenceInCategory(
            category = "refund-promotion",
            preference = PreferenceOptions.OPT_IN,
            channel = "whatsapp"
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.fetchUserPreference(fetchRemote = false).getData()
        subCategory = data?.sections?.get(0)?.subCategories?.get(0)
        Assert.assertEquals("refund-promotion", subCategory?.category)
        Assert.assertEquals(PreferenceOptions.OPT_IN, subCategory?.preferenceOptions)
        channel = subCategory?.channels?.last()
        Assert.assertEquals("whatsapp", channel?.channel)
        Assert.assertEquals(PreferenceOptions.OPT_IN, channel?.preferenceOptions)


        //Update - OPT_OUT and Verify
        every {
            networkClient.httpCall(
                url = "https://collector-staging.suprsend.workers.dev/v2/subscriber/U1/category/refund-promotion?&show_opt_out_channels=true",
                authorization = any(),
                requestMethod = any(),
                requestJson = "{\"preference\":\"opt_out\",\"opt_out_channels\":[\"androidpush\",\"email\",\"webpush\"]}",
                headers = any()
            )
        } returns ApiResponse(
            status = ResponseStatus.SUCCESS,
            statusCode = 200,
            body = AssetHelper.readAssetFileToString("preference/category_update_opt_out.json")
        )
        response = preferences.updateCategoryPreference(
            category = "refund-promotion",
            preference = PreferenceOptions.OPT_OUT
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.fetchUserPreference(fetchRemote = false).getData()
        subCategory = data?.sections?.get(0)?.subCategories?.get(0)
        Assert.assertEquals("refund-promotion", subCategory?.category)
        Assert.assertEquals(PreferenceOptions.OPT_OUT, subCategory?.preferenceOptions)

    }

    /**
     * Covers below cases
     * ALL and verify
     * REQUIRED and verify
     */
    @Test
    fun verifyChannelPreferenceRestricted() {

        val preferences = SuprSend.getInstance().user.getPreferences()
        var data = preferences.fetchUserPreference().getData()

        var channel = data?.channelPreferences?.get(0)
        Assert.assertEquals("androidpush", channel?.channel)
        Assert.assertEquals(false, channel?.isRestricted)


        //Update - ALL and Verify
        every {
            networkClient.httpCall(
                url = "https://collector-staging.suprsend.workers.dev/v2/subscriber/U1/channel_preference",
                authorization = any(),
                requestMethod = any(),
                requestJson = "{\"channel_preferences\":[{\"channel\":\"androidpush\",\"is_restricted\":true}]}",
                headers = any()
            )
        } returns ApiResponse(
            status = ResponseStatus.SUCCESS,
            statusCode = 200,
            body = AssetHelper.readAssetFileToString("preference/channel_preference_is_restricted_false.json")
        )
        var response = preferences.updateOverallChannelPreference(
            "androidpush",
            ChannelPreferenceOptions.ALL
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.fetchUserPreference(fetchRemote = false).getData()
        channel = data?.channelPreferences?.get(0)
        Assert.assertEquals("androidpush", channel?.channel)
        Assert.assertEquals(false, channel?.isRestricted)


        //Update - REQUIRED and Verify
        every {
            networkClient.httpCall(
                url = any(),
                authorization = any(),
                requestMethod = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(
            status = ResponseStatus.SUCCESS,
            statusCode = 200,
            body = AssetHelper.readAssetFileToString("preference/channel_preference_is_restricted_true.json")
        )
        response = preferences.updateOverallChannelPreference(
            "androidpush",
            ChannelPreferenceOptions.REQUIRED
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.fetchUserPreference(fetchRemote = false).getData()
        channel = data?.channelPreferences?.get(0)
        Assert.assertEquals("androidpush", channel?.channel)
        Assert.assertEquals(true, channel?.isRestricted)
    }

}