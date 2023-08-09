package app.suprsend.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import app.suprsend.base.Logger.e

internal class SQLDataHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private val db: SQLiteDatabase = writableDatabase

    override fun onCreate(db: SQLiteDatabase) {
        try {
            val query1 = "CREATE TABLE $TABLENAME_events ($events_Id INTEGER PRIMARY KEY AUTOINCREMENT,$events_Value TEXT ,$events_IsDirty INTEGER ,$events_TimeStamp INTEGER ,$events_Uuid TEXT );"
            val query2 = "CREATE TABLE $TABLENAME_config ($config_Id INTEGER PRIMARY KEY AUTOINCREMENT,$config_Key TEXT ,$config_Value TEXT );"
            db.execSQL(query1)
            db.execSQL(query2)
        } catch (e: Exception) {
            e("db", "onCreate", e)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            try {
                db.execSQL("DROP TABLE IF EXISTS $TABLENAME_events")
                db.execSQL("DROP TABLE IF EXISTS $TABLENAME_config")
            } catch (e: Exception) {
                e("db", "onUpgrade", e)
            }
            onCreate(db)
        } catch (e: Exception) {
            e("db", "onUpgrade", e)
        }
    }

    fun insertEvents(table_model_obj: Event_Model) {
        try {
            val contentValues = ContentValues()
            contentValues.put(events_Value, table_model_obj.value)
            contentValues.put(events_IsDirty, table_model_obj.isDirty)
            contentValues.put(events_TimeStamp, table_model_obj.timeStamp)
            contentValues.put(events_Uuid, table_model_obj.uuid)
            db.insert(TABLENAME_events, null, contentValues)
        } catch (e: Exception) {
            e("db", "insertEvents", e)
        }
    }

    fun getEventsList(isDirty: Int, limit: Long): ArrayList<Event_Model> {
        val list = ArrayList<Event_Model>()
        var cursor: Cursor? = null
        try {
            val query = "select * from $TABLENAME_events where $events_IsDirty = $isDirty ORDER BY $events_TimeStamp LIMIT $limit"
            cursor = db.rawQuery(query, null)
            if (cursor != null && cursor.moveToNext()) {
                do {
                    val log = Event_Model()
                    log.id = cursor.getLong(cursor.getColumnIndexOrThrow(events_Id))
                    log.value = cursor.getString(cursor.getColumnIndexOrThrow(events_Value))
                    log.isDirty = cursor.getInt(cursor.getColumnIndexOrThrow(events_IsDirty))
                    log.timeStamp = cursor.getInt(cursor.getColumnIndexOrThrow(events_TimeStamp)).toLong()
                    list.add(log)
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            e("db", "getEventsList", e)
        } finally {
            cursor?.close()
        }
        return list
    }

    private fun insertConfig(table_model_obj: Config_Model) {
        val contentValues = ContentValues()
        contentValues.put(config_Key, table_model_obj.key)
        contentValues.put(config_Value, table_model_obj.value)
        try {
            db.insert(TABLENAME_config, null, contentValues)
        } catch (e: Exception) {
            e("db", "insertConfig", e)
        }
    }

    fun insertConfigByKey(table_model_obj: Config_Model) {
        var cursor: Cursor? = null
        try {
            val q1 = " SELECT * FROM " + TABLENAME_config + " where " + config_Key + " ='" + table_model_obj.key + "'"
            cursor = db.rawQuery(q1, null)
            if (cursor != null && cursor.count > 0) {
                if (cursor.moveToFirst()) {
                    val contentValues = ContentValues()
                    contentValues.put(config_Value, table_model_obj.value)
                    try {
                        db.update(TABLENAME_config, contentValues, "$config_Key= ? ", arrayOf(table_model_obj.key))
                    } catch (e: Exception) {
                        e("db", "insertConfigByKey", e)
                    }
                }
            } else {
                insertConfig(table_model_obj)
            }
            cursor.close()
        } catch (e: Exception) {
            e("db", "insertConfigByKey", e)
        } finally {
            cursor?.close()
        }
    }

    fun deleteEventsByID(ids: String) {
        try {
            db.execSQL("DELETE FROM $TABLENAME_events WHERE $events_Id IN ($ids)")
        } catch (e: Exception) {
            e("db", "deleteEventsByID", e)
        }
    }

    fun deleteAllEvents() {
        try {
            db.execSQL("DELETE FROM $TABLENAME_events")
        } catch (e: Exception) {
            e("db", "deleteAllEvents", e)
        }
    }

    //Starts Config table
    fun getConfigByKey(key: String): Config_Model {
        val log = Config_Model()
        var cursor: Cursor? = null
        try {
            val query = "select * from $TABLENAME_config where $config_Key ='$key'"
            cursor = db.rawQuery(query, null)
            if (cursor != null && cursor.moveToNext()) {
                do {
                    log.id = cursor.getInt(cursor.getColumnIndexOrThrow(config_Id))
                    log.key = cursor.getString(cursor.getColumnIndexOrThrow(config_Key))
                    log.value = cursor.getString(cursor.getColumnIndexOrThrow(config_Value))
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            e("db", "getConfigByKey", e)
        } finally {
            cursor?.close()
        }
        return log
    }

    fun deleteAllConfigs() {
        try {
            db.execSQL("DELETE FROM $TABLENAME_config")
        } catch (e: Exception) {
            e("db", "deleteAllConfigs", e)
        }
    }

    companion object {
        private const val DATABASE_NAME = "suprsend.db"
        private const val DATABASE_VERSION = 1

        // Table : events
        private const val TABLENAME_events = "events"
        private const val events_Id = "Id"
        private const val events_Value = "value"
        private const val events_IsDirty = "isDirty"
        private const val events_TimeStamp = "timeStamp"
        private const val events_Uuid = "uuid"

        // Table : config
        private const val TABLENAME_config = "config"
        private const val config_Id = "id"
        private const val config_Key = "key_name"
        private const val config_Value = "value"
    }
}