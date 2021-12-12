package com.motrack.sdk

import java.util.*
import kotlin.collections.HashMap

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

class SessionParameters {
    var callbackParameters: HashMap<String, String>? = null
    var partnerParameters: HashMap<String, String>? = null

    fun deepCopy(): SessionParameters {
        val newSessionParameters = SessionParameters()
        if (callbackParameters != null) {
            newSessionParameters.callbackParameters = HashMap(callbackParameters!!)
        }
        if (partnerParameters != null) {
            newSessionParameters.partnerParameters = HashMap(partnerParameters!!)
        }
        return newSessionParameters
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SessionParameters

        if (callbackParameters != other.callbackParameters) return false
        if (partnerParameters != other.partnerParameters) return false

        return true
    }

    override fun hashCode(): Int {
        var result = callbackParameters?.hashCode() ?: 0
        result = 31 * result + (partnerParameters?.hashCode() ?: 0)
        return result
    }
}