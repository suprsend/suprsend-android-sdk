package app.suprsend.user.preference

import app.suprsend.base.NetworkInfo
import app.suprsend.base.Response
import app.suprsend.base.SSConstants
import app.suprsend.base.executeWithThrottleLast
import app.suprsend.exception.NoInternetException
import app.suprsend.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class PreferencesImpl : Preferences {

    override fun setPreferenceConfig(tenantId: String?, showOptOutChannels: Boolean) {
        SSInternalUserPreference.tenantId = tenantId
        SSInternalUserPreference.showOptOutChannels = showOptOutChannels
    }

    override fun registerCallback(preferenceCallback: PreferenceCallback) {
        SSInternalUserPreference.preferenceCallback = preferenceCallback
    }

    override fun unRegisterCallback() {
        SSInternalUserPreference.preferenceCallback = null
    }

    override fun fetchUserPreference(fetchRemote: Boolean): Response<PreferenceData> {
        val response = SSInternalUserPreference.fetchAndSavePreferenceData(fetchRemote)
        if (fetchRemote)
            sendUpdate()
        return response
    }

    override fun fetchCategories(
        limit: Int?,
        offset: Int?
    ): Response<JSONObject> {
        val response = SSInternalUserPreference.fetchCategories(limit, offset)
        return response
    }

    override fun fetchCategory(
        category: String
    ): Response<JSONObject> {
        val response = SSInternalUserPreference.fetchCategory(category)
        sendUpdate()
        return response
    }

    override fun fetchOverallChannelPreferences(): Response<JSONObject> {
        val response = SSInternalUserPreference.fetchOverallChannelPreferences()
        sendUpdate()
        return response
    }

    override fun updateCategoryPreference(
        category: String,
        preference: PreferenceOptions
    ): Response<JSONObject> {
        Logger.i(SSConstants.TAG_SUPRSEND,"updateCategoryPreference : $category : ${preference.name}")
        if (!NetworkInfo.isConnected()) {
            sendUpdate()
            return Response.Error(NoInternetException())
        }
        var response: Response<JSONObject>? = null
        val job = CoroutineScope(Dispatchers.IO).launch(start = CoroutineStart.LAZY) {
            Logger.i(SSConstants.TAG_SUPRSEND,"updateCategoryPreference : executing $category ${preference.name}")
            response = SSInternalUserPreference.updateCategoryPreference(category, preference)
        }
        val scheduleJob = job.executeWithThrottleLast("Category$category", 2000,"$category ${preference.name}")

        Logger.i(SSConstants.TAG_SUPRSEND,"isCancelled: $category : ${preference.name} : ${scheduleJob.isCancelled}")
        Logger.i(SSConstants.TAG_SUPRSEND,"isActive: $category : ${preference.name} : ${scheduleJob.isActive}")
        Logger.i(SSConstants.TAG_SUPRSEND,"isCompleted: $category : ${preference.name} : ${scheduleJob.isCompleted}")

        val returnResponse= if (scheduleJob.isCancelled) {
            Response.Error(Exception("updateCategoryPreference ignored due to debounce: $category : ${preference.name}"))
        } else {
            if (response?.isSuccess() == true) {
                sendUpdate()
            }else{
                Logger.e(SSConstants.TAG_SUPRSEND, response?.getException()?.message ?: "updateCategoryPreference something went wrong : $category : ${preference.name}")
            }
            response ?: Response.Error(Exception("updateCategoryPreference something went wrong : $category : ${preference.name}"))
        }
        Logger.i(SSConstants.TAG_SUPRSEND,"returnResponse : $category : ${preference.name} : ${returnResponse.getData()} : ${returnResponse.getException()?.message}")
        return returnResponse
    }

    override fun updateChannelPreferenceInCategory(
        category: String,
        channel: String,
        preference: PreferenceOptions
    ): Response<JSONObject> {
        if (!NetworkInfo.isConnected()) {
            sendUpdate()
            return Response.Error(NoInternetException())
        }
        var response: Response<JSONObject>? = null
        val job = CoroutineScope(Dispatchers.IO).launch(start = CoroutineStart.LAZY) {
            response = SSInternalUserPreference.updateChannelPreferenceInCategory(category, channel, preference)
        }
        val scheduleJob = job.executeWithThrottleLast("Channel$category$channel", 2000,"$category : $channel : ${preference.name}")
        return if (scheduleJob.isCancelled) {
            Response.Error(Exception("updateChannelPreferenceInCategory ignored due to debounce: $category : $channel : ${preference.name}"))
        } else {
            if (response?.isSuccess() == true) {
                sendUpdate()
            }else{
                Logger.e(SSConstants.TAG_SUPRSEND, response?.getException()?.message ?: "Something went wrong")
            }
            response ?: Response.Error(Exception("updateCategoryPreference something went wrong: $category : $channel"))
        }
    }

    override fun updateOverallChannelPreference(
        channel: String,
        channelPreferenceOptions: ChannelPreferenceOptions
    ): Response<JSONObject> {
        if (!NetworkInfo.isConnected()) {
            sendUpdate()
            return Response.Error(NoInternetException())
        }
        var response: Response<JSONObject>? = null
        val job = CoroutineScope(Dispatchers.IO).launch(start = CoroutineStart.LAZY) {
            response = SSInternalUserPreference.updateOverallChannelPreference(channel, channelPreferenceOptions)
        }
        val scheduleJob = job.executeWithThrottleLast("Overall$channel", 2000,"$channel ${channelPreferenceOptions.name}")
        return if (scheduleJob.isCancelled) {
            Response.Error(Exception("updateOverallChannelPreference ignored due to debounce: $channel"))
        } else {
            if (response?.isSuccess() != true) {
                Logger.e(SSConstants.TAG_SUPRSEND, response?.getException()?.message ?: "Something went wrong")
            }
            response ?: Response.Error(Exception("updateOverallChannelPreference something went wrong: $channel"))
        }
    }

    private fun sendUpdate() {
        SSInternalUserPreference.preferenceCallback?.onUpdate(fetchUserPreference(false).getData()?:PreferenceData())
    }

}