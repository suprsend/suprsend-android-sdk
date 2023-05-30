package app.suprsend.database

internal class Event_Model(
    var value: String? = null,
    var isDirty: Int = 0,
    var timeStamp: Long = 0,
    var uuid: String? = null,
    //Database autoincrement id
    var id: Long? = null
)