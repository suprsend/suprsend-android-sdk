package app.suprsend

import app.suprsend.base.BaseTest
import app.suprsend.base.LocalStorage
import app.suprsend.base.NetworkClient
import app.suprsend.base.SSConstants
import app.suprsend.base.TestConstants
import app.suprsend.base.TokenGenerator
import app.suprsend.model.ApiResponse
import app.suprsend.model.ErrorType
import app.suprsend.model.ResponseStatus
import app.suprsend.model.SuprSendOptions
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class SuprSendTrackSetTest : BaseTest() {
    private val networkClient = mockk<NetworkClient>()

    @Test
    fun verifyTrackEventFailureDueToDistinctIdMissing() {
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
            options = SuprSendOptions(
                "https://collector-staging.suprsend.workers.dev"
            )
        )
        SuprSendInternal.networkClient = networkClient
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        var actionStatus = suprsend.trackEvent("Home Viewed")
        Assert.assertEquals(false, actionStatus.isSuccess())
        Assert.assertEquals(ErrorType.VALIDATION_ERROR, actionStatus.errorType)
        Assert.assertEquals("Distinct id is missing - trackEvent Home Viewed", actionStatus.message)

        //With properties
        actionStatus = suprsend.trackEvent("Home Viewed", JSONObject().apply {
            put("time", System.currentTimeMillis())
            put("app", "test_sdk")
        })
        Assert.assertEquals(false, actionStatus.isSuccess())
        Assert.assertEquals(ErrorType.VALIDATION_ERROR, actionStatus.errorType)
        Assert.assertEquals("Distinct id is missing - trackEvent Home Viewed", actionStatus.message)
    }

    @Test
    fun verifyTrackEventFailure() {
        every {
            networkClient.httpCall(
                url = any(),
                authorization = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(ResponseStatus.SUCCESS) andThen ApiResponse(ResponseStatus.ERROR)
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            options = SuprSendOptions(
                "https://collector-staging.suprsend.workers.dev"
            )
        )
        SuprSendInternal.networkClient = networkClient
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        var actionStatus = suprsend.identify("1231")
        Assert.assertEquals(true, actionStatus.isSuccess()) // Identity success

        actionStatus = suprsend.trackEvent("Home Viewed")
        Assert.assertEquals(false, actionStatus.isSuccess()) // Track event failure

        actionStatus = suprsend.trackEvent("Home Viewed", JSONObject().apply {
            put("time", System.currentTimeMillis())
            put("app", "test_sdk")
        })
        Assert.assertEquals(false, actionStatus.isSuccess()) // Track event with properties failure
    }

    @Test
    fun verifyTrackEventFailureDueToTokenExpired() {
        val userTokenFetcher = mockk<UserTokenFetcher>(relaxed = true)
        every { userTokenFetcher.getToken(any()) } returns TokenGenerator.generateToken() andThen TokenGenerator.generateToken(System.currentTimeMillis() - 3000)
        every {
            networkClient.httpCall(
                url = any(),
                authorization = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(ResponseStatus.SUCCESS) andThen ApiResponse(ResponseStatus.ERROR)
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            options = SuprSendOptions(
                "https://collector-staging.suprsend.workers.dev"
            ),
            userTokenFetcher = userTokenFetcher
        )
        SuprSendInternal.networkClient = networkClient
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        var actionStatus = suprsend.identify("1231")
        Assert.assertEquals(true, actionStatus.isSuccess()) // Identity success

        //Lets store expire token
        SuprSendInternal.storeToken(TokenGenerator.generateToken(System.currentTimeMillis() - 3000))

        actionStatus = suprsend.trackEvent("Home Viewed")
        Assert.assertEquals(false, actionStatus.isSuccess()) // Track event failure
        Assert.assertEquals("Your token is expired, retried 3 times still it failed", actionStatus.message)

        actionStatus = suprsend.trackEvent("Home Viewed", JSONObject().apply {
            put("time", System.currentTimeMillis())
            put("app", "test_sdk")
        })
        Assert.assertEquals(false, actionStatus.isSuccess()) // Track event with properties failure
        Assert.assertEquals("Your token is expired, retried 3 times still it failed", actionStatus.message)
    }

    @Test
    fun verifyTrackEventByPassNotificationEvent() {
        val userTokenFetcher = mockk<UserTokenFetcher>(relaxed = true)
        // Even for expired token trackEvent method for notification events should succeed as it is bypassed from backend
        every { userTokenFetcher.getToken(any()) } returns TokenGenerator.generateToken() andThen TokenGenerator.generateToken(System.currentTimeMillis() - 3000)
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
            options = SuprSendOptions(
                "https://collector-staging.suprsend.workers.dev"
            ),
            userTokenFetcher = userTokenFetcher
        )
        SuprSendInternal.networkClient = networkClient
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        var actionStatus = suprsend.identify("1231")
        Assert.assertEquals(true, actionStatus.isSuccess()) // Identity success

        SuprSendInternal.storeToken(TokenGenerator.generateToken(System.currentTimeMillis() - 3000))
        actionStatus = suprsend.trackEvent(eventName = SSConstants.S_EVENT_NOTIFICATION_DELIVERED,
            properties = JSONObject().apply {
                put("id", "M1")
            })
        Assert.assertEquals(true, actionStatus.isSuccess())

        actionStatus = suprsend.trackEvent(eventName = SSConstants.S_EVENT_NOTIFICATION_CLICKED,
            properties = JSONObject().apply {
                put("id", "M1")
            })
        Assert.assertEquals(true, actionStatus.isSuccess())

        actionStatus = suprsend.trackEvent(eventName = SSConstants.S_EVENT_NOTIFICATION_DISMISS,
            properties = JSONObject().apply {
                put("id", "M1")
            })
        Assert.assertEquals(true, actionStatus.isSuccess())

    }

    @Test
    fun verifyTrackEventSuccess() {
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
            options = SuprSendOptions(
                "https://collector-staging.suprsend.workers.dev"
            )
        )
        SuprSendInternal.networkClient = networkClient
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        var actionStatus = suprsend.identify("1231")
        Assert.assertEquals(true, actionStatus.isSuccess()) // Identity success

        actionStatus = suprsend.trackEvent("Home Viewed")
        Assert.assertEquals(true, actionStatus.isSuccess())

        actionStatus = suprsend.trackEvent("Home Viewed", JSONObject().apply {
            put("time", System.currentTimeMillis())
            put("app", "test_sdk")
        })
        Assert.assertEquals(true, actionStatus.isSuccess())
    }

    @Test
    fun verifySetOperatorFailure() {
        every {
            networkClient.httpCall(
                url = any(),
                authorization = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(ResponseStatus.SUCCESS) andThen ApiResponse(ResponseStatus.ERROR)
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            options = SuprSendOptions(
                "https://collector-staging.suprsend.workers.dev"
            )
        )
        SuprSendInternal.networkClient = networkClient
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        var actionStatus = suprsend.identify("1231")
        Assert.assertEquals(true, actionStatus.isSuccess()) // Identity success

        actionStatus = suprsend.user.set("S1", "V1")
        Assert.assertEquals(false, actionStatus.isSuccess()) // Track event failure

        actionStatus = suprsend.user.set("S2", JSONObject().apply {
            put("time", System.currentTimeMillis())
            put("app", "test_sdk")
        })
        Assert.assertEquals(false, actionStatus.isSuccess()) // Track event with properties failure
    }

    @Test
    fun verifySetOperatorFailureDueToTokenExpired() {
        val userTokenFetcher = mockk<UserTokenFetcher>(relaxed = true)
        every { userTokenFetcher.getToken(any()) } returns TokenGenerator.generateToken() andThen TokenGenerator.generateToken(System.currentTimeMillis() - 3000)
        every {
            networkClient.httpCall(
                url = any(),
                authorization = any(),
                requestJson = any(),
                headers = any()
            )
        } returns ApiResponse(ResponseStatus.SUCCESS) andThen ApiResponse(ResponseStatus.ERROR)
        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            options = SuprSendOptions(
                "https://collector-staging.suprsend.workers.dev"
            ),
            userTokenFetcher = userTokenFetcher
        )
        SuprSendInternal.networkClient = networkClient
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        var actionStatus = suprsend.identify("1231")
        Assert.assertEquals(true, actionStatus.isSuccess()) // Identity success

        //Lets store expire token
        SuprSendInternal.storeToken(TokenGenerator.generateToken(System.currentTimeMillis() - 3000))

        actionStatus = suprsend.user.set("S1", "V1")
        Assert.assertEquals(false, actionStatus.isSuccess()) // Track event failure
        Assert.assertEquals("Your token is expired, retried 3 times still it failed", actionStatus.message)

        actionStatus = suprsend.user.set("S2", JSONObject().apply {
            put("time", System.currentTimeMillis())
            put("app", "test_sdk")
        })
        Assert.assertEquals(false, actionStatus.isSuccess()) // Track event with properties failure
        Assert.assertEquals("Your token is expired, retried 3 times still it failed", actionStatus.message)
    }

    @Test
    fun verifySetOperatorSuccess() {
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
            options = SuprSendOptions(
                "https://collector-staging.suprsend.workers.dev"
            )
        )
        SuprSendInternal.networkClient = networkClient
        val suprsend = SuprSend.getInstance()
        suprsend.reset(true)
        var actionStatus = suprsend.identify("1231")
        Assert.assertEquals(true, actionStatus.isSuccess()) // Identity success

        actionStatus = suprsend.user.set("S1", "V1")
        Assert.assertEquals(true, actionStatus.isSuccess())

        actionStatus = suprsend.user.set("S2", JSONObject().apply {
            put("time", System.currentTimeMillis())
            put("app", "test_sdk")
        })
        Assert.assertEquals(true, actionStatus.isSuccess())
    }
}