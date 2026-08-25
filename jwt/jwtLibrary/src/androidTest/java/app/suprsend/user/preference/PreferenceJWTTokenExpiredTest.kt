package app.suprsend.user.preference

import app.suprsend.Emitter
import app.suprsend.SSInternal
import app.suprsend.SuprSend
import app.suprsend.RefreshUserTokenCallback
import app.suprsend.base.AssetHelper
import app.suprsend.base.BaseTest
import app.suprsend.base.NetworkClient
import app.suprsend.base.TestConstants
import app.suprsend.base.TokenGenerator
import app.suprsend.base.assertIsFailure
import app.suprsend.base.assertIsSuccess
import app.suprsend.model.ApiResponse
import app.suprsend.model.ResponseStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PreferenceJWTTokenExpiredTest : BaseTest() {

    private val networkClient: NetworkClient = mockk(relaxed = true)
    private val refreshUserToken = mockk<RefreshUserTokenCallback>(relaxed = true)
    var suprSend: SuprSend
    var preferences: Preferences

    init {
        UserPreferenceRemote.networkClient = networkClient
        SSInternal.networkClient = networkClient
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            host = TestConstants.SS_BASE_URL
        )
        SuprSend.setRefreshUserToken(refreshUserToken)

        suprSend = SuprSend.getInstance()
        preferences = suprSend.preferences
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
    }

    @Before
    fun setUp() {
        Preferences.debounceDelayMs = 50L
        suprSend.reset(true)
        every { refreshUserToken.getToken(any()) } returns TokenGenerator.generateToken()

        val action = suprSend.identify("U1")
        action.assertIsSuccess()
    }

    @Test
    fun verifyTokenExpiredWhileFetchUserPreference() {
        SSInternal.storeToken(TokenGenerator.generateToken(System.currentTimeMillis() - 3000))
        every { refreshUserToken.getToken(any()) } returns TokenGenerator.generateToken(System.currentTimeMillis() - 3000)

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

        val action = preferences.getPreferences()
        action.assertIsFailure()
        Assert.assertEquals("Your token is expired, retried 3 times still it failed", action.error?.message)

        every { refreshUserToken.getToken(any()) } returns TokenGenerator.generateToken()
        val action2 = preferences.getPreferences()
        action2.assertIsSuccess()
        Assert.assertEquals(5, action2.body?.sections?.size)
        Assert.assertEquals(5, action2.body?.channelPreferences?.size)
    }

    @Test
    fun verifyTokenExpiredWhileCategoryPreferenceUpdate() {
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

        every {
            networkClient.httpCall(
                url = "${TestConstants.SS_BASE_URL}/v1/user/U1/preference/category/refund-promotion/?show_opt_out_channels=true",
                authorization = any(),
                requestMethod = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(
            status = ResponseStatus.SUCCESS,
            statusCode = 200,
            body = AssetHelper.readAssetFileToString("preference/category_update_opt_in.json")
        )

        val data = preferences.getPreferences().body
        Assert.assertEquals(5, data?.sections?.size)
        Assert.assertEquals(5, data?.channelPreferences?.size)

        SSInternal.storeToken(TokenGenerator.generateToken(System.currentTimeMillis() - 3000))
        every { refreshUserToken.getToken(any()) } returns TokenGenerator.generateToken(System.currentTimeMillis() - 3000)

        val errorLatch = CountDownLatch(1)
        var errorMessage: String? = null
        SuprSend.getInstance().emitter.on(Emitter.Event.preferencesError) { response ->
            errorMessage = response?.error?.message
            errorLatch.countDown()
        }

        var action = preferences.updateCategoryPreference(
            category = "refund-promotion",
            preference = PreferenceOptions.optIn
        )
        action.assertIsSuccess()
        Assert.assertTrue(errorLatch.await(2, TimeUnit.SECONDS))
        Assert.assertEquals("Your token is expired, retried 3 times still it failed", errorMessage)

        every { refreshUserToken.getToken(any()) } returns TokenGenerator.generateToken()
        action = preferences.updateCategoryPreference(
            category = "refund-promotion",
            preference = PreferenceOptions.optIn
        )
        action.assertIsSuccess()
    }

    @Test
    fun verifyTokenExpiredWhileChannelPreferenceUpdate() {
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

        every {
            networkClient.httpCall(
                url = "${TestConstants.SS_BASE_URL}/v1/user/U1/preference/category/refund-promotion/?show_opt_out_channels=true",
                authorization = any(),
                requestMethod = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(
            status = ResponseStatus.SUCCESS,
            statusCode = 200,
            body = AssetHelper.readAssetFileToString("preference/category_channel_whatsapp_update_opt_out.json")
        )

        val data = preferences.getPreferences().body
        Assert.assertEquals(5, data?.sections?.size)
        Assert.assertEquals(5, data?.channelPreferences?.size)

        SSInternal.storeToken(TokenGenerator.generateToken(System.currentTimeMillis() - 3000))
        every { refreshUserToken.getToken(any()) } returns TokenGenerator.generateToken(System.currentTimeMillis() - 3000)

        val errorLatch = CountDownLatch(1)
        var errorMessage: String? = null
        SuprSend.getInstance().emitter.on(Emitter.Event.preferencesError) { response ->
            errorMessage = response?.error?.message
            errorLatch.countDown()
        }

        var action = preferences.updateChannelPreferenceInCategory(
            channel = "whatsapp",
            preference = PreferenceOptions.optIn,
            category = "refund-promotion"
        )
        action.assertIsSuccess()
        Assert.assertTrue(errorLatch.await(2, TimeUnit.SECONDS))
        Assert.assertEquals("Your token is expired, retried 3 times still it failed", errorMessage)

        every { refreshUserToken.getToken(any()) } returns TokenGenerator.generateToken()
        action = preferences.updateChannelPreferenceInCategory(
            channel = "whatsapp",
            preference = PreferenceOptions.optIn,
            category = "refund-promotion"
        )
        action.assertIsSuccess()
    }
}
