package app.suprsend

import android.app.Application
import android.content.Context
import app.suprsend.base.BasicDetails
import app.suprsend.base.LogLevel
import app.suprsend.base.Logger
import app.suprsend.base.PeriodicFlush
import app.suprsend.base.SSConstants
import app.suprsend.base.SdkAndroidCreator
import app.suprsend.base.executorService
import app.suprsend.base.isInValidKey
import app.suprsend.base.uuid
import app.suprsend.config.ConfigHelper
import app.suprsend.user.UserLocalDatasource
import app.suprsend.user.api.UserApiInternalContract
import app.suprsend.xiaomi.SSXiaomiReceiver
import com.xiaomi.channel.commonutils.logger.LoggerInterface
import com.xiaomi.mipush.sdk.MiPushClient
import org.json.JSONObject

class SSApi
private constructor(
    apiKey: String,
    apiSecret: String,
    apiBaseUrl: String? = null, // If null data will be directed to prod server
    isFromCache: Boolean = false
) {

    private val basicDetails: BasicDetails = BasicDetails(apiKey, apiSecret, apiBaseUrl)
    private val ssUserApi: SSUserApi = SSUserApi()

    init {

        ConfigHelper.addOrUpdate(SSConstants.CONFIG_API_BASE_URL, basicDetails.getApiBaseUrl())
        ConfigHelper.addOrUpdate(SSConstants.CONFIG_API_KEY, basicDetails.apiKey)
        ConfigHelper.addOrUpdate(SSConstants.CONFIG_API_SECRET, basicDetails.apiSecret)

        // Anonymous user id generation
        val userLocalDatasource = UserLocalDatasource()
        val userId = userLocalDatasource.getIdentity()
        if (userId.isBlank()) {
            userLocalDatasource.identify(uuid())
        }

        // Device Properties
        SSApiInternal.setDeviceId(SdkAndroidCreator.deviceInfo.getDeviceId())

        if (!SSApiInternal.isAppInstalled()) {
            // App Launched
            track(SSConstants.S_EVENT_APP_INSTALLED)
            SSApiInternal.setAppLaunched()
        }

        if (!isFromCache)
            track(SSConstants.S_EVENT_APP_LAUNCHED)

        val application = SdkAndroidCreator.context.applicationContext as Application

        // Flush periodically
        PeriodicFlush.start()

        // Flush on activity lifecycle
        application.registerActivityLifecycleCallbacks(ActivityLifecycleCallbackHandler(this))

        // Flush on Exception
        // ExceptionHandler(newInstance).track()
    }

    fun identify(uniqueId: String) {
        executorService.execute {
            SSApiInternal.identify(uniqueId)
            SSApiInternal.flush()
        }
    }

    fun setSuperProperty(key: String, value: Any) {
        executorService.execute {
            SSApiInternal.setSuperProperty(key, value)
        }
    }

    fun setSuperProperties(properties: JSONObject) {
        executorService.execute {
            SSApiInternal.setSuperProperties(properties = properties)
        }
    }

    fun unSetSuperProperty(key: String) {
        executorService.execute {
            SSApiInternal.removeSuperProperty(key)
        }
    }

    fun track(eventName: String, properties: JSONObject? = null) {
        executorService.execute {
            SSApiInternal.track(eventName = eventName, properties = properties)
        }
    }

    fun purchaseMade(properties: JSONObject) {
        executorService.execute {
            SSApiInternal.purchaseMade(properties)
        }
    }

    fun getUser(): UserApiInternalContract {
        return ssUserApi
    }

    fun flush() {
        SSApiInternal.flush()
    }

    fun reset() {
        executorService.execute {
            SSApiInternal.reset()
            SSApiInternal.flush()
        }
    }

    fun setLogLevel(level: LogLevel) {
        Logger.logLevel = level
    }

    companion object {

        private val instancesMap = hashMapOf<String, SSApi>()

        /**
         * Should be called before Application super.onCreate()
         */
        fun init(context: Context) {

            // Setting android context to user everywhere
            if (!SdkAndroidCreator.isContextInitialized()) {
                SdkAndroidCreator.context = context.applicationContext
            }

        }

        fun initXiaomi(context: Context, appId: String, apiKey: String) {
            try {
                MiPushClient.registerPush(context, appId, apiKey)
                com.xiaomi.mipush.sdk.Logger.setLogger(context, object : LoggerInterface {
                    override fun setTag(tag: String?) {
                        Logger.i(SSXiaomiReceiver.TAG, "set Tag : $tag")
                    }

                    override fun log(message: String?) {
                        Logger.i(SSXiaomiReceiver.TAG, "$message")
                    }

                    override fun log(message: String?, throwable: Throwable?) {
                        Logger.e(SSXiaomiReceiver.TAG, "$message", throwable)
                    }
                })
            } catch (e: Exception) {
                Logger.e(SSXiaomiReceiver.TAG, "initXiaomi", e)
            }
        }

        fun getInstance(apiKey: String, apiSecret: String, apiBaseUrl: String? = null): SSApi {
            return getInstanceInternal(apiKey = apiKey, apiSecret = apiSecret, apiBaseUrl = apiBaseUrl)
        }

        internal fun getInstanceFromCachedApiKey(): SSApi? {
            val apiKey = ConfigHelper.get(SSConstants.CONFIG_API_KEY) ?: return null
            val secret = ConfigHelper.get(SSConstants.CONFIG_API_SECRET) ?: return null
            val apiBaseUrl = ConfigHelper.get(SSConstants.CONFIG_API_BASE_URL) ?: return null
            return getInstanceInternal(apiKey = apiKey, apiSecret = secret, apiBaseUrl = apiBaseUrl, isFromCache = true)
        }

        private fun getInstanceInternal(apiKey: String, apiSecret: String, apiBaseUrl: String? = null, isFromCache: Boolean = false): SSApi {
            val uniqueId = "$apiKey-$apiSecret"
            if (instancesMap.containsKey(uniqueId)) {
                return instancesMap[uniqueId]!!
            }
            val instance = SSApi(apiKey, apiSecret, apiBaseUrl, isFromCache)
            instancesMap[uniqueId] = instance
            return instance
        }
    }
}
