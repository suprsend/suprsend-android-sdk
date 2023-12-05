package app.suprsend.base

import androidx.test.InstrumentationRegistry
import app.suprsend.SSApi
import app.suprsend.SSApiInternal
import app.suprsend.config.ConfigHelper
import app.suprsend.database.SQLDataHelper
import junit.framework.Assert.assertEquals
import org.json.JSONObject
import org.junit.Before

open class BaseTest {
    val context = InstrumentationRegistry.getContext()
    private val sqlDataHelper = SQLDataHelper(context)

    init {
        SdkAndroidCreator.context = context
        if (
            listOf(
                TestConstant.CONFIG_API_BASE_URL,
                TestConstant.CONFIG_API_KEY,
                TestConstant.CONFIG_API_SECRET
            ).any { it == "XXXX" }
        )
            throw IllegalStateException("Please input credentials")

        ConfigHelper.addOrUpdate(SSConstants.CONFIG_API_BASE_URL, TestConstant.CONFIG_API_BASE_URL)
        ConfigHelper.addOrUpdate(SSConstants.CONFIG_API_KEY, TestConstant.CONFIG_API_KEY)
        ConfigHelper.addOrUpdate(SSConstants.CONFIG_API_SECRET, TestConstant.CONFIG_API_SECRET)
        SSApiInternal.identify(TestConstant.CONFIG_API_USER_ID)
    }

    @Before
    fun before() {
        clean()
    }

    protected fun clean() {
        deleteAllEvents()
        sqlDataHelper.deleteAllConfigs()
    }

    fun deleteAllEvents() {
        sqlDataHelper.deleteAllEvents()
    }

    protected fun getTestProductData(): MutableMap<String, Any> {
        return mutableMapOf(
            "Product ID" to "P1",
            "Product Available" to true,
            "Product Price" to 43.1,
            "Product Quantity" to 10,
            "Product Sold" to 9999999999999L
        )
    }

    protected fun getTestProductJsonObject(): JSONObject {
        return JSONObject(getTestProductData() as Map<*, *>)
    }

    protected fun verifyProductProperties(propertiesPayload: JSONObject) {
        assertEquals("P1", propertiesPayload.getString("Product ID"))
        assertEquals(true, propertiesPayload.getBoolean("Product Available"))
        assertEquals(43.1, propertiesPayload.getDouble("Product Price"), 0.0)
        assertEquals(10, propertiesPayload.getInt("Product Quantity"))
        assertEquals(9999999999999L, propertiesPayload.getLong("Product Sold"))
    }
}
