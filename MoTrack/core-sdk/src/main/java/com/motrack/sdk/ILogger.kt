package com.motrack.sdk

interface ILogger {
    fun setLogLevel(logLevel: LogLevel, isProductionEnvironment: Boolean)

    fun setLogLevelString(logLevelString: String?, isProductionEnvironment: Boolean)

    fun verbose(message: String, vararg parameters: Any)

    fun debug(message: String, vararg parameters: Any)

    fun info(message: String, vararg parameters: Any)

    fun warn(message: String, vararg parameters: Any)
    fun warnInProduction(message: String, vararg parameters: Any)

    fun error(message: String, vararg parameters: Any)

    fun assert(message: String, vararg parameters: Any)

    fun lockLogLevel()
}