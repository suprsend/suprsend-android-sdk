package app.suprsend.sprop

import app.suprsend.base.BaseTest
import junit.framework.Assert.assertEquals
import org.json.JSONObject
import org.junit.Test

class SuperPropertiesLocalDataSourceTest : BaseTest() {

    @Test
    fun testAddSuperProperty() {
        val superPropertiesLocalDataSource = SuperPropertiesLocalDataSource()
        superPropertiesLocalDataSource.add("Product Name", "Cycle 123")
        assertEquals(
            JSONObject().apply {
                put("Product Name", "Cycle 123")
            }.toString(),
            superPropertiesLocalDataSource.getAll().toString()
        )
    }

    @Test
    fun testRemoveProperty() {
        val superPropertiesLocalDataSource = SuperPropertiesLocalDataSource()
        superPropertiesLocalDataSource.add("Product Name", "Cycle 123")
        superPropertiesLocalDataSource.add("Price", 590)
        assertEquals(
            JSONObject().apply {
                put("Product Name", "Cycle 123")
                put("Price", 590)
            }.toString(),
            superPropertiesLocalDataSource.getAll().toString()
        )

        superPropertiesLocalDataSource.remove("Price")
        assertEquals(
            JSONObject().apply {
                put("Product Name", "Cycle 123")
            }.toString(),
            superPropertiesLocalDataSource.getAll().toString()
        )
    }
}