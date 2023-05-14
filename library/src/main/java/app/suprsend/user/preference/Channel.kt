package app.suprsend.user.preference

data class Channel(
    val channel: String,
    val preference: Preference,
    val isEditable: Boolean
)