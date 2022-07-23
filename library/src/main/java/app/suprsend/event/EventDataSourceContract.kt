package app.suprsend.event

import app.suprsend.database.Event_Model

internal interface EventDataSourceContract {
    fun track(body: String, isDirty: Boolean = true)
    fun getEvents(limit: Long, isDirty: Boolean = true): List<Event_Model>
    fun delete(ids: String)
}
