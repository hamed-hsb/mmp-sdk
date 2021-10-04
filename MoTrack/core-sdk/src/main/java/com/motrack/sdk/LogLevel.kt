package com.motrack.sdk

import android.util.Log

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