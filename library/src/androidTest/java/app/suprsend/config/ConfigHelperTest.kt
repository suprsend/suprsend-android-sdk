package app.suprsend.config

import app.suprsend.base.BaseTest
import junit.framework.Assert.assertEquals
import org.junit.Test

class ConfigHelperTest : BaseTest() {

    @Test
    fun testAddOrUpdate() {
        ConfigHelper.addOrUpdate("appName", "SuperSend")
        assertEquals("SuperSend", ConfigHelper.get("appName"))
    }

    @Test
    fun testAddOrUpdateForNotExistingValue() {
        assertEquals(null, ConfigHelper.get("userName"))
    }
}
