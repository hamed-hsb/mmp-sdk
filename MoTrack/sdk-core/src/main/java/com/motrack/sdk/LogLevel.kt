package com.motrack.sdk

import android.util.Log

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

enum class LogLevel(private var androidLogLevel: Int) {

    VERBOSE(Log.VERBOSE),
    DEBUG(Log.DEBUG),
    INFO(Log.INFO),
    WARN(Log.WARN),
    ERROR(Log.ERROR),
    ASSERT(Log.ASSERT),
    SUPPRESS(8);

    fun getAndroidLogLevel(): Int {
        return androidLogLevel
    }
}