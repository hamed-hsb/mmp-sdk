package com.motrack.sdk

import org.json.JSONObject

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

open class ResponseData {
    var success = false
    var willRetry = false
    var adid: String? = null
    var message: String? = null
    var timestamp: String? = null
    var jsonResponse: JSONObject? = null
    var activityKind: ActivityKind? = null
    var trackingState: TrackingState? = null
    var attribution: MotrackAttribution? = null
    var askIn: Long? = null
    var retryIn: Long? = null
    var continueIn: Long? = null

    var activityPackage: ActivityPackage? = null
    lateinit var sendingParameters: Map<String?, String?>

    companion object {
        @JvmStatic
        fun buildResponseData(
            activityPackage: ActivityPackage,
            sendingParameters: Map<String?, String?>
        ): ResponseData {
            val responseData: ResponseData?
            val activityKind = activityPackage.getActivityKind()

            responseData = when (activityKind) {
                ActivityKind.SESSION -> SessionResponseData(activityPackage)
                ActivityKind.CLICK -> SdkClickResponseData()
                ActivityKind.ATTRIBUTION -> AttributionResponseData()
                ActivityKind.EVENT -> EventResponseData(activityPackage)
                else -> ResponseData()
            }
            responseData.activityKind = activityKind
            responseData.activityPackage = activityPackage
            responseData.sendingParameters = sendingParameters

            return responseData
        }
    }

    override fun toString(): String {
        return "message:$message timestamp:$timestamp json:$jsonResponse"
    }
}