package app.suprsend.user.api

import app.suprsend.SSApiInternal
import app.suprsend.base.Logger
import app.suprsend.base.SSConstants
import app.suprsend.base.SdkAndroidCreator
import app.suprsend.base.filterSSReservedKeys
import app.suprsend.base.isInValidKey
import app.suprsend.base.isMobileNumberValid
import app.suprsend.base.isValidEmail
import app.suprsend.base.size
import app.suprsend.event.PayloadCreator
import app.suprsend.user.UserLocalDatasource
import org.json.JSONArray
import org.json.JSONObject

internal object SSInternalUser {
    val userLocalDatasource = UserLocalDatasource()

    fun set(key: String, value: Any) {
        filterAndStoreOperatorPayload(
            properties = JSONObject().apply { put(key, value) },
            operator = SSConstants.SET
        )
    }

    fun set(properties: JSONObject) {
        filterAndStoreOperatorPayload(
            properties = properties,
            operator = SSConstants.SET
        )
    }

    fun setOnce(key: String, value: Any) {
        filterAndStoreOperatorPayload(
            properties = JSONObject().apply { put(key, value) },
            operator = SSConstants.SET_ONCE
        )
    }

    fun setOnce(properties: JSONObject) {
        filterAndStoreOperatorPayload(
            properties = properties,
            operator = SSConstants.SET_ONCE
        )
    }

    fun increment(key: String, value: Number) {
        filterAndStoreOperatorPayload(
            properties = JSONObject().apply { put(key, value) },
            operator = SSConstants.ADD
        )
    }

    fun increment(properties: Map<String, Number>) {
        filterAndStoreOperatorPayload(
            properties = JSONObject().apply {
                properties.forEach { (propertyKey, propertyValue) ->
                    put(propertyKey, propertyValue)
                }
            },
            operator = SSConstants.ADD
        )
    }

    fun append(key: String, value: Any) {
        filterAndStoreOperatorPayload(
            properties = JSONObject().apply { put(key, value) },
            operator = SSConstants.APPEND
        )
    }

    fun append(properties: JSONObject) {
        filterAndStoreOperatorPayload(
            properties = properties,
            operator = SSConstants.APPEND
        )
    }

    fun remove(key: String, value: Any) {
        filterAndStoreOperatorPayload(
            properties = JSONObject().apply { put(key, value) },
            operator = SSConstants.REMOVE
        )
    }

    fun remove(properties: JSONObject) {
        filterAndStoreOperatorPayload(
            properties = properties,
            operator = SSConstants.REMOVE
        )
    }

    fun unSet(key: String) {
        unSet(listOf(key))
    }

    fun unSet(keys: List<String>) {
        val filteredValidKeys = keys.filter { key -> !key.isInValidKey() }
        if (filteredValidKeys.isNotEmpty()) {
            storeOperatorPayload(
                setPropertiesArray = JSONArray().apply {
                    keys.forEach { key ->
                        put(key)
                    }
                },
                operator = SSConstants.UNSET
            )
        } else {
            Logger.i(SSConstants.TAG_VALIDATION, "Payload ignored as none keys are valid after filtering reserved keys for unset operator $keys")
        }
    }

    fun setEmail(email: String) {
        if (email.isValidEmail()) {
            storeOperatorPayload(
                properties = JSONObject().apply { put(SSConstants.EMAIL, email) },
                operator = SSConstants.APPEND
            )
        } else {
            Logger.e(TAG, "Email is not valid : $email")
        }
    }

    fun unSetEmail(email: String) {
        if (email.isValidEmail()) {
            storeOperatorPayload(
                properties = JSONObject().apply { put(SSConstants.EMAIL, email) },
                operator = SSConstants.REMOVE
            )
        } else {
            Logger.e(TAG, "Email is not valid : $email")
        }
    }

    fun setSms(mobile: String) {
        if (isMobileNumberValid(mobile)) {
            storeOperatorPayload(
                properties = JSONObject().apply { put(SSConstants.SMS, mobile) },
                operator = SSConstants.APPEND
            )
        } else {
            Logger.e(TAG, "Mobile number is not valid : $mobile")
        }
    }

    fun unSetSms(mobile: String) {
        if (isMobileNumberValid(mobile)) {
            storeOperatorPayload(
                properties = JSONObject().apply { put(SSConstants.SMS, mobile) },
                operator = SSConstants.REMOVE
            )
        } else {
            Logger.e(TAG, "Mobile number is not valid : $mobile")
        }
    }

    fun setWhatsApp(mobile: String) {
        if (isMobileNumberValid(mobile)) {
            storeOperatorPayload(
                properties = JSONObject().apply { put(SSConstants.WHATS_APP, mobile) },
                operator = SSConstants.APPEND
            )
        } else {
            Logger.e(TAG, "Mobile number is not valid : $mobile")
        }
    }

    fun unSetWhatsApp(mobile: String) {
        if (isMobileNumberValid(mobile)) {
            storeOperatorPayload(
                properties = JSONObject().apply { put(SSConstants.WHATS_APP, mobile) },
                operator = SSConstants.REMOVE
            )
        } else {
            Logger.e(TAG, "Mobile number is not valid : $mobile")
        }
    }

    fun setAndroidFcmPush(newToken: String) {
        val oldToken = SSApiInternal.getFcmToken()
        if (oldToken != newToken) {
            SSApiInternal.setFcmToken(newToken)
        }
        val jsonObject = JSONObject()
        jsonObject.put(SSConstants.PUSH_ANDROID_TOKEN, newToken)
        jsonObject.put(SSConstants.PUSH_VENDOR, SSConstants.PUSH_VENDOR_FCM)
        jsonObject.put(SSConstants.DEVICE_ID, SSApiInternal.getDeviceID())
        storeOperatorPayload(
            properties = jsonObject,
            operator = SSConstants.APPEND
        )
    }

    fun unSetAndroidFcmPush(token: String) {
        val jsonObject = JSONObject()
        jsonObject.put(SSConstants.PUSH_ANDROID_TOKEN, token)
        jsonObject.put(SSConstants.PUSH_VENDOR, SSConstants.PUSH_VENDOR_FCM)
        jsonObject.put(SSConstants.DEVICE_ID, SSApiInternal.getDeviceID())
        storeOperatorPayload(
            properties = jsonObject,
            operator = SSConstants.REMOVE
        )
    }

    fun setAndroidXiaomiPush(newToken: String) {
        val oldToken = SSApiInternal.getXiaomiToken()
        if (oldToken != newToken) {
            SSApiInternal.setXiaomiToken(newToken)
        }
        val jsonObject = JSONObject()
        jsonObject.put(SSConstants.PUSH_ANDROID_TOKEN, newToken)
        jsonObject.put(SSConstants.PUSH_VENDOR, SSConstants.PUSH_VENDOR_XIAOMI)
        jsonObject.put(SSConstants.DEVICE_ID, SSApiInternal.getDeviceID())
        storeOperatorPayload(
            properties = jsonObject,
            operator = SSConstants.APPEND
        )
    }

    fun unSetAndroidXiaomiPush(token: String) {
        val jsonObject = JSONObject()
        jsonObject.put(SSConstants.PUSH_ANDROID_TOKEN, token)
        jsonObject.put(SSConstants.PUSH_VENDOR, SSConstants.PUSH_VENDOR_XIAOMI)
        jsonObject.put(SSConstants.DEVICE_ID, SSApiInternal.getDeviceID())
        storeOperatorPayload(
            properties = jsonObject,
            operator = SSConstants.REMOVE
        )
    }

    fun notificationClicked(id: String, actionId: String? = null) {
        SSApiInternal.saveTrackEventPayload(
            eventName = SSConstants.S_EVENT_NOTIFICATION_CLICKED,
            propertiesJO = JSONObject().apply {
                put("id", id)
                if (actionId != null) {
                    put("label_id", actionId)
                }
            }
        )
    }

    fun storeOperatorPayload(properties: JSONObject? = null, operator: String, setPropertiesArray: JSONArray? = null) {

        SdkAndroidCreator
            .eventLocalDatasource
            .track(
                body = PayloadCreator
                    .buildUserOperatorPayload(
                        distinctId = userLocalDatasource.getIdentity(),
                        setProperties = properties,
                        setPropertiesArray = setPropertiesArray,
                        operator = operator
                    ).toString(),
                isDirty = true
            )
    }

    private fun filterAndStoreOperatorPayload(properties: JSONObject, operator: String) {

        val filteredProperties = properties.filterSSReservedKeys()

        if (filteredProperties.size() == 0) {
            return
        }
        storeOperatorPayload(properties = filteredProperties, operator = operator)
    }

    const val TAG = SSApiInternal.TAG
}
