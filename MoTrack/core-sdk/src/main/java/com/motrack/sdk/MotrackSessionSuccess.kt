package com.motrack.sdk

import org.json.JSONObject

/**
 * @author yaya (@yahyalmh)
 * @since 06th October 2021
 */

class MotrackSessionSuccess {

    var adid: String? = null
    var message: String? = null
    var timestamp: String? = null
    var jsonResponse: JSONObject? = null


    override fun toString(): String {
        return "MotrackSessionSuccess(adid=$adid, message=$message, timestamp=$timestamp, jsonResponse=$jsonResponse)"
    }
}