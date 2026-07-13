package app.suprsend

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.WorkerThread
import app.suprsend.base.DeviceInfo
import app.suprsend.base.LocalStorage
import app.suprsend.base.NetworkClient
import app.suprsend.base.NetworkInfo
import app.suprsend.base.SSConstants
import app.suprsend.event.PayloadOfflineStore
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
        userToken: String? = null,
        refreshUserToken: RefreshUserTokenCallback? = null,
        force: Boolean = false
    ): ApiResponse {
        if (refreshUserToken != null)
            suprSendData.refreshUserToken = refreshUserToken

        if (userToken != null)
            LocalStorage.setValue(SSConstants.USER_TOKEN, userToken)

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

        val localDistinctId = SDKPref.distinctId
        if (!force && distinctId == localDistinctId) {
            suprSendData.distinctId = distinctId
            return ApiResponse(status = ResponseStatus.SUCCESS)
        }
        SDKPref.distinctIdTry = distinctId
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
            SDKPref.distinctId = distinctId
            SDKPref.distinctIdTry = null
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
            val isNotificationEvent = listOf(
                SSConstants.S_EVENT_NOTIFICATION_DELIVERED,
                SSConstants.S_EVENT_NOTIFICATION_CLICKED,
                SSConstants.S_EVENT_NOTIFICATION_DISMISS
            ).contains(eventName)
            if (!isNotificationEvent) {
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

            // Notification events must not be dropped when the device is offline.
            // Persist the payload locally and let EventFlushHandler retry once connectivity returns.
            if (isNotificationEvent && !NetworkInfo.isConnected()) {
                PayloadOfflineStore.store(eventPayload)
                Logger.i(SSConstants.TAG_SUPRSEND, "Offline - queued $eventName for later flush")
                return ApiResponse(
                    status = ResponseStatus.SUCCESS,
                    message = "Internet not available, event queued for later flush"
                )
            }

            val httpResponse = networkClient.httpCall(
                url = "${suprSendData.host}/v2/event",
                authorization = suprSendData.publicApiKey ?: "",
                requestJson = eventPayload.toString(),
                headers = addSSSignature()
            )
            if (!httpResponse.isSuccess()) {
                // Race condition: connectivity may have dropped mid-call. Persist the
                // notification event so the periodic flush can still deliver it.
                if (isNotificationEvent && httpResponse.errorType == ErrorType.NETWORK_ERROR) {
                    PayloadOfflineStore.store(eventPayload)
                    Logger.i(SSConstants.TAG_SUPRSEND, "Network error - queued $eventName for later flush")
                    return ApiResponse(
                        status = ResponseStatus.SUCCESS,
                        message = "Internet not available, event queued for later flush"
                    )
                }
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
                url = "${suprSendData.host}/v2/event",
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
        val refreshUserToken = suprSendData.refreshUserToken

        if (refreshUserToken != null) {
            var userToken = getToken() ?: ""

            if (isJWTTokenExpired(userToken)) {
                return if (retryCount <= SSConstants.MAX_REFRESH_TOKEN_RETRY) {
                    if(userToken.isBlank()){
                        Logger.v(SSConstants.TAG_SUPRSEND, "User token is not present")
                    }else{
                        Logger.v(SSConstants.TAG_SUPRSEND, "User token is expired $userToken")
                    }
                    userToken = refreshUserToken.getToken(distinctId)
                    if (!isJWTTokenExpired(userToken)) {
                        storeToken(userToken)
                        Logger.v(SSConstants.TAG_SUPRSEND, "Got $distinctId $userToken")
                        val tryDistinctIdentity = SDKPref.distinctIdTry
                        val identifyFailedEarlier = !tryDistinctIdentity.isNullOrBlank()
                        if (!fromIdentify && identifyFailedEarlier) {
                            val response = identity(distinctId, force = true) // refresh
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
            return ApiResponse(status = ResponseStatus.SUCCESS, statusCode = 200, message = "refreshTokenIfRequired : Succeeded : $userToken")
        }
        return ApiResponse(status = ResponseStatus.SUCCESS, statusCode = 200, message = "JWT is disabled")
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
        // We finalized to not send device properties in payload will send in header
//        DeviceInfo.addDeviceInfoProperties(filteredProperties)
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
        val headersL = headers ?: hashMapOf()

        // Mirrors web SDK ApiClient.getHeaders(): the two suprsend user-agent
        // headers ride along with every outgoing request.
        suprSendData.userAgent
            ?.takeIf { it.isNotBlank() }
            ?.let {
                //Log.i(SSConstants.TAG_SUPRSEND, "X-Suprsend-User-Agent : $it")
                headersL["X-Suprsend-User-Agent"] = it
            }
        suprSendData.clientUserAgentJson
            ?.takeIf { it.isNotBlank() }
            ?.let {
                //Log.i(SSConstants.TAG_SUPRSEND, "X-Suprsend-Client-User-Agent : $it")
                headersL["X-Suprsend-Client-User-Agent"] = it
            }

        if (suprSendData.refreshUserToken != null) {
            headersL["x-ss-signature"] = getToken() ?: ""
        }

        return headersL.ifEmpty { null }
    }

    fun reset(unSubscribeNotification: Boolean) {
        if (unSubscribeNotification)
            removeNotificationToken()
        SSPreferenceInternal.clearUserPreference()
        SSInboxInternal.reset()
        suprSendData.distinctId = null
        LocalStorage.remove(SSConstants.USER_TOKEN)
        SDKPref.distinctId = null
        SDKPref.distinctIdTry = null
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
        val fcmToken = SDKPref.fcmToken
        if (!fcmToken.isNullOrBlank()) {
            val jsonObject = JSONObject()
            jsonObject.put(SSConstants.PUSH_ANDROID_TOKEN, fcmToken)
            jsonObject.put(SSConstants.ID_PROVIDER, SSConstants.PUSH_VENDOR_FCM)
            jsonObject.put(SSConstants.DEVICE_ID, DeviceInfo.getDeviceId())
            val response = trackOperator(
                operator = SSConstants.APPEND,
                properties = jsonObject,
                ignoreFilter = true
            )
            if(response.isSuccess()){
                SDKPref.fcmTokenSyncedToServer = true
            }
        }
    }

    @WorkerThread
    private fun removeNotificationToken(): ApiResponse? {
        val fcmToken = SDKPref.fcmToken
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
        val tryDistinctId = SDKPref.distinctIdTry?:""
        var action: ApiResponse? = null
        if (tryDistinctId.isNotBlank()) {
            action = identity(tryDistinctId) // try to identify
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