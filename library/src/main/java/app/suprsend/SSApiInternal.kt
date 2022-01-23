package app.suprsend

import app.suprsend.base.Logger
import app.suprsend.base.SSConstants
import app.suprsend.base.SdkAndroidCreator
import app.suprsend.base.filterSSReservedKeys
import app.suprsend.base.flushExecutorService
import app.suprsend.base.size

import app.suprsend.base.uuid
import app.suprsend.config.ConfigHelper
import app.suprsend.event.EventFlushHandler
import app.suprsend.event.PayloadCreator
import app.suprsend.sprop.SuperPropertiesLocalDataSource
import app.suprsend.user.UserLocalDatasource
import app.suprsend.user.api.SSInternalUser
import org.json.JSONObject

internal object SSApiInternal {

    var isFlushing = false

    val userLocalDatasource = UserLocalDatasource()

    fun identify(uniqueId: String) {
        SdkAndroidCreator
            .eventLocalDatasource
            .track(
                body = PayloadCreator
                    .buildIdentityEventPayload(
                        identifiedId = uniqueId,
                        anonymousId = userLocalDatasource.getIdentity()
                    ).toString(),
                isDirty = true
            )
        userLocalDatasource.identify(uniqueId)
        appendNotificationToken()
        saveTrackEventPayload(SSConstants.S_EVENT_USER_LOGIN)
    }

    fun setSuperProperty(key: String, value: Any) {
        Logger.i(TAG, "Setting super properties $key $value")
        val superPropertiesRepository = SuperPropertiesLocalDataSource()
        superPropertiesRepository.add(key, value)
    }

    fun setSuperProperties(properties: JSONObject) {
        Logger.i(TAG, "Setting super properties $properties")
        val superPropertiesRepository = SuperPropertiesLocalDataSource()
        superPropertiesRepository.add(properties)
    }

    fun removeSuperProperty(key: String) {
        Logger.i(TAG, "Remove super properties $key")
        val superPropertiesRepository = SuperPropertiesLocalDataSource()
        superPropertiesRepository.remove(key)
    }

    fun track(eventName: String, properties: JSONObject? = null) {
        val filteredJson = properties?.filterSSReservedKeys() ?: properties
        if (properties != null && filteredJson.size() == 0) {
            return
        }
        saveTrackEventPayload(eventName, filteredJson)
    }

    fun purchaseMade(properties: JSONObject) {
        saveTrackEventPayload(eventName = SSConstants.S_EVENT_PURCHASE_MADE, propertiesJO = properties)
    }

    //Todo : Need clarity cover test cases
    fun notificationSubscribed() {
        saveTrackEventPayload(eventName = SSConstants.S_EVENT_NOTIFICATION_SUBSCRIBED)
    }

    //Todo : Need clarity cover test cases
    fun notificationUnSubscribed() {
        saveTrackEventPayload(eventName = SSConstants.S_EVENT_NOTIFICATION_UNSUBSCRIBED)
    }

    //Todo : Need clarity cover test cases
    fun pageVisited() {
        saveTrackEventPayload(eventName = SSConstants.S_EVENT_PAGE_VISITED)
    }

    fun flush() {
        if (isFlushing) {
            Logger.i(EventFlushHandler.TAG, "Flush request is ignored as flush is already in progress")
            return
        }

        Logger.i(EventFlushHandler.TAG, "Trying to flush events")

        isFlushing = true

        flushExecutorService.execute {
            Logger.i(EventFlushHandler.TAG, "Flush event started")
            EventFlushHandler.flushEvents()
            isFlushing = false
            Logger.i(EventFlushHandler.TAG, "Flush event completed")
        }
    }

    fun reset() {
        val newID = uuid()
        val userId = userLocalDatasource.getIdentity()
        Logger.i(TAG, "reset : Current : $userId New : $newID")
        saveTrackEventPayload(SSConstants.S_EVENT_USER_LOGOUT)
        userLocalDatasource.identify(newID)
        appendNotificationToken()
    }

    fun saveTrackEventPayload(eventName: String, propertiesJO: JSONObject? = null) {
        if (eventName.isBlank()) {
            Logger.i(TAG, "event name cannot be blank")
            return
        }

        val superPropertiesLocalDataSource = SuperPropertiesLocalDataSource()
        SdkAndroidCreator
            .eventLocalDatasource
            .track(
                body = PayloadCreator.buildTrackEventPayload(
                    eventName = eventName,
                    distinctId = userLocalDatasource.getIdentity(),
                    superProperties = superPropertiesLocalDataSource.getAll(),
                    defaultProperties = SdkAndroidCreator.deviceInfo.getDeviceInfoProperties(),
                    userProperties = propertiesJO
                ).toString(),
                isDirty = true
            )

    }


    private fun appendNotificationToken() {
        val fcmToken = getFcmToken()
        if (fcmToken.isNotBlank()) {
            val jsonObject = JSONObject()
            jsonObject.put(SSConstants.PUSH_ANDROID_TOKEN, fcmToken)
            jsonObject.put(SSConstants.PUSH_VENDOR, SSConstants.PUSH_VENDOR_FCM)
            jsonObject.put(SSConstants.DEVICE_ID, getDeviceID())
            SSInternalUser.storeOperatorPayload(properties = jsonObject, operator = SSConstants.APPEND)
        }

        val xiaomiToken = getXiaomiToken()
        if (xiaomiToken.isNotBlank()) {
            val jsonObject = JSONObject()
            jsonObject.put(SSConstants.PUSH_ANDROID_TOKEN, xiaomiToken)
            jsonObject.put(SSConstants.PUSH_VENDOR, SSConstants.PUSH_VENDOR_XIAOMI)
            jsonObject.put(SSConstants.DEVICE_ID, getDeviceID())
            SSInternalUser.storeOperatorPayload(properties = jsonObject, operator = SSConstants.APPEND)
        }
    }

    fun isAppInstalled(): Boolean {
        return ConfigHelper.getBoolean(SSConstants.CONFIG_IS_APP_LAUNCHED) ?: false
    }

    /**
     * Is required to invalidated previous push notification token for same device id
     */
    fun setDeviceId(deviceId: String) {
        ConfigHelper.addOrUpdate(SSConstants.CONFIG_DEVICE_ID, deviceId)
    }

    fun getDeviceID(): String {
        return ConfigHelper.get(SSConstants.CONFIG_DEVICE_ID) ?: ""
    }

    fun setFcmToken(token: String) {
        ConfigHelper.addOrUpdate(SSConstants.CONFIG_FCM_PUSH_TOKEN, token)
    }

    fun getFcmToken(): String {
        return ConfigHelper.get(SSConstants.CONFIG_FCM_PUSH_TOKEN) ?: ""
    }

    fun setXiaomiToken(token: String) {
        ConfigHelper.addOrUpdate(SSConstants.CONFIG_XIAOMI_PUSH_TOKEN, token)
    }

    fun getXiaomiToken(): String {
        return ConfigHelper.get(SSConstants.CONFIG_XIAOMI_PUSH_TOKEN) ?: ""
    }

    fun setOppoRegisterId(registerId: String) {
        ConfigHelper.addOrUpdate(SSConstants.CONFIG_OPPO_PUSH_REGISTER_ID, registerId)
    }

    fun getOppoRegisterId(): String {
        return ConfigHelper.get(SSConstants.CONFIG_OPPO_PUSH_REGISTER_ID) ?: ""
    }

    fun setAppLaunched() {
        ConfigHelper.addOrUpdate(SSConstants.CONFIG_IS_APP_LAUNCHED, true)
    }

    fun getCachedApiKey(): String {
        return ConfigHelper.get(SSConstants.CONFIG_API_KEY) ?: ""
    }

    const val TAG = "ssinternal"
}