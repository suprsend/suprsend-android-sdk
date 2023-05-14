package app.suprsend.user.preference

interface UserPreferenceListener {
    fun onUpdate(userPreferences: UserPreferences)
}