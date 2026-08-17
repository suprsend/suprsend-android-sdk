package app.suprsend.user

import app.suprsend.SSApiInternal
import app.suprsend.base.Logger
import app.suprsend.base.SSConstants
import app.suprsend.config.ConfigHelper

internal class UserLocalDatasource {

    fun identify(uniqueId: String) {
        Logger.i(TAG, "Identity : $uniqueId")
        ConfigHelper.addOrUpdate(SSConstants.CONFIG_USER_ID, uniqueId)
    }

    fun getIdentity(): String {
        return ConfigHelper.get(SSConstants.CONFIG_USER_ID) ?: ""
    }

    companion object {
        const val TAG = SSApiInternal.TAG
    }
}
