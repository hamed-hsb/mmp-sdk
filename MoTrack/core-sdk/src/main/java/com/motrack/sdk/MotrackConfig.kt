package com.motrack.sdk

import android.content.Context

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

class MotrackConfig {
    private var preinstallTrackingEnabled: Boolean = false
    private var sendInBackground: Boolean = false
    private var eventBufferingEnabled: Boolean = false
    private lateinit var logger: ILogger
    private var context: Context? = null
    private var appToken: String? = null
    private lateinit var environment: String

    companion object {
        const val ENVIRONMENT_SANDBOX = "sandbox"
        const val ENVIRONMENT_PRODUCTION = "production"
    }

    private fun init(
        context: Context?,
        appToken: String?,
        environment: String,
        allowSuppressLogLevel: Boolean
    ) {
        logger = MotrackFactory.getLogger()
        if (allowSuppressLogLevel && ENVIRONMENT_PRODUCTION == environment) {
            setLogLevel(LogLevel.SUPPRESS, environment)
        } else {
            setLogLevel(LogLevel.INFO, environment)
        }

        var context = context
        if (context != null) {
            context = context.applicationContext
        }

        this.context = context
        this.appToken = appToken
        this.environment = environment
    }

    public fun isValid(): Boolean {
        if (!checkAppToken(appToken)) return false
        if (!checkContext(context)) return false
        if (!checkEnvironment(environment)) return false

        return true
    }

    private fun checkAppToken(appToken: String?): Boolean {
        if (appToken.isNullOrEmpty()) {
            logger.error("Missing App Token")
            return false
        }

        if (appToken.length != DefaultConfig.APP_TOKEN_LENGTH) {
            logger.error("Malformed App Token: $appToken")
            return false
        }
        return true
    }

    private fun checkContext(context: Context?): Boolean {
        if (context == null) {
            logger.error("Missing Context")
            return false
        }

        if (!Util.checkPermission(context, android.Manifest.permission.INTERNET)) {
            logger.error("Missing Permission: ${android.Manifest.permission.INTERNET}")
            return false
        }
        return true
    }

    private fun checkEnvironment(environment: String): Boolean {
        if (environment.isNullOrEmpty()) {
            logger.error("Missing Environment")
            return false
        }
        if (environment == ENVIRONMENT_SANDBOX) {
            logger.warnInProduction(
                "SANDBOX: Motrack is running in Sandbox mode. " +
                        "Use this setting for testing. " +
                        "Don't forget to set the environment to `production` before publishing!"
            )
            return true
        }
        if (environment == ENVIRONMENT_PRODUCTION) {
            logger.warnInProduction(
                "PRODUCTION: Motrack is running in Production mode. " +
                        "Use this setting only for the build that you want to publish. " +
                        "Set the environment to `sandbox` if you want to test your app!"
            )
            return true
        }

        logger.error(
            "Unknown Environment $environment" +
                    "The environment can only be $ENVIRONMENT_SANDBOX for testing or $ENVIRONMENT_PRODUCTION for publishing"
        )
        return false
    }

    private fun setLogLevel(logLevel: LogLevel, environment: String) {
        logger.setLogLevel(logLevel, ENVIRONMENT_PRODUCTION == environment)
    }
}