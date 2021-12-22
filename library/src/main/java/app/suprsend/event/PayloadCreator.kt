package app.suprsend.event

import app.suprsend.SSApiInternal
import app.suprsend.base.Logger
import app.suprsend.base.SSConstants
import app.suprsend.base.addUpdateJsoObject
import app.suprsend.base.size
import app.suprsend.base.uuid
import app.suprsend.user.api.SSInternalUser
import org.json.JSONArray
import org.json.JSONObject

internal object PayloadCreator {

    fun buildIdentityEventPayload(
        identifiedId: String,
        anonymousId: String,
        apiKey: String = SSApiInternal.getCachedApiKey()
    ): JSONObject {
        val identifyPayload = JSONObject()
        identifyPayload.put(SSConstants.EVENT, SSConstants.IDENTIFY)
        identifyPayload.put(SSConstants.ENV, apiKey)

        val propertiesJsonObject = JSONObject()
        propertiesJsonObject.put(SSConstants.IDENTIFIED_ID, identifiedId)
        propertiesJsonObject.put(SSConstants.ANONYMOUS_ID, anonymousId)
        identifyPayload.put(SSConstants.PROPERTIES, propertiesJsonObject)
        Logger.i(SSApiInternal.TAG, "identity : $identifiedId $identifyPayload")
        return identifyPayload
    }

    fun buildTrackEventPayload(
        eventName: String,
        distinctId: String,
        superProperties: JSONObject,
        defaultProperties: JSONObject,
        userProperties: JSONObject?,
        apiKey: String = SSApiInternal.getCachedApiKey()
    ): JSONObject {

        var finalProperties = defaultProperties

        // Add super properties
        if (superProperties.size() > 0)
            finalProperties = finalProperties.addUpdateJsoObject(superProperties)

        // Add user properties
        if (userProperties.size() > 0)
            finalProperties = finalProperties.addUpdateJsoObject(userProperties)

        val eventPayload = JSONObject()
        eventPayload.addCommonEventProperties()
        eventPayload.put(SSConstants.EVENT, eventName)
        eventPayload.put(SSConstants.DISTINCT_ID, distinctId)
        eventPayload.put(SSConstants.ENV, apiKey)
        eventPayload.put(SSConstants.PROPERTIES, finalProperties)

        Logger.i(SSApiInternal.TAG, "Event Payload : $eventName $userProperties $eventPayload")

        return eventPayload
    }

    /**
     * Supported operator are
     * set,unset,set_once,add,append,remove
     */
    fun buildUserOperatorPayload(
        distinctId: String,
        operator: String,
        setProperties: JSONObject? = null,
        setPropertiesArray: JSONArray? = null,
        apiKey: String = SSApiInternal.getCachedApiKey()
    ): JSONObject {
        val operatorPayload = JSONObject()
        operatorPayload.addCommonEventProperties()
        operatorPayload.put(SSConstants.DISTINCT_ID, distinctId)
        operatorPayload.put(SSConstants.ENV, apiKey)

        if (setProperties != null)
            operatorPayload.put(operator, setProperties)

        if (setPropertiesArray != null)
            operatorPayload.put(operator, setPropertiesArray)

        Logger.i(SSInternalUser.TAG, "Operator Payload : $operator $setProperties $operatorPayload")
        return operatorPayload
    }
}

private fun JSONObject.addCommonEventProperties() {
    put(SSConstants.INSERT_ID, uuid())
    put(SSConstants.TIME, System.currentTimeMillis())
}
