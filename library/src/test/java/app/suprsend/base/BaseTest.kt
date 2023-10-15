package app.suprsend.base

import app.suprsend.config.ConfigHelper
import org.robolectric.RuntimeEnvironment

open class BaseTest {
    init {
        SdkAndroidCreator.context = RuntimeEnvironment.getApplication().applicationContext
        ConfigHelper.addOrUpdate(SSConstants.CONFIG_API_BASE_URL, TestConstant.CONFIG_API_BASE_URL)
        ConfigHelper.addOrUpdate(SSConstants.CONFIG_API_KEY, TestConstant.CONFIG_API_KEY)
        ConfigHelper.addOrUpdate(SSConstants.CONFIG_API_SECRET, TestConstant.CONFIG_API_SECRET)
        ConfigHelper.addOrUpdate(SSConstants.CONFIG_USER_ID, TestConstant.CONFIG_API_USER_ID)
    }
}