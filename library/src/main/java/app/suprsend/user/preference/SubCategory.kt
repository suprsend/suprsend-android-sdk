package app.suprsend.user.preference

data class SubCategory(
    val name: String,
    val category: String,
    val description: String,
    val defaultPreference: String,
    val preference: Preference,
    val isEditable: Boolean,
    val channels: List<Channel>
)