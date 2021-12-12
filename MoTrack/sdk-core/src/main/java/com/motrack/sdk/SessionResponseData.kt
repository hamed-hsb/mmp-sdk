package com.motrack.sdk

import org.json.JSONObject

/**
 * @author yaya (@yahyalmh)
 * @since 06th October 2021
 */

class SessionResponseData(activityPackage: ActivityPackage) : ResponseData() {

    private val sdkPlatform = activityPackage.getClientSdk()?.let { Util.getSdkPrefixPlatform(it) }

    public fun getSuccessResponseData(): MotrackSessionSuccess? {
        if (!success) {
            return null
        }

        val successResponseData = MotrackSessionSuccess()
        if ("unity" == sdkPlatform) {
            successResponseData.adid = adid ?: ""
            successResponseData.message = message ?: ""
            successResponseData.timestamp = timestamp ?: ""
            successResponseData.jsonResponse = jsonResponse ?: JSONObject()
        } else {
            // Rest of all platforms.
            successResponseData.message = message
            successResponseData.timestamp = timestamp
            successResponseData.adid = adid
            successResponseData.jsonResponse = jsonResponse
        }

        return successResponseData
    }

    fun getFailureResponseData(): MotrackSessionFailure? {
        if (success) {
            return null
        }
        val failureResponseData = MotrackSessionFailure()
        if ("unity" == sdkPlatform) {
            // Unity platform.
            failureResponseData.message = if (message != null) message else ""
            failureResponseData.timestamp = if (timestamp != null) timestamp else ""
            failureResponseData.adid = if (adid != null) adid else ""
            failureResponseData.willRetry = willRetry
            failureResponseData.jsonResponse =
                if (jsonResponse != null) jsonResponse else JSONObject()
        } else {
            // Rest of all platforms.
            failureResponseData.message = message
            failureResponseData.timestamp = timestamp
            failureResponseData.adid = adid
            failureResponseData.willRetry = willRetry
            failureResponseData.jsonResponse = jsonResponse
        }
        return failureResponseData
    }
}