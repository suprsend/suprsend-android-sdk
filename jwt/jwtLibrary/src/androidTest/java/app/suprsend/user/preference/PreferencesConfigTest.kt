package app.suprsend.user.preference

import org.junit.Assert
import org.junit.Before
import org.junit.Test

class PreferencesDefaultConfigTest : PreferencesTestHelper() {

    @Before
    fun setup() {
        identifyUser()
    }

    @Test
    fun verifyIfConfigIsNotPresent() {
        val data = preferences().getPreferences(
            args = Preferences.Args(
                tenantId = "T1",
                showOptOutChannels = false
            )
        ).body

        Assert.assertEquals(5, data?.sections?.size)
        Assert.assertEquals(5, data?.channelPreferences?.size)

        Assert.assertEquals("refund-promotion", data?.sections?.get(0)?.subcategories?.get(0)?.category)
        Assert.assertEquals(PreferenceOptions.optOut, data?.sections?.get(0)?.subcategories?.get(0)?.preference)
        Assert.assertEquals(
            true,
            data?.sections?.get(0)?.subcategories?.get(0)?.channels?.all { it.preference == PreferenceOptions.optOut }
        )
    }
}
