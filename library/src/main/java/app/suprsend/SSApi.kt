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
import app.suprsend.base.filterSSReservedKeys
import app.suprsend.base.uuid
import app.suprsend.config.ConfigHelper
import app.suprsend.user.UserLocalDatasource
import app.suprsend.user.api.UserApiInternalContract
import app.suprsend.xiaomi.SSXiaomiReceiver
import com.xiaomi.channel.commonutils.logger.LoggerInterface
import com.xiaomi.mipush.sdk.Logger as XiaomiLogger
import com.xiaomi.mipush.sdk.MiPushClient
import org.json.JSONObject

class SSApi
private constructor() {

    private val ssUserApi: SSUserApi = SSUserApi()

    init {

        val application = SdkAndroidCreator.context.applicationContext as Application

        // Flush periodically
        PeriodicFlush.start()

        // Flush on activity lifecycle
        application.registerActivityLifecycleCallbacks(ActivityLifecycleCallbackHandler())

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
            SSApiInternal.setSuperProperties(properties = properties.filterSSReservedKeys())
        }
    }

    fun unSetSuperProperty(key: String) {
        executorService.execute {
            SSApiInternal.removeSuperProperty(key)
        }
    }

    fun track(eventName: String, properties: JSONObject) {
        executorService.execute {
            SSApiInternal.track(eventName = eventName, properties = properties)
        }
    }

    fun track(eventName: String) {
        executorService.execute {
            SSApiInternal.track(eventName = eventName, properties = null)
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
        fun init(context: Context, apiKey: String, apiSecret: String) {
            init(context, apiKey, apiSecret, null)
        }

        /**
         * Should be called before Application super.onCreate()
         */
        fun init(context: Context, apiKey: String, apiSecret: String, apiBaseUrl: String? = null) {

            // Setting android context to user everywhere
            if (SdkAndroidCreator.isContextInitialized()) {
                return
            }
            SdkAndroidCreator.context = context.applicationContext
            val basicDetails = BasicDetails(apiKey, apiSecret, apiBaseUrl)

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
                SSApiInternal.saveTrackEventPayload(SSConstants.S_EVENT_APP_INSTALLED)
                SSApiInternal.setAppInstalled()
            }

            /**
             * Due to xiaomi integration on non xiaomi device application create is getting called twice so i have to add
             * this below check to ignore app launch time if time is less than 1000ms
             * manifest - android:process=":pushservice" - This is leading to creation of MyApplication twice on non xiaomi device
             */
            val currentTime = System.currentTimeMillis()
            val isLaunched = (currentTime - SSApiInternal.getAppLaunchTime()) > 1000
            if (isLaunched) {
                SSApiInternal.saveTrackEventPayload(SSConstants.S_EVENT_APP_LAUNCHED)
                SSApiInternal.setAppLaunchTime(currentTime)
            }
        }

        fun initXiaomi(context: Context, appId: String, apiKey: String) {
            try {
                MiPushClient.registerPush(context, appId, apiKey)
                XiaomiLogger.setLogger(context, object : LoggerInterface {
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

        fun getInstance(): SSApi {
            return getInstanceInternal()
        }

        internal fun getInstanceFromCachedApiKey(): SSApi {
            return getInstanceInternal()
        }

        private fun getInstanceInternal(): SSApi {
            val uniqueId = "only_one_instance_support"
            if (instancesMap.containsKey(uniqueId)) {
                return instancesMap[uniqueId]!!
            }
            val instance = SSApi()
            instancesMap[uniqueId] = instance
            return instance
        }
    }
}
