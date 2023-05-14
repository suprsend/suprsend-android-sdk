package app.suprsend.user.preference

data class UserPreferences(
    val categories: List<Category>,
    val channelPreferences: List<ChannelPreference>
)