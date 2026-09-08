package app.suprsend.feed

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
import org.json.JSONArray

/**
 * Shared setup for Feed tests: SDK init, mocked identify, mocked feed HTTP.
 */
open class FeedTestHelper : BaseTest() {

    protected val networkClient: NetworkClient = mockk(relaxed = true)

    protected val inboxHost = TestConstants.SS_INBOX_BASE_URL
    protected val socketHost = "https://staging-inbox-api.suprsend.com"

    protected fun identifyUser(distinctId: String = "U1") {
        SSInternal.networkClient = networkClient

        every {
            networkClient.httpCall(
                url = any(),
                authorization = any(),
                date = any(),
                requestJson = any(),
                requestMethod = any(),
                headers = any()
            )
        } answers {
            val url = firstArg<String>()
            when {
                url.contains("/v2/event") ->
                    success(AssetHelper.readAssetFileToString("event_and_operator_response.json"))

                url.contains("notifications_count") ->
                    success(AssetHelper.readAssetFileToString("feed/notifications_count.json"))

                url.contains("/notifications/") && (
                    url.contains("/seen") ||
                        url.contains("/read") ||
                        url.contains("/unread") ||
                        url.contains("/archive") ||
                        url.contains("/interacted")
                    ) -> success(AssetHelper.readAssetFileToString("feed/action_success.json"))

                url.contains("bulk/notifications/seen") ||
                    url.contains("reset_bell_count") ||
                    url.contains("mark_all_read") ->
                    success(AssetHelper.readAssetFileToString("feed/action_success.json"))

                url.contains("/notifications/") ->
                    success(AssetHelper.readAssetFileToString("feed/notification_detail.json"))

                url.contains("/notifications") ->
                    success(AssetHelper.readAssetFileToString("feed/notifications.json"))

                else -> ApiResponse(status = ResponseStatus.ERROR, statusCode = 404, message = "Unstubbed $url")
            }
        }

        SuprSend.initialize(
            context = context,
            publicApiKey = TestConstants.PUBLIC_API_KEY,
            host = TestConstants.SS_BASE_URL
        )
        val suprSend = SuprSend.getInstance()
        suprSend.reset(true)
        suprSend.identify(distinctId).assertIsSuccess()
    }

    protected fun createFeed(
        tenantId: String? = null,
        stores: List<IStore>? = null,
        pageSize: Int? = null
    ): Feed {
        return SuprSend.getInstance().feeds.initialize(
            IFeedOptions(
                tenantId = tenantId,
                pageSize = pageSize,
                stores = stores,
                host = FeedHost(socketHost = socketHost, apiHost = inboxHost)
            )
        )
    }

    protected fun storesFromAsset(): List<IStore> {
        return IStore.from(JSONArray(AssetHelper.readAssetFileToString("inbox/stores.json")))
    }

    private fun success(body: String): ApiResponse {
        return ApiResponse(status = ResponseStatus.SUCCESS, statusCode = 200, body = body)
    }
}
