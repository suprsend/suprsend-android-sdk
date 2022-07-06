package app.suprsend.config

import app.suprsend.base.SdkAndroidCreator

import app.suprsend.database.Config_Model


/**
 * This will only save key and value(string)
 */
internal object ConfigHelper {

    private var configChangeMap = hashMapOf<String, ConfigListener>()

    fun addOrUpdate(key: String, value: String) {
        SdkAndroidCreator.sqlDataHelper.insert_configByKey(Config_Model(key = key, value = value))
        configChangeMap[key]?.onChange()
    }

    fun get(key: String): String? {
        return SdkAndroidCreator.sqlDataHelper.getconfigByKey(key)?.value
    }

    fun addOrUpdate(key: String, value: Int) {
        SdkAndroidCreator.sqlDataHelper.insert_configByKey(Config_Model(key = key, value = value.toString()))
        configChangeMap[key]?.onChange()
    }

    fun getInt(key: String): Int? {
        return SdkAndroidCreator.sqlDataHelper.getconfigByKey(key)?.value?.toInt()
    }

    fun addOrUpdate(key: String, value: Boolean) {
        SdkAndroidCreator.sqlDataHelper.insert_configByKey(Config_Model(key = key, value = getBooleanToString(value)))
        configChangeMap[key]?.onChange()
    }


    fun getBoolean(key: String): Boolean? {
        return getStringToBoolean(SdkAndroidCreator.sqlDataHelper.getconfigByKey(key)?.value)
    }

    fun setChangeListener(key: String, configListener: ConfigListener) {
        configChangeMap[key] = configListener
    }

    fun removeListener(key: String) {
        configChangeMap.remove(key)
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
