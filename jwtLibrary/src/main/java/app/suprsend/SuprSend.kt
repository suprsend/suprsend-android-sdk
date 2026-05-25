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
import app.suprsend.notification.NotificationActionType
import app.suprsend.notification.NotificationActionVo
import app.suprsend.user.User
import app.suprsend.utils.runOnUIThread
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

class SuprSend private constructor() {

    val user = User()

    init {
        if (FirebaseApp.getApps(SSInternal.context).size > 0) {
            FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener(
                OnCompleteListener<InstanceIdResult> { task ->
                    if (!task.isSuccessful) {
                        Logger.i(SSConstants.TAG_SUPRSEND, "Fetching FCM registration token failed")
                        return@OnCompleteListener
                    }
                    val token = task.result?.token
                    if (!token.isNullOrBlank()) {
                        user.setAndroidFcmPushAsync(token)
                    }
                }
            )
        }
    }

    @WorkerThread
    fun identify(distinctId: String): ApiResponse {
        return SSInternal.identity(
            distinctId = distinctId
        )
    }

    fun identityAsync(distinctId: String, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = identify(distinctId)
                actionStatusCallback?.let {
                    SSInternal.context.runOnUIThread { it.onComplete(actionStatus) }
                }
            } catch (e: Exception) {
                actionStatusCallback?.onComplete(ApiResponse(status = ResponseStatus.ERROR, message = "Failed to execute identityAsync", exception = e))
            }
        }
    }

    fun isIdentified(): Boolean {
        return if (SSInternal.suprSendData.userTokenFetcher == null) {
            !SSInternal.suprSendData.distinctId.isNullOrBlank()
        } else {
            !SSInternal.suprSendData.distinctId.isNullOrBlank() && !SSInternal.getToken().isNullOrBlank()
        }
    }

    fun getDistinctId(): String? {
        return SSInternal.suprSendData.distinctId
    }

    @WorkerThread
    fun trackEvent(eventName: String): ApiResponse {
        return SSInternal.trackEvent(
            eventName = eventName
        )
    }

    fun trackEventAsync(eventName: String, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = trackEvent(eventName)
                actionStatusCallback?.let {
                    SSInternal.context.runOnUIThread {
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
        return SSInternal.trackEvent(
            eventName = eventName,
            properties = properties
        )
    }

    fun trackEventAsync(eventName: String, properties: JSONObject, actionStatusCallback: ActionStatusCallback? = null) {
        sdkExecutorService.execute {
            try {
                val actionStatus = trackEvent(eventName, properties)
                actionStatusCallback?.let {
                    SSInternal.context.runOnUIThread {
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
        SSInternal.reset(unSubscribeNotification)
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

    fun notificationClicked(notificationActionVo: NotificationActionVo) {
        trackEventAsync(
            eventName = SSConstants.S_EVENT_NOTIFICATION_CLICKED,
            properties = JSONObject().apply {
                put("id", notificationActionVo.notificationId)
                if (notificationActionVo.notificationActionType == NotificationActionType.BUTTON) {
                    put("label_id", notificationActionVo.id)
                }
            }
        )
    }

    fun setLogLevel(level: LogLevel) {
        Logger.logLevel = level
    }


    companion object {
        @Volatile
        private var suprsend: SuprSend? = null
        fun getInstance(): SuprSend {
            if (!SSInternal.isSuprSendDataInitialized()) {
                throw IllegalStateException("Suprsend SDK is not initialized. Please use Suprsend.initialize() method to initialize.")
            }
            return suprsend ?: synchronized(this) {
                suprsend ?: SuprSend().also { suprsend = it }
            }
        }

        fun initialize(context: Context, publicApiKey: String, baseUrl: String) {
            SSInternal.context = context.applicationContext
            SSInternal.suprSendData.publicApiKey = publicApiKey
            SSInternal.suprSendData.distinctId = LocalStorage.getValue(SSConstants.CONFIG_DISTINCT_ID)
            SSInternal.suprSendData.baseUrl = baseUrl
        }

        fun setInboxBaseUrl(inboxBaseUrl: String) {
            SSInternal.suprSendData.inboxBaseUrl = inboxBaseUrl
        }

        fun setUserTokenFetcher(userTokenFetcher: UserTokenFetcher?) {
            SSInternal.suprSendData.userTokenFetcher = userTokenFetcher
        }

        fun setTenantId(tenantId: String?) {
            if (tenantId.isNullOrBlank())
                SSInternal.suprSendData.tenantId = null
            else
                SSInternal.suprSendData.tenantId = tenantId
        }

        fun setLogger(loggerCallback: LoggerCallback) {
            SSInternal.loggerCallback = loggerCallback
        }

        fun setNotificationCallback(notificationCallbackListener: NotificationCallbackListener) {
            SSInternal.suprSendData.notificationCallbackListener = notificationCallbackListener
        }
    }

}