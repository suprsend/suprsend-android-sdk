package app.suprsend.database;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import app.suprsend.base.Logger;

public class SQLDataHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "suprsend.db";
    private static final int DATABASE_VERSION = 1;

    // Table : events
    private static final String TABLENAME_events = "events";
    private static final String events_Id = "Id";
    private static final String events_Value = "value";
    private static final String events_IsDirty = "isDirty";
    private static final String events_TimeStamp = "timeStamp";
    private static final String events_Uuid = "uuid";

    // Table : config
    private static final String TABLENAME_config = "config";
    private static final String config_Id = "id";
    private static final String config_Key = "key_name";
    private static final String config_Value = "value";

    private final SQLiteDatabase db;

    private static final String TAG = "database_log";

    public SQLDataHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query1 = "CREATE TABLE " + TABLENAME_events + " (" + events_Id + " INTEGER PRIMARY KEY AUTOINCREMENT," + events_Value + " TEXT ," + events_IsDirty + " INTEGER ," + events_TimeStamp + " INTEGER ," + events_Uuid + " TEXT " + ");";
        String query2 = "CREATE TABLE " + TABLENAME_config + " (" + config_Id + " INTEGER PRIMARY KEY AUTOINCREMENT," + config_Key + " TEXT ," + config_Value + " TEXT " + ");";
        try {
            db.execSQL(query1);
            db.execSQL(query2);
        } catch (Exception e) {
            Logger.INSTANCE.e("db","onCreate",e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLENAME_events);
            db.execSQL("DROP TABLE IF EXISTS " + TABLENAME_config);
        } catch (Exception e) {
            Logger.INSTANCE.e("db","onUpgrade",e);
        }
        onCreate(db);
    }

    public void insert_events(Event_Model table_model_obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(events_Value, table_model_obj.getValue());
        contentValues.put(events_IsDirty, table_model_obj.isDirty());
        contentValues.put(events_TimeStamp, table_model_obj.getTimeStamp());
        contentValues.put(events_Uuid, table_model_obj.getUuid());
        try {
            db.insert(TABLENAME_events, null, contentValues);
        } catch (Exception e) {
            Logger.INSTANCE.e("db","insert_events",e);
        }
    }

    public ArrayList<Event_Model> geteventsList(int isDirty, long limit) {
        ArrayList<Event_Model> list = new ArrayList<Event_Model>();
        Cursor c = null;
        try {
            String query = "select * from " + TABLENAME_events + " where " + events_IsDirty + " = " + isDirty + " ORDER BY " + events_TimeStamp + " LIMIT " + limit;
            c = db.rawQuery(query, null);
            if (c != null && c.moveToNext()) {
                do {
                    Event_Model log = new Event_Model();
                    log.setId(c.getLong(c.getColumnIndexOrThrow(events_Id)));
                    log.setValue(c.getString(c.getColumnIndexOrThrow(events_Value)));
                    log.setDirty(c.getInt(c.getColumnIndexOrThrow(events_IsDirty)));
                    log.setTimeStamp(c.getInt(c.getColumnIndexOrThrow(events_TimeStamp)));
                    list.add(log);
                } while (c.moveToNext());
            }
            c.close();
        } catch (Exception e) {
            Logger.INSTANCE.e("db","geteventsList",e);
        } finally {
            if (c != null)
                c.close();
        }
        return list;
    }

    private void insert_config(Config_Model table_model_obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(config_Key, table_model_obj.getKey());
        contentValues.put(config_Value, table_model_obj.getValue());
        try {
            db.insert(TABLENAME_config, null, contentValues);
        } catch (Exception e) {
            Logger.INSTANCE.e("db","insert_config",e);
        }
    }

    public void insert_configByKey(Config_Model table_model_obj) {
        Cursor c = null;
        try {
            String q1 = " SELECT * FROM " + TABLENAME_config + " where " + config_Key + " ='" + table_model_obj.getKey() + "'";

            c = db.rawQuery(q1, null);
            if (c != null && c.getCount() > 0) {
                if (c.moveToFirst()) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(config_Value, table_model_obj.getValue());
                    try {
                        db.update(TABLENAME_config, contentValues, config_Key + "= ? ", new String[]{table_model_obj.getKey()});
                    } catch (Exception e) {
                        Logger.INSTANCE.e("db","insert_configByKey",e);
                    }
                }
            } else {
                insert_config(table_model_obj);
            }
            c.close();
        } catch (Exception e) {
            Logger.INSTANCE.e("db","insert_configByKey",e);
        } finally {
            if (c != null)
                c.close();
        }
    }

    public void deleteeventsByID(String ids) {
        try {
            db.execSQL("DELETE FROM " + TABLENAME_events + " WHERE " + events_Id + " IN (" + ids + ")");
        } catch (Exception e) {
            Logger.INSTANCE.e("db","deleteeventsByID",e);
        }
    }

    public void deleteAllEvents() {
        try {
            db.execSQL("DELETE FROM " + TABLENAME_events);
        } catch (Exception e) {
            Logger.INSTANCE.e("db","deleteAllEvents",e);
        }
    }

    //Starts Config table
    public Config_Model getconfigByKey(String key) {
        Config_Model log = new Config_Model();
        Cursor c = null;
        try {
            String query = "select * from " + TABLENAME_config + " where " + config_Key + " ='" + key + "'";
            c = db.rawQuery(query, null);
            if (c != null && c.moveToNext()) {
                do {
                    log.setId(c.getInt(c.getColumnIndexOrThrow(config_Id)));
                    log.setKey(c.getString(c.getColumnIndexOrThrow(config_Key)));
                    log.setValue(c.getString(c.getColumnIndexOrThrow(config_Value)));

                } while (c.moveToNext());
            }
            c.close();
        } catch (Exception e) {
            Logger.INSTANCE.e("db","getconfigByKey",e);
        } finally {
            if (c != null)
                c.close();
        }
        return log;
    }

    public void deleteAllConfigs() {
        try {
            db.execSQL("DELETE FROM " + TABLENAME_config);
        } catch (Exception e) {
            Logger.INSTANCE.e("db","deleteAllConfigs",e);
        }
    }
}