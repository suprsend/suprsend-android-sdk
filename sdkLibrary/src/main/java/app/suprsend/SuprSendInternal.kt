package app.suprsend

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.WorkerThread
import app.suprsend.base.DeviceInfo
import app.suprsend.base.LocalStorage
import app.suprsend.base.NetworkClient
import app.suprsend.base.NetworkInfo
import app.suprsend.base.SSConstants
import app.suprsend.log.Logger
import app.suprsend.log.LoggerCallback
import app.suprsend.model.ApiResponse
import app.suprsend.model.ErrorType
import app.suprsend.model.ResponseStatus
import app.suprsend.utils.filterSSReservedKeys
import com.auth0.android.jwt.JWT
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@SuppressLint("StaticFieldLeak")
internal object SuprSendInternal {

    lateinit var context: Context

    var loggerCallback: LoggerCallback? = null
    lateinit var suprSendData: SuprSendData
    var networkClient = NetworkClient()

    fun identity(
        distinctId: String
    ): ApiResponse {

        if (distinctId.isBlank()) {
            return ApiResponse(
                status = ResponseStatus.ERROR,
                errorType = ErrorType.VALIDATION_ERROR,
                message = "distinctId is missing"
            )
        }

        // other user already present
        if (!suprSendData.distinctId.isNullOrBlank() && suprSendData.distinctId !== distinctId) {
            return ApiResponse(
                status = ResponseStatus.ERROR,
                errorType = ErrorType.VALIDATION_ERROR,
                message = "User already loggedin, reset current user to login new user"
            )
        }

        val localDistinctId = LocalStorage.getValue(SSConstants.CONFIG_DISTINCT_ID)
        if (distinctId == localDistinctId) {
            return ApiResponse(status = ResponseStatus.SUCCESS)
        }
        val apiResponse = trackEvent(
            eventName = "\$identify",
            distinctId = distinctId,
            properties = JSONObject().apply {
                put("\$identified_id", distinctId)
            },
            ignoreFilter = true
        )
        if (apiResponse.status == ResponseStatus.SUCCESS) {
            LocalStorage.setValue(SSConstants.CONFIG_DISTINCT_ID, distinctId)
            suprSendData.distinctId = distinctId
            appendNotificationToken()
        }
        return apiResponse
    }

    fun trackEvent(
        // Only in case of identity method distinct id is passed externally else from all places it is taken from cache
        distinctId: String = suprSendData.distinctId ?: "",
        eventName: String,
        properties: JSONObject = JSONObject(),
        ignoreFilter: Boolean = false
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
                if (distinctId.isBlank()) {
                    Logger.i(SSConstants.TAG_SUPRSEND, "Distinct id is missing - trackEvent $eventName")
                    return ApiResponse(
                        status = ResponseStatus.ERROR,
                        errorType = ErrorType.VALIDATION_ERROR,
                        message = "Distinct id is missing - trackEvent $eventName"
                    )
                }
                val operationStatus = refreshTokenIfRequired(distinctId = distinctId)
                if (!operationStatus.isSuccess())
                    return operationStatus
            }

            val eventPayload = this.buildTrackEventPayload(distinctId, eventName, properties, ignoreFilter)

            return networkClient.httpCall(
                url = "${suprSendData.host}/v2/event",
                authorization = suprSendData.publicApiKey ?: "",
                requestJson = eventPayload.toString(),
                headers = addSSSignature()
            )
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

            if (suprSendData.distinctId.isNullOrBlank()) {
                Logger.i(SSConstants.TAG_SUPRSEND, "Distinct id cannot be blank - trackOperator $operator")
                return ApiResponse(
                    status = ResponseStatus.ERROR,
                    message = "Distinct id cannot be blank - trackOperator $operator"
                )
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

            return networkClient.httpCall(
                url = "${suprSendData.host}/v2/event",
                authorization = suprSendData.publicApiKey ?: "",
                requestJson = eventPayload.toString(),
                headers = addSSSignature()
            )
        } catch (e: Exception) {
            return ApiResponse(
                status = ResponseStatus.ERROR,
                message = "Operator api call failed - $operator for $properties $propertiesJA"
            )
        }
    }

    fun refreshTokenIfRequired(distinctId: String, retryCount: Int = 1): ApiResponse {
        if (!NetworkInfo.isConnected()) {
            return ApiResponse(
                status = ResponseStatus.ERROR,
                message = "Internet connection is not available"
            )
        }
        val userTokenFetcher = suprSendData.userTokenFetcher
        if (userTokenFetcher != null) {
            var userToken = getToken() ?: ""

            if (userToken.isBlank()) {
                Logger.i(SSConstants.TAG_SUPRSEND, "Fetching user token for first time")
                userToken = userTokenFetcher.getToken(distinctId)
                storeToken(userToken)
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
                    if (!isJWTTokenExpired(userToken))
                        storeToken(userToken)
                    else {
                        Logger.e(SSConstants.TAG_SUPRSEND,"Invalid token has received : $userToken")
                    }
                    refreshTokenIfRequired(distinctId, retryCount + 1)
                } else {
                    ApiResponse(status = ResponseStatus.ERROR, message = "Your token is expired, retried ${SSConstants.MAX_REFRESH_TOKEN_RETRY} times still it failed")
                }
            }
        }
        return ApiResponse(status = ResponseStatus.SUCCESS, statusCode = 200, message = "refreshTokenIfRequired : Succeeded")
    }

    private fun isJWTTokenExpired(userToken: String): Boolean {
        val expiresOn = JWT(userToken).expiresAt?.time
        var hasExpired = true
        if (expiresOn != null) {
            hasExpired = expiresOn <= System.currentTimeMillis()
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
    fun removeNotificationToken(): ApiResponse? {
        val fcmToken = LocalStorage.getValue(SSConstants.CONFIG_FCM_PUSH_TOKEN)
        if (!fcmToken.isNullOrBlank()) {
            val jsonObject = JSONObject()
            jsonObject.put(SSConstants.PUSH_ANDROID_TOKEN, fcmToken)
            jsonObject.put(SSConstants.ID_PROVIDER, SSConstants.PUSH_VENDOR_FCM)
            jsonObject.put(SSConstants.DEVICE_ID, DeviceInfo.getDeviceId())
            return SuprSendInternal
                .trackOperator(
                    properties = jsonObject,
                    operator = SSConstants.REMOVE,
                    ignoreFilter = true
                )
        }
        return null
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

}