package com.motrack.test

import android.util.Log
import com.motrack.test.Constants.Companion.LOGTAG
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 14th November 2021
 */

class Util {
    companion object {
        fun debug(message: String, vararg parameters: Any?) {
            try {
                Log.d(LOGTAG, String.format(Locale.US, message, *parameters))
            } catch (e: Exception) {
                Log.e(
                    LOGTAG, String.format(
                        Locale.US,
                        "Error formatting log message: $message, with params: ${parameters.contentToString()}",
                    )
                )
            }
        }

        fun error(message: String, vararg parameters: Any?) {
            try {
                Log.e(LOGTAG, String.format(Locale.US, message, *parameters))
            } catch (e: java.lang.Exception) {
                Log.e(
                    LOGTAG, String.format(
                        Locale.US,
                        "Error formatting log message: $message, with params: ${parameters.contentToString()}",
                    )
                )
            }
        }

        fun appendBasePath(basePath: String?, path: String): String {
            return if (basePath == null) {
                path
            } else basePath + path
        }
    }
}