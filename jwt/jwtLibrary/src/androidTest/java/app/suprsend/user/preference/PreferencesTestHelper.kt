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

/**
 * Shared setup for Preferences tests: SDK init, mocked identify, mocked preference HTTP.
 */
open class PreferencesTestHelper : BaseTest() {

    protected val networkClient: NetworkClient = mockk(relaxed = true)

    protected fun identifyUser(distinctId: String = "U1") {
        Preferences.debounceDelayMs = 60_000L
        UserPreferenceRemote.networkClient = networkClient
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
            val method = (invocation.args.getOrNull(4) as? String) ?: "GET"
            val isCategoryList = url.contains("/preference/category/?") ||
                url.matches(Regex(".*/preference/category/(\\?.*)?$"))
            when {
                url.contains("/v2/event") ->
                    success(AssetHelper.readAssetFileToString("event_and_operator_response.json"))

                method.equals("PATCH", ignoreCase = true) && url.contains("/preference/category/") ->
                    success(AssetHelper.readAssetFileToString("preference/category_update_opt_in.json"))

                method.equals("PATCH", ignoreCase = true) && url.contains("/preference/channel_preference") ->
                    success(AssetHelper.readAssetFileToString("preference/channel_preference_is_restricted_true.json"))

                url.contains("/preference/category/") && !isCategoryList ->
                    success(AssetHelper.readAssetFileToString("preference/category_update_opt_in.json"))

                url.contains("/preference/category") ->
                    success(AssetHelper.readAssetFileToString("preference/categories.json"))

                url.contains("/preference/channel_preference") ->
                    success(AssetHelper.readAssetFileToString("preference/channel_preference_is_restricted_false.json"))

                url.contains("/preference/") ->
                    success(AssetHelper.readAssetFileToString("preference/full_preference_1.json"))

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

    protected fun preferences(): Preferences = SuprSend.getInstance().preferences

    private fun success(body: String): ApiResponse {
        return ApiResponse(status = ResponseStatus.SUCCESS, statusCode = 200, body = body)
    }
}
