package app.suprsend.sprop

import app.suprsend.base.addUpdateJsoObject
import app.suprsend.base.toKotlinJsonObject
import app.suprsend.config.ConfigHelper
import org.json.JSONObject

internal class SuperPropertiesLocalDataSource : SuperPropertiesRepositoryContract {

    override fun add(key: String, value: Any) {
        val updatedJsonObject = getAll()
        updatedJsonObject.put(key,value)
        saveValues(updatedJsonObject)
    }

    override fun add(properties: JSONObject) {
        val updatedProperties = getAll().addUpdateJsoObject(properties)
        saveValues(updatedProperties as JSONObject)
    }

    override fun remove(key: String) {
        val allProp = getAll()
        allProp.remove(key)
        saveValues(allProp)
    }

    override fun removeAll() {
        saveValues(JSONObject())
    }

    override fun getAll(): JSONObject {
        val superPropertiesJsonString = ConfigHelper.get(CONFIG_KEY)
        return superPropertiesJsonString.toKotlinJsonObject()
    }

    private fun saveValues(updatedJsonObject: JSONObject) {
        ConfigHelper.addOrUpdate(CONFIG_KEY, updatedJsonObject.toString())
    }

    companion object {
        const val CONFIG_KEY = "super_properties"
    }
}
