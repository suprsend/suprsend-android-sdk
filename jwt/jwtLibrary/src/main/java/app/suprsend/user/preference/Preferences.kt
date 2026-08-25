package app.suprsend.user.preference

import androidx.annotation.WorkerThread
import app.suprsend.Emitter
import app.suprsend.SSInternal
import app.suprsend.SuprSend
import app.suprsend.model.ApiResponse
import app.suprsend.model.ErrorType
import app.suprsend.model.ResponseError
import app.suprsend.utils.urlEncode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class Preferences {

    class Args(
        val tenantId: String? = null,
        val showOptOutChannels: Boolean? = null,
        val tags: PreferenceTags? = null,
        val locale: String? = null
    )

    class CategoryArgs(
        val tenantId: String? = null,
        val showOptOutChannels: Boolean? = null,
        val tags: PreferenceTags? = null,
        val locale: String? = null,
        val limit: Int? = null,
        val offset: Int? = null
    )

    private class UpdateCategoryParams(
        val category: String,
        val body: RequestPayload,
        val subCategory: Category,
        val args: Args?
    )

    private class UpdateChannelParams(
        val body: ChannelRequestPayload,
        val args: Args?
    )

    private var preferenceData: PreferenceData? = null
    private var preferenceArgs: Args? = null

    private val categoryPreferenceDebouncer = KeyedDebouncer<UpdateCategoryParams>()
    private val channelPreferenceDebouncer = KeyedDebouncer<UpdateChannelParams>()

    var data: PreferenceData?
        get() = preferenceData
        set(value) {
            preferenceData = value
        }

    init {
        categoryPreferenceDebouncer.action = { params ->
            _updateCategoryPreferences(
                category = params.category,
                body = params.body,
                subCategory = params.subCategory,
                args = params.args
            )
        }
        channelPreferenceDebouncer.action = { params ->
            _updateChannelPreferences(body = params.body, args = params.args)
        }
    }

    internal fun getUrlpath(path: String? = null, qp: Map<String, Any?>? = null): String {
        val distinctID = urlEncode(SSInternal.suprSendData.distinctId ?: "")
        var urlPath = "v1/user/$distinctID/preference/"
        if (!path.isNullOrEmpty()) {
            urlPath += "$path/"
        }
        val queryParams = qp?.mapNotNull { (key, value) ->
            if (value != null) "$key=${urlEncode(value.toString())}" else null
        }?.joinToString("&")
        return if (queryParams.isNullOrEmpty()) urlPath else "$urlPath?$queryParams"
    }

    private fun encodeTags(tags: PreferenceTags?): String? {
        if (tags == null) return null
        return when (tags) {
            is PreferenceTags.string -> tags.value
            is PreferenceTags.dictionary -> {
                try {
                    val sorted = JSONObject()
                    tags.dict.keys.sorted().forEach { key ->
                        sorted.put(key, tags.dict[key])
                    }
                    sorted.toString()
                } catch (ignored: Exception) {
                    null
                }
            }
            else -> null
        }
    }

    private fun resolveShowOptOutChannels(args: Args?): Boolean {
        return args?.showOptOutChannels
            ?: preferenceArgs?.showOptOutChannels
            ?: true
    }

    private fun resolvedArgs(args: Args?, showOptOutChannels: Boolean): Args {
        return Args(
            tenantId = args?.tenantId ?: preferenceArgs?.tenantId ?: SSInternal.suprSendData.tenantId,
            showOptOutChannels = showOptOutChannels,
            tags = args?.tags ?: preferenceArgs?.tags,
            locale = args?.locale ?: preferenceArgs?.locale
        )
    }

    private fun tenantId(argsTenantId: String?): String? {
        return argsTenantId ?: SSInternal.suprSendData.tenantId
    }

    @WorkerThread
    fun getPreferences(args: Args? = null): PreferenceAPIResponse {
        val queryParams = mapOf(
            "tenant_id" to tenantId(args?.tenantId),
            "show_opt_out_channels" to (args?.showOptOutChannels ?: true),
            "tags" to encodeTags(args?.tags),
            "locale" to args?.locale
        )
        preferenceArgs = args

        val response = UserPreferenceRemote.request(path = getUrlpath(qp = queryParams))
            .toPreferenceAPIResponse()

        if (response.error == null) {
            this.data = response.body
        }
        return response
    }

    @WorkerThread
    fun getCategories(args: CategoryArgs? = null): ApiResponse {
        val queryParams = mapOf(
            "tenant_id" to tenantId(args?.tenantId),
            "show_opt_out_channels" to (args?.showOptOutChannels ?: true).toString(),
            "tags" to encodeTags(args?.tags),
            "locale" to args?.locale,
            "limit" to args?.limit,
            "offset" to args?.offset
        )
        return UserPreferenceRemote.request(path = getUrlpath(path = "category", qp = queryParams))
    }

    @WorkerThread
    fun getCategory(category: String, args: Args? = null): ApiResponse {
        val queryParams = mapOf(
            "tenant_id" to tenantId(args?.tenantId),
            "show_opt_out_channels" to (args?.showOptOutChannels ?: true).toString(),
            "locale" to args?.locale
        )
        return UserPreferenceRemote.request(path = getUrlpath(path = "category/$category", qp = queryParams))
    }

    @WorkerThread
    fun getOverallChannelPreferences(args: Args? = null): ApiResponse {
        val queryParams = mapOf(
            "tenant_id" to tenantId(args?.tenantId)
        )
        return UserPreferenceRemote.request(path = getUrlpath(path = "channel_preference", qp = queryParams))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun _updateCategoryPreferences(
        category: String,
        body: RequestPayload,
        subCategory: Category,
        args: Args? = null
    ): PreferenceAPIResponse {
        val queryParams = mapOf(
            "tenant_id" to (args?.tenantId ?: preferenceArgs?.tenantId ?: SSInternal.suprSendData.tenantId),
            "show_opt_out_channels" to resolveShowOptOutChannels(args).toString(),
            "tags" to encodeTags(args?.tags ?: preferenceArgs?.tags),
            "locale" to (args?.locale ?: preferenceArgs?.locale)
        )
        val response = UserPreferenceRemote.request(
            path = getUrlpath(path = "category/$category", qp = queryParams),
            requestJson = body.toJson(),
            requestMethod = "PATCH"
        ).toPreferenceAPIResponse()

        if (response.error != null) {
            SuprSend.getInstance().emitter.emit(event = Emitter.Event.preferencesError, data = response)
        } else {
            val refreshed = getPreferences(args = preferenceArgs)
            SuprSend.getInstance().emitter.emit(event = Emitter.Event.preferencesUpdated, data = refreshed)
        }
        return response
    }

    private fun _updateChannelPreferences(
        body: ChannelRequestPayload,
        args: Args? = null
    ): PreferenceAPIResponse {
        val queryParams = mapOf(
            "tenant_id" to (args?.tenantId ?: preferenceArgs?.tenantId ?: SSInternal.suprSendData.tenantId)
        )
        val response = UserPreferenceRemote.request(
            path = getUrlpath(path = "channel_preference", qp = queryParams),
            requestJson = body.toJson(),
            requestMethod = "PATCH"
        ).toPreferenceAPIResponse()

        if (response.error != null) {
            SuprSend.getInstance().emitter.emit(event = Emitter.Event.preferencesError, data = response)
        } else {
            val refreshed = getPreferences(args = preferenceArgs)
            SuprSend.getInstance().emitter.emit(event = Emitter.Event.preferencesUpdated, data = refreshed)
        }
        return response
    }

    fun updateCategoryPreference(
        category: String,
        preference: PreferenceOptions,
        args: Args? = null
    ): PreferenceAPIResponse {
        val data = this.data ?: return validationError("Call getPreferences method before performing action")
        val sections = data.sections ?: return validationError("Sections doesn't exist")

        var categoryData: Category? = null
        var dataUpdated = false

        for (section in sections) {
            var abort = false
            if (section.subcategories == null) {
                continue
            }
            for (subcategory in section.subcategories) {
                if (subcategory.category == category) {
                    categoryData = subcategory
                    if (subcategory.isEditable) {
                        if (subcategory.preference != preference) {
                            subcategory.preference = preference
                            dataUpdated = true
                            abort = true
                            break
                        }
                    } else {
                        return validationError("Category preference is not editable")
                    }
                }
            }
            if (abort) {
                break
            }
        }

        val resolvedCategory = categoryData ?: return validationError("Category not found")
        if (!dataUpdated) {
            return PreferenceAPIResponse.success(statusCode = null, body = data)
        }

        val optOutChannels = arrayListOf<String>()
        resolvedCategory.channels?.forEach { channel ->
            if (channel.preference == PreferenceOptions.optOut) {
                optOutChannels.add(channel.channel)
            }
        }

        val showOptOutChannels = resolveShowOptOutChannels(args)
        val channels = if (showOptOutChannels && preference == PreferenceOptions.optIn) null else optOutChannels
        val requestPayload = RequestPayload(
            preference = resolvedCategory.preference,
            optOutChannels = channels
        )

        categoryPreferenceDebouncer.send(
            key = category,
            payload = UpdateCategoryParams(
                category = category,
                body = requestPayload,
                subCategory = resolvedCategory,
                args = resolvedArgs(args, showOptOutChannels)
            )
        )
        return PreferenceAPIResponse.success(body = data)
    }

    fun updateChannelPreferenceInCategory(
        channel: String,
        preference: PreferenceOptions,
        category: String,
        args: Args? = null
    ): PreferenceAPIResponse {
        val data = this.data ?: return validationError("Call getPreferences method before performing action")
        val sections = data.sections ?: return validationError("Sections doesn't exist")

        var categoryData: Category? = null
        var selectedChannelData: CategoryChannel? = null
        var dataUpdated = false

        for (section in sections) {
            var abort = false
            val subcategories = section.subcategories ?: continue
            for (subcategory in subcategories) {
                if (subcategory.category == category) {
                    categoryData = subcategory
                    val channels = subcategory.channels
                    if (channels != null) {
                        for (channelData in channels) {
                            if (channelData.channel == channel) {
                                selectedChannelData = channelData
                                if (channelData.isEditable) {
                                    if (channelData.preference != preference) {
                                        channelData.preference = preference
                                        if (preference == PreferenceOptions.optIn) {
                                            subcategory.preference = PreferenceOptions.optIn
                                        }
                                        dataUpdated = true
                                        abort = true
                                        break
                                    }
                                } else {
                                    return validationError("Channel preference is not editable")
                                }
                            }
                        }
                    }
                }
                if (abort) {
                    break
                }
            }
            if (abort) {
                break
            }
        }

        val resolvedCategory = categoryData ?: return validationError("Category not found")
        if (selectedChannelData == null) {
            return validationError("Category's channel not found")
        }
        if (!dataUpdated) {
            return PreferenceAPIResponse.success(body = data)
        }

        val optOutChannels = arrayListOf<String>()
        resolvedCategory.channels?.forEach { categoryChannel ->
            if (categoryChannel.preference == PreferenceOptions.optOut) {
                optOutChannels.add(categoryChannel.channel)
            }
        }

        val showOptOutChannels = resolveShowOptOutChannels(args)
        val categoryPreference =
            if (showOptOutChannels && resolvedCategory.preference == PreferenceOptions.optOut && preference == PreferenceOptions.optIn) {
                PreferenceOptions.optIn
            } else {
                resolvedCategory.preference
            }

        val requestPayload = RequestPayload(
            preference = categoryPreference,
            optOutChannels = optOutChannels
        )

        categoryPreferenceDebouncer.send(
            key = category,
            payload = UpdateCategoryParams(
                category = category,
                body = requestPayload,
                subCategory = resolvedCategory,
                args = resolvedArgs(args, showOptOutChannels)
            )
        )
        return PreferenceAPIResponse.success(body = data)
    }

    fun updateOverallChannelPreference(
        channel: String,
        preference: ChannelLevelPreferenceOptions,
        args: Args? = null
    ): PreferenceAPIResponse {
        val data = this.data ?: return validationError("Call getPreferences method before performing action")
        val channelPreferences = data.channelPreferences
            ?: return validationError("Channel preferences doesn't exist")

        var channelData: ChannelPreference? = null
        var dataUpdated = false
        val preferenceRestricted = preference == ChannelLevelPreferenceOptions.required

        for (channelItem in channelPreferences) {
            if (channelItem.channel == channel) {
                channelData = channelItem
                if (channelItem.isRestricted != preferenceRestricted) {
                    channelItem.isRestricted = preferenceRestricted
                    dataUpdated = true
                    break
                }
            }
        }

        val resolvedChannel = channelData ?: return validationError("Channel data not found")
        if (!dataUpdated) {
            return PreferenceAPIResponse.success(body = data)
        }

        val requestPayload = ChannelRequestPayload(channelPreferences = listOf(resolvedChannel))
        channelPreferenceDebouncer.send(
            key = resolvedChannel.channel,
            payload = UpdateChannelParams(body = requestPayload, args = args)
        )
        return PreferenceAPIResponse.success(body = data)
    }

    internal fun clear() {
        preferenceData = null
        preferenceArgs = null
        categoryPreferenceDebouncer.cancelAll()
        channelPreferenceDebouncer.cancelAll()
    }

    private fun validationError(message: String): PreferenceAPIResponse {
        return PreferenceAPIResponse.error(
            ResponseError(type = ErrorType.VALIDATION_ERROR, message = message)
        )
    }

    companion object {
        internal var debounceDelayMs = 1000L
    }
}

internal class KeyedDebouncer<Payload> {
    private val lock = Any()
    private val jobs = mutableMapOf<String, Job>()
    var action: ((Payload) -> Unit)? = null

    fun send(key: String, payload: Payload) {
        val delayMs = Preferences.debounceDelayMs
        synchronized(lock) {
            val actionRef = action
            jobs[key]?.cancel()
            val job = CoroutineScope(Dispatchers.IO).launch {
                delay(delayMs)
                actionRef?.invoke(payload)
            }
            jobs[key] = job
        }
    }

    fun cancelAll() {
        synchronized(lock) {
            jobs.values.forEach { it.cancel() }
            jobs.clear()
        }
    }
}
