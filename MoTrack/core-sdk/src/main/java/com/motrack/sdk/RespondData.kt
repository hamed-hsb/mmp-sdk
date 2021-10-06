package com.motrack.sdk

import org.json.JSONObject

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

open class RespondData {
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

    lateinit var activityPackage: ActivityPackage
    lateinit var sendingParameters: Map<String, String>

    companion object {
        @JvmStatic
        fun buildResponseData(
            activityPackage: ActivityPackage,
            sendingParameters: Map<String, String>
        ): RespondData {
            val respondData: RespondData?
            val activityKind = activityPackage.getActivityKind()

            respondData = when (activityKind) {
                ActivityKind.SESSION -> SessionResponseData(activityPackage)
                ActivityKind.CLICK -> SdkClickResponseData()
                ActivityKind.ATTRIBUTION -> AttributionResponseData()
                ActivityKind.EVENT -> EventResponseData(activityPackage)
                else -> RespondData()
            }
            respondData.activityKind = activityKind
            respondData.activityPackage = activityPackage
            respondData.sendingParameters = sendingParameters

            return respondData
        }
    }

    override fun toString(): String {
        return "message:$message timestamp:$timestamp json:$jsonResponse"
    }
}