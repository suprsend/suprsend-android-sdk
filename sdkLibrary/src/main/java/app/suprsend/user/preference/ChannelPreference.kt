package app.suprsend.user.preference

data class ChannelPreference(
    val channel: String,
    val isRestricted: Boolean
) {
    fun toChannelPreferenceOptions(): ChannelPreferenceOptions {
        return if (isRestricted) ChannelPreferenceOptions.REQUIRED else ChannelPreferenceOptions.ALL
    }
}