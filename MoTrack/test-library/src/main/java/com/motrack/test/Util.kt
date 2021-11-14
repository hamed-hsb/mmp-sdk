package com.motrack.test

import android.util.Log
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 14th November 2021
 */

class Util {
    companion object {
        fun debug(message: String?, vararg parameters: Any?) {
            try {
                Log.d(Constants.LOGTAG, String.format(Locale.US, message!!, *parameters))
            } catch (e: Exception) {
                Log.e(
                    Constants.LOGTAG, String.format(
                        Locale.US,
                        "Error formatting log message: $message, with params: ${parameters.contentToString()}",
                    )
                )
            }
        }
    }
}