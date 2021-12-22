package app.suprsend.user

import app.suprsend.base.BaseTest
import junit.framework.Assert.assertEquals
import org.junit.Test

class UserLocalDatasourceTest : BaseTest() {

    @Test
    fun testIdentifyWithCallingIdentity() {
        val userLocalDatasource = UserLocalDatasource()
        assertEquals("", userLocalDatasource.getIdentity())
    }

    @Test
    fun testIdentify() {
        val userLocalDatasource = UserLocalDatasource()
        userLocalDatasource.identify("123")
        assertEquals("123", userLocalDatasource.getIdentity())
    }
}
