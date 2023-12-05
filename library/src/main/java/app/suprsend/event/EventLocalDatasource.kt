package app.suprsend.event

import app.suprsend.base.SdkAndroidCreator
import app.suprsend.database.DBConversion
import app.suprsend.database.Event_Model

internal class EventLocalDatasource : EventDataSourceContract {

    override fun track(body: String, isDirty: Boolean) {
        SdkAndroidCreator.sqlDataHelper.insertEvents(
            Event_Model(
                value = body,
                isDirty = DBConversion.booleanToLong(isDirty),
                timeStamp = System.currentTimeMillis()
            )
        )
    }

    override fun getEvents(limit: Long, isDirty: Boolean): List<Event_Model> {
        return SdkAndroidCreator.sqlDataHelper.getEventsList(DBConversion.booleanToLong(isDirty), limit)
    }

    override fun delete(ids: String) {
        SdkAndroidCreator.sqlDataHelper.deleteEventsByID(ids)
    }
}
