package app.suprsend.config

import app.suprsend.base.SdkAndroidCreator

import app.suprsend.database.Config_Model


/**
 * This will only save key and value(string)
 */
internal object ConfigHelper {

    fun addOrUpdate(key: String, value: String) {
        SdkAndroidCreator.sqlDataHelper.insert_configByKey(Config_Model(key = key, value = value))
    }

    fun get(key: String): String? {
        return SdkAndroidCreator.sqlDataHelper.getconfigByKey(key)?.value
    }

    fun addOrUpdate(key: String, value: Boolean) {
        SdkAndroidCreator.sqlDataHelper.insert_configByKey(Config_Model(key = key, value = getBooleanToString(value)))
    }

    fun getBoolean(key: String): Boolean? {
        return getStringToBoolean(SdkAndroidCreator.sqlDataHelper.getconfigByKey(key)?.value)
    }

    private fun getStringToBoolean(value: String?): Boolean? {
        if (value == "1")
            return true
        return false
    }

    private fun getBooleanToString(value: Boolean): String {
        return if (value) "1" else "0"
    }
}
