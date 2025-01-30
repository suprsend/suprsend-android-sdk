package app.suprsend.user.preference

data class PreferenceData(
    val sections: List<Section>,
    val channelPreferences: List<ChannelPreference>
)