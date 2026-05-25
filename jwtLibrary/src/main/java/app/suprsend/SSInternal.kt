package app.suprsend

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.WorkerThread
import app.suprsend.base.DeviceInfo
import app.suprsend.base.LocalStorage
import app.suprsend.base.NetworkClient
import app.suprsend.base.NetworkInfo
import app.suprsend.base.SSConstants
import app.suprsend.inbox.SSInboxInternal
import app.suprsend.log.Logger
import app.suprsend.log.LoggerCallback
import app.suprsend.model.ApiResponse
import app.suprsend.model.ErrorType
import app.suprsend.model.ResponseStatus
import app.suprsend.user.preference.SSPreferenceInternal
import app.suprsend.utils.filterSSReservedKeys
import com.auth0.android.jwt.JWT
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@SuppressLint("StaticFieldLeak")
internal object SSInternal {

    lateinit var context: Context

    var loggerCallback: LoggerCallback? = null
    var suprSendData: SuprSendData = SuprSendData()
    var networkClient = NetworkClient()

    fun identity(
        distinctId: String,
        force: Boolean = false
    ): ApiResponse {

        if (distinctId.isBlank()) {
            return ApiResponse(
                status = ResponseStatus.ERROR,
                errorType = ErrorType.VALIDATION_ERROR,
                message = "distinctId is missing"
            )
        }

        // other user already present
        if (!force && !suprSendData.distinctId.isNullOrBlank() && suprSendData.distinctId !== distinctId) {
            return ApiResponse(
                status = ResponseStatus.ERROR,
                errorType = ErrorType.VALIDATION_ERROR,
                message = "User already loggedin, reset current user to login new user"
            )
        }

        val localDistinctId = LocalStorage.getValue(SSConstants.CONFIG_DISTINCT_ID)
        if (!force && distinctId == localDistinctId) {
            return ApiResponse(status = ResponseStatus.SUCCESS)
        }
        LocalStorage.setValue(SSConstants.CONFIG_DISTINCT_ID_TRY, distinctId)
        val apiResponse = trackEvent(
            eventName = SSConstants.IDENTIFY,
            distinctId = distinctId,
            properties = JSONObject().apply {
                put(SSConstants.IDENTIFIED_ID, distinctId)
            },
            ignoreFilter = true,
            fromIdentify = true
        )
        if (apiResponse.status == ResponseStatus.SUCCESS) {
            LocalStorage.setValue(SSConstants.CONFIG_DISTINCT_ID, distinctId)
            LocalStorage.setValue(SSConstants.CONFIG_DISTINCT_ID_TRY, "")
            suprSendData.distinctId = distinctId
            appendNotificationToken()
        }
        return apiResponse
    }

    fun trackEvent(
        eventName: String,
        properties: JSONObject = JSONObject(),
        ignoreFilter: Boolean = false,
        // Only in case of identity method distinct id is passed externally else from all places it is taken from cache
        distinctId: String = suprSendData.distinctId ?: "",
        fromIdentify: Boolean = false
    ): ApiResponse {
        try {
            var canBiPassDistinctId = false
            if (listOf(
                    SSConstants.S_EVENT_NOTIFICATION_DELIVERED,
                    SSConstants.S_EVENT_NOTIFICATION_CLICKED,
                    SSConstants.S_EVENT_NOTIFICATION_DISMISS
                ).contains(eventName)
            ) {
                canBiPassDistinctId = true
            }
            if (!canBiPassDistinctId) {
                // Trying to identify(recover) if user has called identify earlier
                if (distinctId.isBlank()) {
                    val action = tryToIdentify("Distinct id is missing - trackEvent $eventName")
                    if (action != null)
                        return action
                }
                val operationStatus = refreshTokenIfRequired(distinctId = distinctId, fromIdentify = fromIdentify)
                if (!operationStatus.isSuccess())
                    return operationStatus
            }

            val eventPayload = this.buildTrackEventPayload(distinctId, eventName, properties, ignoreFilter)

            val httpResponse = networkClient.httpCall(
                url = "${suprSendData.baseUrl}/v2/event",
                authorization = suprSendData.publicApiKey ?: "",
                requestJson = eventPayload.toString(),
                headers = addSSSignature()
            )
            if (!httpResponse.isSuccess()) {
                checkStatusCodeAndRemoveLocalToken(httpResponse.body)
            }
            return httpResponse
        } catch (e: Exception) {
            return ApiResponse(
                status = ResponseStatus.ERROR,
                message = "Events api failed due to failure",
                exception = e
            )
        }
    }


    fun trackOperator(
        operator: String,
        properties: JSONObject? = null,
        propertiesJA: JSONArray? = null,
        ignoreFilter: Boolean = false
    ): ApiResponse {
        try {

            // Trying to identify(recover) if user has called identify earlier
            if (suprSendData.distinctId.isNullOrBlank()) {
                val action = tryToIdentify("Distinct id cannot be blank - trackOperator $operator")
                if (action != null)
                    return action
            }

            val distinctId = suprSendData.distinctId!!

            val operationStatus = refreshTokenIfRequired(distinctId = distinctId)
            if (!operationStatus.isSuccess())
                return operationStatus

            val eventPayload = buildOperatorPayload(
                distinctId = distinctId,
                operator = operator,
                properties = properties,
                propertiesJA = propertiesJA,
                ignoreFilter = ignoreFilter
            )

            val httpResponse =  networkClient.httpCall(
                url = "${suprSendData.baseUrl}/v2/event",
                authorization = suprSendData.publicApiKey ?: "",
                requestJson = eventPayload.toString(),
                headers = addSSSignature()
            )
            if (!httpResponse.isSuccess()) {
                checkStatusCodeAndRemoveLocalToken(httpResponse.body)
            }
            return httpResponse
        } catch (e: Exception) {
            return ApiResponse(
                status = ResponseStatus.ERROR,
                message = "Operator api call failed - $operator for $properties $propertiesJA"
            )
        }
    }

    fun refreshTokenIfRequired(
        distinctId: String,
        retryCount: Int = 1,
        fromIdentify: Boolean = false
    ): ApiResponse {
        if (!NetworkInfo.isConnected()) {
            return ApiResponse(
                status = ResponseStatus.ERROR,
                message = "Internet connection is not available"
            )
        }
        val userTokenFetcher = suprSendData.userTokenFetcher
        var userToken = getToken() ?: ""
        if (userTokenFetcher != null) {

            if (userToken.isBlank()) {
                Logger.v(SSConstants.TAG_SUPRSEND, "User token is blank")
                userToken = userTokenFetcher.getToken(distinctId)
                Logger.v(SSConstants.TAG_SUPRSEND, "Got $distinctId $userToken")
                storeToken(userToken)
                if (!fromIdentify) {
                    val response = identity(distinctId, force = true)
                    Logger.v(SSConstants.TAG_SUPRSEND, "Response : $response")
                }
            }
            if (userToken.isBlank()) {
                return ApiResponse(
                    status = ResponseStatus.ERROR,
                    message = "User token is still null after calling getToken"
                )
            }

            if (isJWTTokenExpired(userToken)) {
                return if (retryCount <= SSConstants.MAX_REFRESH_TOKEN_RETRY) {
                    Logger.i(SSConstants.TAG_SUPRSEND, "User token is expired")
                    userToken = userTokenFetcher.getToken(distinctId)
                    if (!isJWTTokenExpired(userToken)) {
                        storeToken(userToken)
                        Logger.v(SSConstants.TAG_SUPRSEND, "Got $distinctId $userToken")
                        if (!fromIdentify) {
                            val response = identity(distinctId, force = true)
                            Logger.v(SSConstants.TAG_SUPRSEND, "Response : $response")
                        }
                    } else {
                        Logger.e(SSConstants.TAG_SUPRSEND, "Invalid token has received : $userToken")
                    }
                    refreshTokenIfRequired(distinctId, retryCount + 1)
                } else {
                    ApiResponse(status = ResponseStatus.ERROR, statusCode = 401, message = "Your token is expired, retried ${SSConstants.MAX_REFRESH_TOKEN_RETRY} times still it failed")
                }
            }
        }
        return ApiResponse(status = ResponseStatus.SUCCESS, statusCode = 200, message = "refreshTokenIfRequired : Succeeded : $userToken")
    }

    private fun isJWTTokenExpired(userToken: String): Boolean {
        if(userToken.isBlank()) return true
        val expiresOn = JWT(userToken).expiresAt?.time
        var hasExpired = true
        if (expiresOn != null) {
            hasExpired = expiresOn <= (System.currentTimeMillis() + 3000)
        }
        return hasExpired
    }

    internal fun buildTrackEventPayload(
        distinctId: String,
        eventName: String,
        properties: JSONObject,
        ignoreFilter: Boolean = false
    ): JSONObject {
        val eventPayload = JSONObject()
        eventPayload.put(SSConstants.EVENT, eventName)
        eventPayload.put(SSConstants.DISTINCT_ID, distinctId)
        eventPayload.put(SSConstants.INSERT_ID, UUID.randomUUID().toString())
        eventPayload.put(SSConstants.TIME, System.currentTimeMillis())

        val filteredProperties = if (ignoreFilter) properties else properties.filterSSReservedKeys()
        DeviceInfo.addDeviceInfoProperties(filteredProperties)
        eventPayload.put(SSConstants.PROPERTIES, filteredProperties)

        return eventPayload
    }

    internal fun buildOperatorPayload(
        distinctId: String,
        operator: String,
        properties: JSONObject? = null,
        propertiesJA: JSONArray? = null,
        ignoreFilter: Boolean = false
    ): JSONObject {
        val eventPayload = JSONObject()
        eventPayload.put(SSConstants.DISTINCT_ID, distinctId)
        eventPayload.put(SSConstants.INSERT_ID, UUID.randomUUID().toString())
        eventPayload.put(SSConstants.TIME, System.currentTimeMillis())
        if (properties != null) {
            eventPayload.put(operator, if (ignoreFilter) properties else properties.filterSSReservedKeys())
        }

        if (propertiesJA != null) {
            if (propertiesJA.length() == 0) {
                Logger.i(SSConstants.TAG_SUPRSEND, "In $operator properties are empty")
            }
            eventPayload.put(operator, propertiesJA)
        }
        return eventPayload
    }

    fun addSSSignature(
        headers: MutableMap<String, String>? = null
    ): Map<String, String>? {
        if (suprSendData.userTokenFetcher != null) {
            val headersL = headers ?: hashMapOf()
            headersL["x-ss-signature"] = getToken() ?: ""
            return headersL
        }
        return headers
    }

    fun reset(unSubscribeNotification: Boolean) {
        if (unSubscribeNotification)
            removeNotificationToken()
        SSPreferenceInternal.clearUserPreference()
        SSInboxInternal.reset()
        suprSendData.distinctId = null
        LocalStorage.remove(SSConstants.USER_TOKEN)
        LocalStorage.remove(SSConstants.CONFIG_DISTINCT_ID)
        LocalStorage.remove(SSConstants.CONFIG_DISTINCT_ID_TRY)
    }


    fun storeToken(userToken: String?) {
        if (userToken == null) {
            LocalStorage.remove(SSConstants.USER_TOKEN)
        } else {
            LocalStorage.setValue(SSConstants.USER_TOKEN, userToken)
        }
    }

    fun getToken(): String? {
        return LocalStorage.getValue(SSConstants.USER_TOKEN)
    }

    @WorkerThread
    private fun appendNotificationToken() {
        val fcmToken = LocalStorage.getValue(SSConstants.CONFIG_FCM_PUSH_TOKEN)
        if (!fcmToken.isNullOrBlank()) {
            val jsonObject = JSONObject()
            jsonObject.put(SSConstants.PUSH_ANDROID_TOKEN, fcmToken)
            jsonObject.put(SSConstants.ID_PROVIDER, SSConstants.PUSH_VENDOR_FCM)
            jsonObject.put(SSConstants.DEVICE_ID, DeviceInfo.getDeviceId())
            trackOperator(
                operator = SSConstants.APPEND,
                properties = jsonObject,
                ignoreFilter = true
            )
        }
    }

    @WorkerThread
    private fun removeNotificationToken(): ApiResponse? {
        val fcmToken = LocalStorage.getValue(SSConstants.CONFIG_FCM_PUSH_TOKEN)
        if (!fcmToken.isNullOrBlank()) {
            val jsonObject = JSONObject()
            jsonObject.put(SSConstants.PUSH_ANDROID_TOKEN, fcmToken)
            jsonObject.put(SSConstants.ID_PROVIDER, SSConstants.PUSH_VENDOR_FCM)
            jsonObject.put(SSConstants.DEVICE_ID, DeviceInfo.getDeviceId())
            return trackOperator(
                properties = jsonObject,
                operator = SSConstants.REMOVE,
                ignoreFilter = true
            )
        }
        return null
    }

    private fun tryToIdentify(log: String): ApiResponse? {
        val tryDistinctId = LocalStorage.getValue(SSConstants.CONFIG_DISTINCT_ID_TRY) ?: ""
        var action: ApiResponse? = null
        if (tryDistinctId.isNotBlank()) {
            action = identity(tryDistinctId)
        }
        if (action?.isSuccess() == false || tryDistinctId.isBlank()) {
            Logger.i(SSConstants.TAG_SUPRSEND, log)
            return ApiResponse(
                status = ResponseStatus.ERROR,
                errorType = ErrorType.VALIDATION_ERROR,
                message = log
            )
        }
        return null
    }

    fun isSuprSendDataInitialized(): Boolean {
        return !suprSendData.publicApiKey.isNullOrBlank()
    }

    fun checkStatusCodeAndRemoveLocalToken(errorBody: String?) {
        try {
            val errorResponseStr = errorBody ?: "{}"
            val errorResponse = JSONObject(errorResponseStr)
            val type = errorResponse.optJSONObject("error")?.optString("type")
            if (type == "token_invalid") {
                LocalStorage.remove(SSConstants.USER_TOKEN)
                refreshTokenIfRequired(distinctId = suprSendData.distinctId ?: "")
            }
        } catch (e: Exception) {
            Logger.e(SSConstants.TAG_SUPRSEND, e)
        }
    }

}