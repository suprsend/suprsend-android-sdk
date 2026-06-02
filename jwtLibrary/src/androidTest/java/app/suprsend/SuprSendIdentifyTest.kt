package app.suprsend

import app.suprsend.base.AssetHelper
import app.suprsend.base.BaseTest
import app.suprsend.base.NetworkClient
import app.suprsend.base.TestConstants
import app.suprsend.base.TokenGenerator
import app.suprsend.base.assertMessageId
import app.suprsend.model.ApiResponse
import app.suprsend.model.ErrorType
import app.suprsend.model.ResponseStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

class SuprSendIdentifyTest : BaseTest() {
    private val networkClient = mockk<NetworkClient>()

    @Test
    fun verifyIdentityDistinctIdBlank() {
        val refreshTokenCallback = mockk<RefreshTokenCallback>(relaxed = true)
        SuprSend.initialize(
            context = context,
            
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            host = TestConstants.SS_BASE_URL,
        )
        SuprSend.setRefreshTokenCallback(refreshTokenCallback)
        SSInternal.networkClient = networkClient
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        val actionStatus = suprsend.identify("")
        Assert.assertEquals(ResponseStatus.ERROR, actionStatus.status)
        Assert.assertEquals(ErrorType.VALIDATION_ERROR, actionStatus.errorType)
        Assert.assertEquals("distinctId is missing", actionStatus.message)
    }

    @Test
    fun verifyIdentityCalledWhenAlreadyIdentified() {
        val refreshTokenCallback = mockk<RefreshTokenCallback>(relaxed = true)
        every { refreshTokenCallback.getToken(any()) } returns TokenGenerator.generateToken()
        every {
            networkClient.httpCall(
                url = any(),
                authorization = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(ResponseStatus.SUCCESS)
        SuprSend.initialize(
            context = context,
            
            publicApiKey = TestConstants.PUBLIC_API_KEY,
                host = TestConstants.SS_BASE_URL,
        )
        SuprSend.setRefreshTokenCallback(refreshTokenCallback)
        SSInternal.networkClient = networkClient
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        var actionStatus = suprsend.identify("U1")
        Assert.assertEquals(true, actionStatus.isSuccess())

        actionStatus = suprsend.identify("U2")
        Assert.assertEquals(false, actionStatus.isSuccess())
        Assert.assertEquals(ResponseStatus.ERROR, actionStatus.status)
        Assert.assertEquals(ErrorType.VALIDATION_ERROR, actionStatus.errorType)
        Assert.assertEquals("User already loggedin, reset current user to login new user", actionStatus.message)
    }

    @Test
    fun verifyIdentityFailureAfter3Retries() {
        val refreshTokenCallback = mockk<RefreshTokenCallback>(relaxed = true)
        every { refreshTokenCallback.getToken(any()) } returnsMany (
                listOf(
                    TokenGenerator.generateToken(System.currentTimeMillis() - 3000), // Expired Token
                    TokenGenerator.generateToken(System.currentTimeMillis() - 3000), // Expired Token
                    TokenGenerator.generateToken(System.currentTimeMillis() - 3000), // Expired Token
                )
                )
        every {
            networkClient.httpCall(
                url = any(),
                authorization = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(ResponseStatus.SUCCESS)
        SuprSend.initialize(
            context = context,
            
            publicApiKey = TestConstants.PUBLIC_API_KEY,
                host = TestConstants.SS_BASE_URL,
        )
        SuprSend.setRefreshTokenCallback(refreshTokenCallback)
        SSInternal.networkClient = networkClient
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        val actionStatus = suprsend.identify("1231")
        Assert.assertEquals(false, actionStatus.isSuccess())
        Assert.assertEquals("Your token is expired, retried 3 times still it failed", actionStatus.message)
    }

    @Test
    fun verifyIdentityRecoveredAt3rdAttempt() {
        val refreshTokenCallback = mockk<RefreshTokenCallback>(relaxed = true)
        every { refreshTokenCallback.getToken(any()) } returnsMany (
                listOf(
                    TokenGenerator.generateToken(System.currentTimeMillis() - 3000), // Expired Token
                    TokenGenerator.generateToken(System.currentTimeMillis() - 3000), // Expired Token
                    TokenGenerator.generateToken()
                )
                )
        every {
            networkClient.httpCall(
                url = any(),
                authorization = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(ResponseStatus.SUCCESS)
        SuprSend.initialize(
            context = context,
            
            publicApiKey = TestConstants.PUBLIC_API_KEY,
                host = TestConstants.SS_BASE_URL,
        )
        SuprSend.setRefreshTokenCallback(refreshTokenCallback)
        SSInternal.networkClient = networkClient
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        val actionStatus = suprsend.identify("1231")
        Assert.assertEquals(true, actionStatus.isSuccess())
    }

    @Test
    fun verifyIdentityFailure() {
        val refreshTokenCallback = mockk<RefreshTokenCallback>(relaxed = true)
        every {
            networkClient.httpCall(
                url = any(),
                authorization = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(ResponseStatus.ERROR)
        SuprSend.initialize(
            context = context,
            
            publicApiKey = TestConstants.PUBLIC_API_KEY,
                host = TestConstants.SS_BASE_URL,
        )
        SuprSend.setRefreshTokenCallback(refreshTokenCallback)
        SSInternal.networkClient = networkClient
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        val actionStatus = suprsend.identify("1231")
        Assert.assertEquals(false, actionStatus.isSuccess())
    }

    @Test
    fun verifyIdentitySuccess() {
        val refreshTokenCallback = mockk<RefreshTokenCallback>(relaxed = true)
        every {
            networkClient.httpCall(
                url = any(),
                authorization = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(
            status = ResponseStatus.SUCCESS,
            statusCode = 200,
            body = AssetHelper.readAssetFileToString("event_and_operator_response.json")
        )

        every { refreshTokenCallback.getToken(any()) } returns TokenGenerator.generateToken()
        SuprSend.initialize(
            context = context,
            
            publicApiKey = TestConstants.PUBLIC_API_KEY,
                host = TestConstants.SS_BASE_URL,
        )
        SuprSend.setRefreshTokenCallback(refreshTokenCallback)
        SSInternal.networkClient = networkClient
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        val actionStatus = suprsend.identify("1231")
        actionStatus.assertMessageId()
    }

}