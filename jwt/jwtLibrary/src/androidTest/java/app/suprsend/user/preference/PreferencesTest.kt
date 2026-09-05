package app.suprsend.user.preference

import app.suprsend.Emitter
import app.suprsend.SuprSend
import app.suprsend.model.ErrorType
import app.suprsend.model.ResponseStatus
import io.mockk.verify
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PreferencesTest : PreferencesTestHelper() {

    @Before
    fun setup() {
        identifyUser()
    }

    @Test
    fun verifyFetchUserPreference() {
        val data = preferences().getPreferences().body

        Assert.assertEquals(5, data?.sections?.size)
        Assert.assertEquals(5, data?.channelPreferences?.size)

        Assert.assertEquals("refund-promotion", data?.sections?.get(0)?.subcategories?.get(0)?.category)
        Assert.assertEquals(PreferenceOptions.optOut, data?.sections?.get(0)?.subcategories?.get(0)?.preference)
        Assert.assertEquals(
            true,
            data?.sections?.get(0)?.subcategories?.get(0)?.channels?.all { it.preference == PreferenceOptions.optOut }
        )
    }

    @Test
    fun getCategoriesReturnsResults() {
        val response = preferences().getCategories(
            args = Preferences.CategoryArgs(limit = 10, offset = 0)
        )

        Assert.assertEquals(ResponseStatus.SUCCESS, response.status)
        val results = JSONObject(response.body!!).getJSONArray("results")
        Assert.assertEquals(2, results.length())
        Assert.assertEquals("refund-promotion", results.getJSONObject(0).getString("category"))

        verify {
            networkClient.httpCall(
                url = match { url ->
                    url.contains("/preference/category") &&
                        url.contains("limit=10") &&
                        url.contains("offset=0")
                },
                authorization = any(),
                date = any(),
                requestJson = any(),
                requestMethod = any(),
                headers = any()
            )
        }
    }

    @Test
    fun getCategoryReturnsCategoryPayload() {
        val response = preferences().getCategory("refund-promotion")

        Assert.assertEquals(ResponseStatus.SUCCESS, response.status)
        val body = JSONObject(response.body!!)
        Assert.assertEquals("refund-promotion", body.getString("category"))
        Assert.assertEquals("opt_in", body.getString("preference"))
    }

    @Test
    fun getOverallChannelPreferencesReturnsChannels() {
        val response = preferences().getOverallChannelPreferences()

        Assert.assertEquals(ResponseStatus.SUCCESS, response.status)
        val channels = JSONObject(response.body!!).getJSONArray("channel_preferences")
        Assert.assertEquals(5, channels.length())
        Assert.assertEquals("androidpush", channels.getJSONObject(0).getString("channel"))
        Assert.assertEquals(false, channels.getJSONObject(0).getBoolean("is_restricted"))
    }

    @Test
    fun getPreferencesSendsTagsLocaleAndTenantId() {
        val response = preferences().getPreferences(
            args = Preferences.Args(
                tenantId = "T1",
                showOptOutChannels = false,
                tags = PreferenceTags.string("premium"),
                locale = "en"
            )
        )

        Assert.assertEquals(ResponseStatus.SUCCESS, response.status)
        verify {
            networkClient.httpCall(
                url = match { url ->
                    url.contains("/preference/") &&
                        !url.contains("/preference/category") &&
                        !url.contains("/preference/channel_preference") &&
                        url.contains("tenant_id=T1") &&
                        url.contains("show_opt_out_channels=false") &&
                        url.contains("tags=premium") &&
                        url.contains("locale=en")
                },
                authorization = any(),
                date = any(),
                requestJson = any(),
                requestMethod = any(),
                headers = any()
            )
        }
    }

    @Test
    fun getPreferencesSendsDictionaryTags() {
        preferences().getPreferences(
            args = Preferences.Args(
                tags = PreferenceTags.dictionary(mapOf("tier" to "gold", "plan" to "pro"))
            )
        )

        verify {
            networkClient.httpCall(
                url = match { url ->
                    url.contains("tags=") &&
                        (url.contains("%7B") || url.contains("{")) &&
                        url.contains("plan") &&
                        url.contains("pro") &&
                        url.contains("tier") &&
                        url.contains("gold")
                },
                authorization = any(),
                date = any(),
                requestJson = any(),
                requestMethod = any(),
                headers = any()
            )
        }
    }

    /**
     * Covers below cases
     *  Category
     *      - optIn and Verify
     *      - optOut and Verify
     *  Category Channel
     *      - optIn and Verify
     *      - optOut and Verify
     */
    @Test
    fun verifyUpdateCategoryAndChannelPreference() {
        val preferences = preferences()
        var data = preferences.getPreferences().body

        var subCategory = data?.sections?.get(0)?.subcategories?.get(0)
        Assert.assertEquals("refund-promotion", subCategory?.category)
        Assert.assertEquals(PreferenceOptions.optOut, subCategory?.preference)

        var response = preferences.updateCategoryPreference(
            category = "refund-promotion",
            preference = PreferenceOptions.optIn
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.data
        subCategory = data?.sections?.get(0)?.subcategories?.get(0)
        Assert.assertEquals("refund-promotion", subCategory?.category)
        Assert.assertEquals(PreferenceOptions.optIn, subCategory?.preference)

        response = preferences.updateChannelPreferenceInCategory(
            channel = "whatsapp",
            preference = PreferenceOptions.optOut,
            category = "refund-promotion"
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.data
        subCategory = data?.sections?.get(0)?.subcategories?.get(0)
        Assert.assertEquals("refund-promotion", subCategory?.category)
        Assert.assertEquals(PreferenceOptions.optIn, subCategory?.preference)
        var channel = subCategory?.channels?.last()
        Assert.assertEquals("whatsapp", channel?.channel)
        Assert.assertEquals(PreferenceOptions.optOut, channel?.preference)

        response = preferences.updateChannelPreferenceInCategory(
            channel = "whatsapp",
            preference = PreferenceOptions.optIn,
            category = "refund-promotion"
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.data
        subCategory = data?.sections?.get(0)?.subcategories?.get(0)
        Assert.assertEquals("refund-promotion", subCategory?.category)
        Assert.assertEquals(PreferenceOptions.optIn, subCategory?.preference)
        channel = subCategory?.channels?.last()
        Assert.assertEquals("whatsapp", channel?.channel)
        Assert.assertEquals(PreferenceOptions.optIn, channel?.preference)

        response = preferences.updateCategoryPreference(
            category = "refund-promotion",
            preference = PreferenceOptions.optOut
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.data
        subCategory = data?.sections?.get(0)?.subcategories?.get(0)
        Assert.assertEquals("refund-promotion", subCategory?.category)
        Assert.assertEquals(PreferenceOptions.optOut, subCategory?.preference)
    }

    /**
     * Covers below cases
     * all and verify
     * required and verify
     */
    @Test
    fun verifyChannelPreferenceRestricted() {
        val preferences = preferences()
        var data = preferences.getPreferences().body

        var channel = data?.channelPreferences?.get(0)
        Assert.assertEquals("androidpush", channel?.channel)
        Assert.assertEquals(false, channel?.isRestricted)

        var response = preferences.updateOverallChannelPreference(
            "androidpush",
            ChannelLevelPreferenceOptions.all
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.data
        channel = data?.channelPreferences?.get(0)
        Assert.assertEquals("androidpush", channel?.channel)
        Assert.assertEquals(false, channel?.isRestricted)

        response = preferences.updateOverallChannelPreference(
            "androidpush",
            ChannelLevelPreferenceOptions.required
        )

        Assert.assertEquals(true, response.isSuccess())
        data = preferences.data
        channel = data?.channelPreferences?.get(0)
        Assert.assertEquals("androidpush", channel?.channel)
        Assert.assertEquals(true, channel?.isRestricted)
    }

    @Test
    fun updateWithoutFetchReturnsValidationError() {
        val response = preferences().updateCategoryPreference(
            category = "refund-promotion",
            preference = PreferenceOptions.optIn
        )

        Assert.assertEquals(ResponseStatus.ERROR, response.status)
        Assert.assertEquals(ErrorType.VALIDATION_ERROR, response.error?.type)
        Assert.assertEquals("Call getPreferences method before performing action", response.error?.message)
    }

    @Test
    fun updateUnknownCategoryReturnsValidationError() {
        preferences().getPreferences()

        val response = preferences().updateCategoryPreference(
            category = "missing-category",
            preference = PreferenceOptions.optIn
        )

        Assert.assertEquals(ResponseStatus.ERROR, response.status)
        Assert.assertEquals(ErrorType.VALIDATION_ERROR, response.error?.type)
        Assert.assertEquals("Category not found", response.error?.message)
    }

    @Test
    fun updateNonEditableCategoryReturnsValidationError() {
        preferences().getPreferences()

        val response = preferences().updateCategoryPreference(
            category = "sub-category",
            preference = PreferenceOptions.optOut
        )

        Assert.assertEquals(ResponseStatus.ERROR, response.status)
        Assert.assertEquals(ErrorType.VALIDATION_ERROR, response.error?.type)
        Assert.assertEquals("Category preference is not editable", response.error?.message)
    }

    @Test
    fun updateNonEditableChannelReturnsValidationError() {
        preferences().getPreferences()

        val response = preferences().updateChannelPreferenceInCategory(
            channel = "email",
            preference = PreferenceOptions.optIn,
            category = "refund-promotion"
        )

        Assert.assertEquals(ResponseStatus.ERROR, response.status)
        Assert.assertEquals(ErrorType.VALIDATION_ERROR, response.error?.type)
        Assert.assertEquals("Channel preference is not editable", response.error?.message)
    }

    @Test
    fun updateUnknownOverallChannelReturnsValidationError() {
        preferences().getPreferences()

        val response = preferences().updateOverallChannelPreference(
            channel = "sms",
            preference = ChannelLevelPreferenceOptions.required
        )

        Assert.assertEquals(ResponseStatus.ERROR, response.status)
        Assert.assertEquals(ErrorType.VALIDATION_ERROR, response.error?.type)
        Assert.assertEquals("Channel data not found", response.error?.message)
    }

    @Test
    fun updateCategoryEmitsPreferencesUpdatedAfterDebounce() {
        Preferences.debounceDelayMs = 50L
        preferences().getPreferences()

        val latch = CountDownLatch(1)
        var updated: PreferenceAPIResponse? = null
        SuprSend.getInstance().emitter.on(Emitter.Event.preferencesUpdated) { response ->
            updated = response
            latch.countDown()
        }

        val response = preferences().updateCategoryPreference(
            category = "refund-promotion",
            preference = PreferenceOptions.optIn
        )
        Assert.assertTrue(response.isSuccess())
        Assert.assertTrue(latch.await(2, TimeUnit.SECONDS))
        Assert.assertEquals(ResponseStatus.SUCCESS, updated?.status)
        Assert.assertEquals(5, updated?.body?.sections?.size)
    }

    @Test
    fun resetClearsCachedPreferenceData() {
        preferences().getPreferences()
        Assert.assertNotNull(preferences().data)

        SuprSend.getInstance().reset(true)
        Assert.assertNull(preferences().data)
    }
}
