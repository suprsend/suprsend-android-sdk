package app.suprsend.user.preference

data class Channel(
    val channel: String,
    val preferenceOptions: PreferenceOptions,
    val isEditable: Boolean
)