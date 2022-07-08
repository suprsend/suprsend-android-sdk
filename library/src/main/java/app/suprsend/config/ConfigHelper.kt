package app.suprsend.config

import app.suprsend.base.Logger
import app.suprsend.base.SdkAndroidCreator

import app.suprsend.database.Config_Model


/**
 * This will only save key and value(string)
 */
internal object ConfigHelper {

    private var configChangeMap = arrayListOf<ConfigListener>()

    fun addOrUpdate(key: String, value: String) {
        SdkAndroidCreator.sqlDataHelper.insert_configByKey(Config_Model(key = key, value = value))
        notifyConfig(key)
    }

    fun get(key: String): String? {
        return SdkAndroidCreator.sqlDataHelper.getconfigByKey(key)?.value
    }

    fun addOrUpdate(key: String, value: Int) {
        SdkAndroidCreator.sqlDataHelper.insert_configByKey(Config_Model(key = key, value = value.toString()))
        notifyConfig(key)
    }

    fun getInt(key: String): Int? {
        return SdkAndroidCreator.sqlDataHelper.getconfigByKey(key)?.value?.toInt()
    }

    fun addOrUpdate(key: String, value: Boolean) {
        SdkAndroidCreator.sqlDataHelper.insert_configByKey(Config_Model(key = key, value = getBooleanToString(value)))
        notifyConfig(key)
    }


    fun getBoolean(key: String): Boolean? {
        return getStringToBoolean(SdkAndroidCreator.sqlDataHelper.getconfigByKey(key)?.value)
    }

    fun setChangeListener(configListener: ConfigListener) {
        configChangeMap.add(configListener)
    }

    fun removeListener(configListener: ConfigListener) {
        configChangeMap.remove(configListener)
    }

    private fun notifyConfig(key: String) {
        try {
            configChangeMap.forEach { it.onChange(key) }
        } catch (e: Exception) {
            Logger.e("config", "", e)
        }
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
