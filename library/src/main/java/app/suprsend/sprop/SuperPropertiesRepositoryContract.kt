package app.suprsend.sprop

import org.json.JSONObject


internal interface SuperPropertiesRepositoryContract {
    fun add(key: String, value: Any)
    fun add(properties: JSONObject)
    fun remove(key: String)
    fun getAll(): JSONObject
}
