package app.suprsend

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

class SuprSendJWTOffTest : BaseTest() {
    private val networkClient = mockk<NetworkClient>()

    @Test
    fun verifyNetworkErrorOnIdentityWithJWTOff() {
        every { networkClient.httpCall(any()) } returns ApiResponse(ResponseStatus.ERROR)
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
        val actionStatus = suprsend.identify("1231")
        Assert.assertEquals(false, actionStatus.isSuccess())
    }

    @Test
    fun verifyIdentityWithJWTOffSuccess() {
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
        val actionStatus = suprsend.identify("1231")
        Assert.assertEquals(true, actionStatus.isSuccess())
    }
}