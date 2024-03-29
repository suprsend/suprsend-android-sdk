package app.suprsend.user.preference

import java.util.Locale

enum class PreferenceOptions {
    OPT_IN, OPT_OUT;

    fun getNetworkName(): String {
        return name.toLowerCase(Locale.getDefault())
    }
    companion object{
        fun from(isSelected:Boolean): PreferenceOptions {
            return if(isSelected) OPT_IN else OPT_OUT
        }
    }
}