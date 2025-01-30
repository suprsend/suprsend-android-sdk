package app.suprsend

import android.content.Context
import androidx.annotation.WorkerThread
import app.suprsend.base.ActionStatusCallback
import app.suprsend.base.LocalStorage
import app.suprsend.base.SSConstants
import app.suprsend.base.sdkExecutorService
import app.suprsend.log.LogLevel
import app.suprsend.log.Logger
import app.suprsend.log.LoggerCallback
import app.suprsend.model.ApiResponse
import app.suprsend.model.ResponseStatus
import app.suprsend.model.SuprSendOptions
import app.suprsend.user.User
import app.suprsend.user.preference.SSInternalUserPreference
import app.suprsend.utils.runOnUIThread
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

class SuprSend private constructor() {

    val user = User()

    init {
        if (FirebaseApp.getApps(SuprSendInternal.context).size > 0) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Logger.i(SSConstants.TAG_SUPRSEND, "Fetching FCM registration token failed")
                    return@OnCompleteListener
                }
                val token = task.result
                if (!token.isNullOrBlank())
                    user.setAndroidFcmPushAsync(token)
            })
        }
    }

    @WorkerThread
    fun identify(distinctId: String): ApiResponse {
        return SuprSendInternal.identity(
            distinctId = distinctId
        )
    }

    fun identityAsync(distinctId: String, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = identify(distinctId)
                actionStatusCallback?.let {
                    SuprSendInternal.context.runOnUIThread { it.onComplete(actionStatus) }
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute identityAsync", exception = e))
            }
        }
    }

    fun isIdentified(): Boolean {
        return if (SuprSendInternal.suprSendData.userTokenFetcher == null) {
            !SuprSendInternal.suprSendData.distinctId.isNullOrBlank()
        } else {
            !SuprSendInternal.suprSendData.distinctId.isNullOrBlank() && !SuprSendInternal.getToken().isNullOrBlank()
        }
    }

    @WorkerThread
    fun trackEvent(eventName: String): ApiResponse {
        return SuprSendInternal.trackEvent(
            eventName = eventName
        )
    }

    fun trackEventAsync(eventName: String, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = trackEvent(eventName)
                actionStatusCallback?.let {
                    SuprSendInternal.context.runOnUIThread {
                        it.onComplete(actionStatus)
                    }
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute identityAsync", exception = e))
            }
        }
    }

    @WorkerThread
    fun trackEvent(eventName: String, properties: JSONObject): ApiResponse {
        return SuprSendInternal.trackEvent(
            eventName = eventName,
            properties = properties
        )
    }

    fun trackEventAsync(eventName: String, properties: JSONObject, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = trackEvent(eventName, properties)
                actionStatusCallback?.let {
                    SuprSendInternal.context.runOnUIThread {
                        it.onComplete(actionStatus)
                    }
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute identityAsync", exception = e))
            }
        }
    }

    @WorkerThread
    fun reset(unSubscribeNotification: Boolean) {
        if (unSubscribeNotification)
            SuprSendInternal.removeNotificationToken()
        SSInternalUserPreference.clearUserPreference()
        SuprSendInternal.suprSendData.distinctId = null
        LocalStorage.remove(SSConstants.USER_TOKEN)
        LocalStorage.remove(SSConstants.CONFIG_DISTINCT_ID)
    }

    fun resetAsync(unSubscribeNotification: Boolean, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                reset(unSubscribeNotification)
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute identityAsync", exception = e))
            }
        }
    }


    fun setLogLevel(level: LogLevel) {
        Logger.logLevel = level
    }


    companion object {
        private val instancesMap = hashMapOf<String, SuprSend>()
        fun getInstance(): SuprSend {
            val uniqueId = "only_one_instance_support"
            if (instancesMap.containsKey(uniqueId)) {
                return instancesMap[uniqueId]!!
            }
            val instance = SuprSend()
            instancesMap[uniqueId] = instance
            return instance
        }

        fun initialize(
            context: Context,
            publicApiKey: String,
            options: SuprSendOptions? = null,
            userTokenFetcher: UserTokenFetcher? = null
        ) {
            SuprSendInternal.context = context.applicationContext
            SuprSendInternal.suprSendData = SuprSendData(
                host = options?.host?.removeSuffix("/") ?: SSConstants.DEFAULT_BASE_API_URL,
                distinctId = LocalStorage.getValue(SSConstants.CONFIG_DISTINCT_ID),
                publicApiKey = publicApiKey,
                userTokenFetcher = userTokenFetcher
            )
        }

        fun setLogger(loggerCallback: LoggerCallback) {
            SuprSendInternal.loggerCallback = loggerCallback
        }

        fun setNotificationCallback(notificationCallbackListener: NotificationCallbackListener) {
            SuprSendInternal.suprSendData.notificationCallbackListener = notificationCallbackListener
        }
    }

}