package com.motrack.sdk

import org.json.JSONObject

/**
 * @author yaya (@yahyalmh)
 * @since 06th October 2021
 */

class MotrackEventFailure {
    var willRetry = false
    var adid: String? = null
    var message: String? = null
    var timestamp: String? = null
    var eventToken: String? = null
    var callbackId: String? = null
    var jsonResponse: JSONObject? = null

    override fun toString(): String {
        return "MotrackEventFailure(willRetry=$willRetry, adid=$adid, message=$message, timestamp=$timestamp, eventToken=$eventToken, callbackId=$callbackId, jsonResponse=$jsonResponse)"
    }
}