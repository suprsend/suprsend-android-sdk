package app.suprsend.user.preference

import app.suprsend.SuprSend
import app.suprsend.base.AssetHelper
import app.suprsend.base.BaseTest
import app.suprsend.base.NetworkClient
import app.suprsend.base.TestConstants
import app.suprsend.model.ApiResponse
import app.suprsend.model.ResponseStatus
import app.suprsend.model.SuprSendOptions
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

class PreferencesTest : BaseTest() {

    private val networkClient: NetworkClient = mockk(relaxed = true)

    @Test
    fun verifyFetchUserPreference() {
        UserPreferenceRemote.networkClient = networkClient
        every {
            networkClient.httpCall(
                url = any(),
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
        val preferences = SuprSend.getInstance().user.getPreferences()
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

    @Test
    fun verifyUpdateCategoryPreference() {
        UserPreferenceRemote.networkClient = networkClient
        every {
            networkClient.httpCall(
                url = any(),
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
        val preferences = SuprSend.getInstance().user.getPreferences()
        preferences.setPreferenceConfig(
            tenantId = "T1",
            showOptOutChannels = false
        )
        var data = preferences.fetchUserPreference().getData()

        var subCategory = data?.sections?.get(0)?.subCategories?.get(0)
        Assert.assertEquals("refund-promotion", subCategory?.category)
        Assert.assertEquals(PreferenceOptions.OPT_OUT, subCategory?.preferenceOptions)


        //Update - OPT_IN and Verify
        every {
            networkClient.httpCall(
                url = any(),
                authorization = any(),
                requestMethod = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(
            ResponseStatus.SUCCESS,
            200,
            AssetHelper.readAssetFileToString("preference/category_update_opt_in.json")
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


        //Update channel whatsapp preference opt_out
        every {
            networkClient.httpCall(
                url = any(),
                authorization = any(),
                requestMethod = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(
            ResponseStatus.SUCCESS,
            200,
            AssetHelper.readAssetFileToString("preference/category_channel_whatsapp_update_opt_out.json")
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
        var channel  = subCategory?.channels?.last()
        Assert.assertEquals("whatsapp", channel?.channel)
        Assert.assertEquals(PreferenceOptions.OPT_OUT, channel?.preferenceOptions)

        //Update channel whatsapp preference opt_in
        every {
            networkClient.httpCall(
                url = any(),
                authorization = any(),
                requestMethod = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(
            ResponseStatus.SUCCESS,
            200,
            AssetHelper.readAssetFileToString("preference/category_channel_whatsapp_update_opt_in.json")
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
        channel  = subCategory?.channels?.last()
        Assert.assertEquals("whatsapp", channel?.channel)
        Assert.assertEquals(PreferenceOptions.OPT_IN, channel?.preferenceOptions)


        //Update - OPT_OUT and Verify
        every {
            networkClient.httpCall(
                url = any(),
                authorization = any(),
                requestMethod = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(
            ResponseStatus.SUCCESS,
            200,
            AssetHelper.readAssetFileToString("preference/category_update_opt_out.json")
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


    @Test
    fun verifyChannelPreferenceRestricted() {
        UserPreferenceRemote.networkClient = networkClient
        every {
            networkClient.httpCall(
                url = any(),
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
        val preferences = SuprSend.getInstance().user.getPreferences()
        preferences.setPreferenceConfig(
            tenantId = "T1",
            showOptOutChannels = false
        )
        var data = preferences.fetchUserPreference().getData()

        var channel = data?.channelPreferences?.get(0)
        Assert.assertEquals("androidpush", channel?.channel)
        Assert.assertEquals(false, channel?.isRestricted)



        //Update - ALL and Verify
        every {
            networkClient.httpCall(
                url = any(),
                authorization = any(),
                requestMethod = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(
            ResponseStatus.SUCCESS,
            200,
            AssetHelper.readAssetFileToString("preference/channel_preference_is_restricted_false.json")
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
            ResponseStatus.SUCCESS,
            200,
            AssetHelper.readAssetFileToString("preference/channel_preference_is_restricted_true.json")
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