package app.suprsend.user.preference

data class PreferenceData(
    val sections: List<Section> = listOf(),
    val channelPreferences: List<ChannelPreference> = listOf()
)