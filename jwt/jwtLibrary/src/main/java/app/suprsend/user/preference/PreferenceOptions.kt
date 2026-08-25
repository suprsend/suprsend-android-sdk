package app.suprsend.user.preference

enum class PreferenceOptions(val rawValue: String) {
    optIn("opt_in"),
    optOut("opt_out");

    companion object {
        fun from(value: String?): PreferenceOptions {
            return values().find { it.rawValue.equals(value, ignoreCase = true) } ?: optOut
        }
    }
}

enum class ChannelLevelPreferenceOptions(val rawValue: String) {
    all("all"),
    required("required")
}

sealed class PreferenceTags {
    class string(val value: String) : PreferenceTags()
    class dictionary(val dict: Map<String, Any>) : PreferenceTags()
}
