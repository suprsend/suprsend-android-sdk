package app.suprsend.user.preference

import app.suprsend.base.BaseTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SSInternalUserPreferenceTest : BaseTest() {

    @Test
    fun fetchPreferenceData() {
        val preference = SSInternalUserPreference.fetchAndSavePreferenceData(fetchRemote = true)
        Assert.assertEquals(true, preference.isSuccess())
    }

    @Test
    fun fetchCategories() {
        val preference = SSInternalUserPreference.fetchCategories()
        Assert.assertEquals(true, preference.isSuccess())
    }

    @Test
    fun getCategory() {
        val preference = SSInternalUserPreference.fetchCategory(category = "overall-check-for-system")
        Assert.assertEquals(true, preference.isSuccess())
    }

    @Test
    fun fetchOverallChannelPreferences() {
        val preference = SSInternalUserPreference.fetchOverallChannelPreferences()
        Assert.assertEquals(true, preference.isSuccess())
    }

    @Test
    fun updateCategoryPreferencePrefNotPresent() {
        val preference = SSInternalUserPreference.updateCategoryPreference(category = "user-opt-in-and-out-cases", preference = PreferenceOptions.OPT_OUT, brandId = null)
        Assert.assertEquals(false, preference.isSuccess())
    }

    @Test
    fun updateCategoryPreference() {
        SSInternalUserPreference.fetchAndSavePreferenceData(fetchRemote = true)
        val preference = SSInternalUserPreference.updateCategoryPreference(
            category = "user-opt-in-and-out-cases",
            preference = PreferenceOptions.OPT_OUT,
            brandId = null
        )
        Assert.assertEquals(true, preference.isSuccess())
    }

    @Test
    fun updateChannelPreferenceInCategoryPrefNotPresent() {
        val preference = SSInternalUserPreference.updateChannelPreferenceInCategory(
            category = "do-not-publish",
            preference = PreferenceOptions.OPT_OUT,
            brandId = null,
            channel = "email"
        )
        Assert.assertEquals(false, preference.isSuccess())
    }

    @Test
    fun updateChannelPreferenceInCategory() {
        SSInternalUserPreference.fetchAndSavePreferenceData(fetchRemote = true)
        val preference = SSInternalUserPreference.updateChannelPreferenceInCategory(
            category = "do-not-publish",
            preference = PreferenceOptions.OPT_IN,
            brandId = null,
            channel = "email"
        )
        Assert.assertEquals(preference.getException().toString(), true, preference.isSuccess())
    }

    @Test
    fun updateOverallChannelPreferencePrefNotPresent() {
        val preference = SSInternalUserPreference.updateOverallChannelPreference(
            channel = "email",
            channelPreference = ChannelPreferenceOptions.REQUIRED
        )
        Assert.assertEquals(false, preference.isSuccess())
    }

    @Test
    fun updateOverallChannelPreference() {
        SSInternalUserPreference.fetchAndSavePreferenceData(fetchRemote = true)
        val preference = SSInternalUserPreference.updateOverallChannelPreference(
            channel = "email",
            channelPreference = ChannelPreferenceOptions.ALL
        )
        Assert.assertEquals(preference.getException().toString(), true, preference.isSuccess())
    }

}