package com.motrack.sdk.imei

import android.content.Context
import com.motrack.sdk.ILogger
import java.util.HashMap

/**
 * @author yaya (@yahyalmh)
 * @since 07th December 2021
 */

class Util {
    fun getImeiParameters(context: Context, logger: ILogger): Map<String, String> {
        val parameters: HashMap<String, String> = HashMap()
        TelephonyIdsUtil.injectImei(parameters, context, logger)
        return parameters
    }
}