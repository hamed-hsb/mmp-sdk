package com.motrack.sdk

import android.util.Log
import com.motrack.sdk.Constants.Companion.LOGTAG

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

class Logger : ILogger {

    private lateinit var logLevel: LogLevel
    private var logLevelLocked: Boolean = false
    private var isProductionEnvironment: Boolean = false

    init {
        setLogLevel(LogLevel.INFO, isProductionEnvironment)
    }

    override fun setLogLevel(logLevel: LogLevel, isProductionEnvironment: Boolean) {
        if (logLevelLocked) {
            return
        }
        this.logLevel = logLevel
        this.isProductionEnvironment = isProductionEnvironment
    }

    override fun setLogLevelString(logLevelString: String?, isProductionEnvironment: Boolean) {
        if (logLevelString != null) {
            try {
                setLogLevel(LogLevel.valueOf(logLevelString.uppercase()), isProductionEnvironment)
            } catch (e: IllegalArgumentException) {
                error("Malformed logLevel $logLevelString, fallback to ${LogLevel.INFO}")
            }
        }
    }

    override fun verbose(message: String, vararg parameters: Any) {
        if (isProductionEnvironment) {
            return
        }
        if (logLevel.getAndroidLogLevel() <= Log.VERBOSE) {
            try {
                Log.v(LOGTAG, message)
            } catch (e: Exception) {
                Log.e(
                    LOGTAG,
                    "Error formatting log message: $message, with params: ${parameters.toList()}"
                )
            }
        }
    }

    override fun debug(message: String, vararg parameters: Any) {
        if (isProductionEnvironment) {
            return
        }
        if (logLevel.getAndroidLogLevel() <= Log.DEBUG) {
            try {
                Log.d(LOGTAG, message)
            } catch (e: Exception) {
                Log.e(
                    LOGTAG,
                    "Error formatting log message: $message, with params: ${parameters.toList()}"
                )
            }
        }
    }

    override fun info(message: String, vararg parameters: Any) {
        if (isProductionEnvironment) {
            return
        }
        if (logLevel.getAndroidLogLevel() <= Log.INFO) {
            try {
                Log.i(LOGTAG, message)
            } catch (e: Exception) {
                Log.e(
                    LOGTAG,
                    "Error formatting log message: $message, with params: ${parameters.toList()}"
                )
            }
        }
    }

    override fun warn(message: String, vararg parameters: Any) {
        if (isProductionEnvironment) {
            return
        }
        if (logLevel.getAndroidLogLevel() <= Log.WARN) {
            try {
                Log.w(LOGTAG, message)
            } catch (e: Exception) {
                Log.e(
                    LOGTAG,
                    "Error formatting log message: $message, with params: ${parameters.toList()}"
                )
            }
        }
    }

    override fun warnInProduction(message: String, vararg parameters: Any) {
        if (logLevel.getAndroidLogLevel() <= Log.WARN) {
            try {
                Log.w(LOGTAG, message)
            } catch (e: Exception) {
                Log.e(
                    LOGTAG,
                    "Error formatting log message: $message, with params: ${parameters.toList()}"
                )
            }
        }
    }

    override fun error(message: String, vararg parameters: Any) {
        if (isProductionEnvironment) {
            return
        }
        if (logLevel.getAndroidLogLevel() <= Log.ERROR) {
            try {
                Log.e(LOGTAG, message)
            } catch (e: Exception) {
                Log.e(
                    LOGTAG,
                    "Error formatting log message: $message, with params: ${parameters.toList()}"
                )
            }
        }
    }

    override fun assert(message: String, vararg parameters: Any) {
        if (isProductionEnvironment) {
            return
        }
        if (logLevel.getAndroidLogLevel() <= Log.ASSERT) {
            try {
                Log.println(Log.ASSERT, LOGTAG, message)
            } catch (e: Exception) {
                Log.e(
                    LOGTAG,
                    "Error formatting log message: $message, with params: ${parameters.toList()}"
                )
            }
        }
    }

    override fun lockLogLevel() {
        logLevelLocked = true
    }
}