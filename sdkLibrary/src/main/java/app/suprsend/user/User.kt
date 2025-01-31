package app.suprsend.user

import androidx.annotation.WorkerThread
import app.suprsend.SuprSendInternal
import app.suprsend.base.ActionStatusCallback
import app.suprsend.base.DeviceInfo
import app.suprsend.base.LocalStorage
import app.suprsend.base.SSConstants
import app.suprsend.base.sdkExecutorService
import app.suprsend.log.Logger
import app.suprsend.model.ApiResponse
import app.suprsend.model.ResponseStatus
import app.suprsend.user.preference.Preferences
import app.suprsend.user.preference.PreferencesImpl
import app.suprsend.utils.isMobileNumberValid
import app.suprsend.utils.isValidEmail
import app.suprsend.utils.runOnUIThread
import org.json.JSONArray
import org.json.JSONObject

class User() {

    private val preference = PreferencesImpl()

    fun getPreferences(): Preferences {
        return preference
    }

    @WorkerThread
    fun setPreferredLanguage(language: String): ApiResponse {
        return SuprSendInternal.trackOperator(
            operator = SSConstants.SET,
            properties = JSONObject().apply {
                put(SSConstants.PREFERRED_LANGUAGE, language)
            },
            ignoreFilter = true
        )
    }

    fun setPreferredLanguageAsync(language: String, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = setPreferredLanguage(language)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute setPreferredLanguage", exception = e))
            }
        }
    }

    @WorkerThread
    fun setTimezone(timezone: String): ApiResponse {
        return SuprSendInternal.trackOperator(
            operator = SSConstants.SET,
            properties = JSONObject().apply {
                put(SSConstants.TIME_ZONE, timezone)
            },
            ignoreFilter = true
        )
    }

    fun setTimezoneAsync(timezone: String, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = setTimezone(timezone)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute setTimezone", exception = e))
            }
        }
    }

    @WorkerThread
    fun set(key: String, value: Any): ApiResponse {
        return SuprSendInternal.trackOperator(
            operator = SSConstants.SET,
            properties = JSONObject().apply {
                put(key, value)
            }
        )
    }

    fun setAsync(key: String, value: Any, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = set(key, value)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute set", exception = e))
            }
        }
    }

    @WorkerThread
    fun set(properties: JSONObject): ApiResponse {
        if (properties.length() == 0) {
            Logger.i(SSConstants.TAG_SUPRSEND, "data provided is empty")
            return ApiResponse(status = ResponseStatus.ERROR, message = "data provided is empty")
        }
        return SuprSendInternal.trackOperator(
            operator = SSConstants.SET,
            properties = properties
        )
    }

    fun setAsync(properties: JSONObject, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = set(properties)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute set", exception = e))
            }
        }
    }

    @WorkerThread
    fun unSet(key: String): ApiResponse {
        return SuprSendInternal.trackOperator(
            operator = SSConstants.UNSET,
            propertiesJA = JSONArray().apply {
                put(key)
            }
        )
    }

    fun unSetAsync(key: String, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = unSet(key)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute unSet", exception = e))
            }
        }
    }

    @WorkerThread
    fun unSet(keys: List<String>): ApiResponse {
        return SuprSendInternal.trackOperator(
            operator = SSConstants.UNSET,
            propertiesJA = JSONArray().apply {
                keys.forEach { key ->
                    put(key)
                }
            }
        )
    }

    fun unSetAsync(keys: List<String>, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = unSet(keys)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute unSet", exception = e))
            }
        }
    }

    @WorkerThread
    fun setOnce(key: String, value: Any): ApiResponse {
        return SuprSendInternal.trackOperator(
            operator = SSConstants.SET_ONCE,
            properties = JSONObject().apply {
                put(key, value)
            }
        )
    }

    fun setOnceAsync(key: String, value: Any, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = setOnce(key, value)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute setOnce", exception = e))
            }
        }
    }

    @WorkerThread
    fun setOnce(properties: JSONObject): ApiResponse {
        return SuprSendInternal.trackOperator(
            operator = SSConstants.SET_ONCE,
            properties = properties
        )
    }

    fun setOnceAsync(properties: JSONObject, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = setOnce(properties)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute setOnce", exception = e))
            }
        }
    }

    /**
     * Used to increment/decrement user properties whose values are numbers. To decrement use -ve values.
     * Keys with $ and ss_ will be removed.
     */
    @WorkerThread
    fun increment(key: String, value: Number): ApiResponse {
        return SuprSendInternal.trackOperator(
            operator = SSConstants.ADD,
            properties = JSONObject().apply {
                put(key, value)
            }
        )
    }

    fun incrementAsync(key: String, value: Number, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = increment(key, value)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute increment", exception = e))
            }
        }
    }

    /**
     * Used to increment/decrement user properties whose values are numbers. To decrement use -ve values.
     * Keys with $ and ss_ will be removed.
     */
    @WorkerThread
    fun increment(properties: Map<String, Number>): ApiResponse {
        return SuprSendInternal.trackOperator(
            operator = SSConstants.ADD,
            properties = JSONObject().apply {
                properties.forEach { (key, value) ->
                    put(key, value)
                }
            }
        )
    }

    fun incrementAsync(properties: Map<String, Number>, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = increment(properties)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute increment", exception = e))
            }
        }
    }

    @WorkerThread
    fun append(key: String, value: Any): ApiResponse {
        return SuprSendInternal.trackOperator(
            operator = SSConstants.APPEND,
            properties = JSONObject().apply {
                put(key, value)
            }
        )
    }

    fun appendAsync(key: String, value: Any, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = append(key, value)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute append", exception = e))
            }
        }
    }

    @WorkerThread
    fun append(properties: JSONObject): ApiResponse {
        return SuprSendInternal.trackOperator(
            operator = SSConstants.APPEND,
            properties = properties
        )
    }

    fun appendAsync(properties: JSONObject, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = append(properties)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute append", exception = e))
            }
        }
    }

    @WorkerThread
    fun remove(key: String, value: Any): ApiResponse {
        return SuprSendInternal.trackOperator(
            operator = SSConstants.REMOVE,
            properties = JSONObject().apply {
                put(key, value)
            }
        )
    }

    fun removeAsync(key: String, value: Any, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = remove(key, value)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute remove", exception = e))
            }
        }
    }

    @WorkerThread
    fun remove(properties: JSONObject): ApiResponse {
        return SuprSendInternal.trackOperator(
            operator = SSConstants.REMOVE,
            properties = properties
        )
    }

    fun removeAsync(properties: JSONObject, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = remove(properties)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute remove", exception = e))
            }
        }
    }

    @WorkerThread
    fun addEmail(email: String): ApiResponse {
        if (!email.isValidEmail()) {
            return ApiResponse(
                status = ResponseStatus.ERROR, message = "Email is not valid : $email"
            )
        }
        return SuprSendInternal.trackOperator(
            operator = SSConstants.APPEND,
            properties = JSONObject().apply {
                put(SSConstants.EMAIL, email)
            },
            ignoreFilter = true
        )
    }

    fun addEmailAsync(email: String, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = addEmail(email)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute addEmail", exception = e))
            }
        }
    }

    @WorkerThread
    fun removeEmail(email: String): ApiResponse {
        if (!email.isValidEmail()) {
            return ApiResponse(
                status = ResponseStatus.ERROR, message = "Email is not valid : $email"
            )
        }
        return SuprSendInternal.trackOperator(
            operator = SSConstants.REMOVE,
            properties = JSONObject().apply {
                put(SSConstants.EMAIL, email)
            },
            ignoreFilter = true
        )
    }

    fun removeEmailAsync(email: String, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = removeEmail(email)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute removeEmail", exception = e))
            }
        }
    }

    @WorkerThread
    fun addSms(mobile: String): ApiResponse {
        if (!isMobileNumberValid(mobile)) {
            return ApiResponse(
                status = ResponseStatus.ERROR,
                message = "Mobile number is not valid : $mobile"
            )
        }
        return SuprSendInternal.trackOperator(
            operator = SSConstants.APPEND,
            properties = JSONObject().apply {
                put(SSConstants.SMS, mobile)
            },
            ignoreFilter = true
        )
    }

    fun addSmsAsync(mobile: String, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = addSms(mobile)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute addSms", exception = e))
            }
        }
    }

    @WorkerThread
    fun removeSms(mobile: String): ApiResponse {
        if (!isMobileNumberValid(mobile)) {
            return ApiResponse(
                status = ResponseStatus.ERROR,
                message = "Mobile number is not valid : $mobile"
            )
        }
        return SuprSendInternal.trackOperator(
            operator = SSConstants.REMOVE,
            properties = JSONObject().apply {
                put(SSConstants.SMS, mobile)
            },
            ignoreFilter = true
        )
    }

    fun removeSmsAsync(mobile: String, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = removeSms(mobile)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute removeSms", exception = e))
            }
        }
    }

    @WorkerThread
    fun addWhatsapp(mobile: String): ApiResponse {
        if (!isMobileNumberValid(mobile)) {
            return ApiResponse(
                status = ResponseStatus.ERROR,
                message = "Mobile number is not valid : $mobile"
            )
        }
        return SuprSendInternal.trackOperator(
            operator = SSConstants.APPEND,
            properties = JSONObject().apply {
                put(SSConstants.WHATS_APP, mobile)
            },
            ignoreFilter = true
        )
    }

    fun addWhatsappAsync(mobile: String, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = addWhatsapp(mobile)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute addWhatsapp", exception = e))
            }
        }
    }

    @WorkerThread
    fun removeWhatsapp(mobile: String): ApiResponse {
        if (!isMobileNumberValid(mobile)) {
            return ApiResponse(
                status = ResponseStatus.ERROR,
                message = "Mobile number is not valid : $mobile"
            )
        }
        return SuprSendInternal.trackOperator(
            operator = SSConstants.REMOVE,
            properties = JSONObject().apply {
                put(SSConstants.WHATS_APP, mobile)
            },
            ignoreFilter = true
        )
    }

    fun removeWhatsappAsync(mobile: String, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = removeWhatsapp(mobile)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute removeWhatsapp", exception = e))
            }
        }
    }

    @WorkerThread
    fun addSlack(properties: JSONObject): ApiResponse {
        return SuprSendInternal.trackOperator(
            operator = SSConstants.APPEND,
            properties = JSONObject().apply {
                put(SSConstants.SLACK, properties)
            },
            ignoreFilter = true
        )
    }

    fun addSlackAsync(properties: JSONObject, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = addSlack(properties)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute addSlack", exception = e))
            }
        }
    }

    @WorkerThread
    fun removeSlack(properties: JSONObject): ApiResponse {
        return SuprSendInternal.trackOperator(
            operator = SSConstants.REMOVE,
            properties = JSONObject().apply {
                put(SSConstants.SLACK, properties)
            },
            ignoreFilter = true
        )
    }

    fun removeSlackAsync(properties: JSONObject, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = removeSlack(properties)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute removeSlack", exception = e))
            }
        }
    }

    @WorkerThread
    fun addMSTeams(properties: JSONObject): ApiResponse {
        return SuprSendInternal.trackOperator(
            operator = SSConstants.APPEND,
            properties = JSONObject().apply {
                put(SSConstants.MS_TEAMS, properties)
            },
            ignoreFilter = true
        )
    }

    fun addMSTeamsAsync(properties: JSONObject, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = addMSTeams(properties)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute addMSTeams", exception = e))
            }
        }
    }

    @WorkerThread
    fun removeMSTeams(properties: JSONObject): ApiResponse {
        return SuprSendInternal.trackOperator(
            operator = SSConstants.REMOVE,
            properties = JSONObject().apply {
                put(SSConstants.MS_TEAMS, properties)
            },
            ignoreFilter = true
        )
    }

    fun removeMSTeamsAsync(properties: JSONObject, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = removeMSTeams(properties)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute removeMSTeamsAsync", exception = e))
            }
        }
    }

    @WorkerThread
    fun setAndroidFcmPush(token: String): ApiResponse {
        val oldToken = LocalStorage.getValue(SSConstants.CONFIG_FCM_PUSH_TOKEN)
        LocalStorage.setValue(SSConstants.CONFIG_FCM_PUSH_TOKEN, token)
        if (oldToken != token) {
            LocalStorage.setValue(SSConstants.CONFIG_FCM_TOKEN_SYNC_STATUS, "false")
        } else {
            val syncStatus = (LocalStorage.getValue(SSConstants.CONFIG_FCM_TOKEN_SYNC_STATUS) ?: "false").toBoolean()
            if (syncStatus) {
                return ApiResponse(status = ResponseStatus.SUCCESS, statusCode = 200, message = "FCM token is already sync")
            }
        }
        val jsonObject = JSONObject()
        jsonObject.put(SSConstants.PUSH_ANDROID_TOKEN, token)
        jsonObject.put(SSConstants.ID_PROVIDER, SSConstants.PUSH_VENDOR_FCM)
        jsonObject.put(SSConstants.DEVICE_ID, DeviceInfo.getDeviceId())

        val actionStatus = SuprSendInternal.trackOperator(
            operator = SSConstants.APPEND,
            properties = jsonObject,
            ignoreFilter = true
        )
        if (actionStatus.isSuccess()) {
            LocalStorage.setValue(SSConstants.CONFIG_FCM_TOKEN_SYNC_STATUS, "true")
        }
        return actionStatus
    }

    fun setAndroidFcmPushAsync(token: String, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = setAndroidFcmPush(token)
                SuprSendInternal.context.runOnUIThread {
                    actionStatusCallback?.onComplete(actionStatus)
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute setAndroidFcmPushAsync", exception = e))
            }
        }
    }
}