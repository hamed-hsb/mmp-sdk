package com.motrack.sdk

import org.json.JSONObject

/**
 * @author yaya (@yahyalmh)
 * @since 06th October 2021
 */

class EventResponseData(activityPackage: ActivityPackage) : ResponseData() {

    private var eventToken: String? = activityPackage.getParameters()?.get("event_token")
    private var callbackId: String? = activityPackage.getParameters()?.get("event_callback_id")
    private var sdkPlatform = activityPackage.getClientSdk()?.let { Util.getSdkPrefixPlatform(it) }


    public fun getSuccessResponseData(): MotrackEventSuccess? {
        if (!success) {
            return null
        }
        val successResponseData = MotrackEventSuccess()
        if ("unity" == sdkPlatform) {
            successResponseData.eventToken = eventToken ?: ""
            successResponseData.message = message ?: ""
            successResponseData.timestamp = timestamp ?: ""
            successResponseData.adid = adid ?: ""
            successResponseData.callbackId = callbackId ?: ""
            successResponseData.jsonResponse = jsonResponse ?: JSONObject()
        } else {
            // Rest of all platforms.
            successResponseData.eventToken = eventToken
            successResponseData.message = message
            successResponseData.timestamp = timestamp
            successResponseData.adid = adid
            successResponseData.callbackId = callbackId
            successResponseData.jsonResponse = jsonResponse
        }

        return successResponseData
    }

    public fun getFailureResponseData(): MotrackEventFailure? {
        if (success) {
            return null
        }
        val failureResponseData = MotrackEventFailure()

        if ("unity" == sdkPlatform) {
            failureResponseData.eventToken = eventToken ?: ""
            failureResponseData.message = message ?: ""
            failureResponseData.timestamp = timestamp ?: ""
            failureResponseData.adid = adid ?: ""
            failureResponseData.callbackId = callbackId ?: ""
            failureResponseData.willRetry = willRetry
            failureResponseData.jsonResponse = jsonResponse ?: JSONObject()
        } else {
            // Rest of all platforms.
            failureResponseData.eventToken = eventToken
            failureResponseData.message = message
            failureResponseData.timestamp = timestamp
            failureResponseData.adid = adid
            failureResponseData.callbackId = callbackId
            failureResponseData.willRetry = willRetry
            failureResponseData.jsonResponse = jsonResponse
        }
        return failureResponseData
    }
}