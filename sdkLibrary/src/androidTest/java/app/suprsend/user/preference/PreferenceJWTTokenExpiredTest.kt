package app.suprsend.user.preference

import app.suprsend.SuprSend
import app.suprsend.SuprSendInternal
import app.suprsend.UserTokenFetcher
import app.suprsend.base.AssetHelper
import app.suprsend.base.BaseTest
import app.suprsend.base.NetworkClient
import app.suprsend.base.Response
import app.suprsend.base.TestConstants
import app.suprsend.base.TokenGenerator
import app.suprsend.base.assertIsFailure
import app.suprsend.base.assertIsSuccess
import app.suprsend.model.ApiResponse
import app.suprsend.model.ResponseStatus
import app.suprsend.model.SuprSendOptions
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class PreferenceJWTTokenExpiredTest : BaseTest() {

    private val networkClient: NetworkClient = mockk(relaxed = true)
    private val userTokenFetcher = mockk<UserTokenFetcher>(relaxed = true)
    var suprSend: SuprSend
    var preferences: Preferences

    init {
        UserPreferenceRemote.networkClient = networkClient
        SuprSendInternal.networkClient = networkClient
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            options = SuprSendOptions(
                host = "https://collector-staging.suprsend.workers.dev"
            ),
            userTokenFetcher = userTokenFetcher
        )

        suprSend = SuprSend.getInstance()
        preferences = suprSend.user.getPreferences()
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

        preferences.setPreferenceConfig(
            tenantId = null,
            showOptOutChannels = true
        )
    }

    @Before
    fun setUp() {
        suprSend.reset(true)
        //Need correct token for identify to succeed
        every { userTokenFetcher.getToken(any()) } returns TokenGenerator.generateToken()

        val action = suprSend.identify("U1")
        action.assertIsSuccess()
    }

    @Test
    fun verifyTokenExpiredWhileFetchUserPreference() {

        //Expiring token before fetch call
        SuprSendInternal.storeToken(TokenGenerator.generateToken(System.currentTimeMillis() -3000))
        every { userTokenFetcher.getToken(any()) } returns TokenGenerator.generateToken(System.currentTimeMillis() -3000)

        //Even fetch full_preference has 200 response,even though test should fail since expired token is mocked
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

        val action = preferences.fetchUserPreference(fetchRemote = true)
        action.assertIsFailure()
        Assert.assertEquals("Your token is expired, retried 3 times still it failed", (action as Response.Error).message)

        //If correct token is sent then fetch preference success
        every { userTokenFetcher.getToken(any()) } returns TokenGenerator.generateToken()
        val action2 = preferences.fetchUserPreference(fetchRemote = true)
        action2.assertIsSuccess()
        Assert.assertEquals(5,action2.getData()?.sections?.size)
        Assert.assertEquals(5,action2.getData()?.channelPreferences?.size)
    }

    @Test
    fun verifyTokenExpiredWhileCategoryPreferenceUpdate() {

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

        val preferences = SuprSend.getInstance().user.getPreferences()
        val data = preferences.fetchUserPreference(fetchRemote = true).getData()

        Assert.assertEquals(5, data?.sections?.size)
        Assert.assertEquals(5, data?.channelPreferences?.size)

        //Expiring token before update call
        SuprSendInternal.storeToken(TokenGenerator.generateToken(System.currentTimeMillis() -3000))
        every { userTokenFetcher.getToken(any()) } returns TokenGenerator.generateToken(System.currentTimeMillis() -3000)

        var action = preferences.updateCategoryPreference(
            category = "refund-promotion",
            preference = PreferenceOptions.OPT_IN
        )

        action.assertIsFailure()
        Assert.assertEquals("Your token is expired, retried 3 times still it failed", (action as Response.Error).message)


        //If correct token is sent then update preference should success
        every { userTokenFetcher.getToken(any()) } returns TokenGenerator.generateToken()
        action = preferences.updateCategoryPreference(
            category = "refund-promotion",
            preference = PreferenceOptions.OPT_IN
        )
        action.assertIsSuccess()

    }

    @Test
    fun verifyTokenExpiredWhileChannelPreferenceUpdate() {

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

        val preferences = SuprSend.getInstance().user.getPreferences()
        val data = preferences.fetchUserPreference(fetchRemote = true).getData()

        Assert.assertEquals(5, data?.sections?.size)
        Assert.assertEquals(5, data?.channelPreferences?.size)

        //Expiring token before update call
        SuprSendInternal.storeToken(TokenGenerator.generateToken(System.currentTimeMillis() -3000))
        every { userTokenFetcher.getToken(any()) } returns TokenGenerator.generateToken(System.currentTimeMillis() -3000)

        var action = preferences.updateChannelPreferenceInCategory(
            category = "refund-promotion",
            preference = PreferenceOptions.OPT_IN,
            channel = "whatsapp"
        )

        action.assertIsFailure()
        Assert.assertEquals("Your token is expired, retried 3 times still it failed", (action as Response.Error).message)


        //If correct token is sent then update preference should success
        every { userTokenFetcher.getToken(any()) } returns TokenGenerator.generateToken()
        action = preferences.updateChannelPreferenceInCategory(
            category = "refund-promotion",
            preference = PreferenceOptions.OPT_IN,
            channel = "whatsapp"
        )
        action.assertIsSuccess()

    }
}