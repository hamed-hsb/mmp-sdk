package com.motrack.sdk

import android.content.Context

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

class MotrackConfig {
    var subscriptionPath: String? = null
    var dgprPath: String? = null
    var basePath: String? = null
    var startOffline = false
    var startEnabled: Boolean? = false
    var processName: String? = null
    var defaultTracker: String? = null
    var sdkPrefix: String? = null

    var pushToken: String? = null
    var delayStart: Double? = 0.0
    var urlStrategy: String? = null
    var gdprPath: String? = null
    var preinstallFilePath: String? = null

    var preinstallTrackingEnabled: Boolean = false
    var sendInBackground: Boolean = false
    var eventBufferingEnabled: Boolean = false
    private lateinit var logger: ILogger
     var context: Context? = null

    var appToken: String? = null
    lateinit var environment: String

    var externalDeviceId: String? = null
    var secretId: String? = null
    var appSecret: String? = null
    var needsCost: Boolean? = null
    var deviceKnown: Boolean? = null

    var userAgent: String? = null

    var onAttributionChangedListener: OnAttributionChangedListener? = null
    var onEventTrackingSucceededListener: OnEventTrackingSucceededListener? = null
    var onEventTrackingFailedListener: OnEventTrackingFailedListener? = null
    public var onDeeplinkResponseListener: OnDeeplinkResponseListener? = null
    var onSessionTrackingFailedListener: OnSessionTrackingFailedListener? = null
    var onSessionTrackingSucceededListener: OnSessionTrackingSucceededListener? = null


    var deepLinkComponent: Class<*>? = null

    var playStoreKidsAppEnabled: Boolean? = null
    var coppaCompliantEnabled: Boolean? = null


    public var preLaunchActions: MotrackInstance.Companion.PreLaunchActions? = null

    companion object {
        const val ENVIRONMENT_SANDBOX = "sandbox"
        const val ENVIRONMENT_PRODUCTION = "production"

        const val URL_STRATEGY_INDIA = "url_strategy_india"
        const val URL_STRATEGY_CHINA = "url_strategy_china"
        const val DATA_RESIDENCY_EU = "data_residency_eu"
        const val DATA_RESIDENCY_TR = "data_residency_tr"
        const val DATA_RESIDENCY_US = "data_residency_us"


        const val AD_REVENUE_APPLOVIN_MAX = "applovin_max_sdk"
        const val AD_REVENUE_MOPUB = "mopub"
        const val AD_REVENUE_ADMOB = "admob_sdk"
        const val AD_REVENUE_IRONSOURCE = "ironsource_sdk"
        const val AD_REVENUE_ADMOST = "admost_sdk"
        const val AD_REVENUE_UNITY = "unity_sdk"
        const val AD_REVENUE_HELIUM_CHARTBOOST = "helium_chartboost_sdk"
        const val AD_REVENUE_SOURCE_PUBLISHER = "publisher_sdk"
    }

    constructor(context: Context?, appToken: String?, environment: String) {
        init(context, appToken, environment, false)
    }

    constructor(
        context: Context?,
        appToken: String?,
        environment: String,
        allowSuppressLogLevel: Boolean
    ) {
        init(context, appToken, environment, allowSuppressLogLevel)
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
            setLogLevel(LogLevel.VERBOSE, environment)
        }

        var context = context
        if (context != null) {
            context = context.applicationContext
        }

        this.context = context
        this.appToken = appToken
        this.environment = environment

        // default values

        // default values
        eventBufferingEnabled = false
        sendInBackground = false
        preinstallTrackingEnabled = false
    }


    fun setPlayStoreKidsAppEnabled(playStoreKidsAppEnabled: Boolean) {
        this.playStoreKidsAppEnabled = playStoreKidsAppEnabled
    }

    fun setCoppaCompliantEnabled(coppaCompliantEnabled: Boolean) {
        this.coppaCompliantEnabled = coppaCompliantEnabled
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


    fun setAppSecret(secretId: Long, info1: Long, info2: Long, info3: Long, info4: Long) {
        this.secretId = String.format("%d", secretId)
        this.appSecret = String.format("%d%d%d%d", info1, info2, info3, info4)
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

    fun setEventBufferingEnabled(eventBufferingEnabled: Boolean?) {
        if (eventBufferingEnabled == null) {
            this.eventBufferingEnabled = false
            return
        }
        this.eventBufferingEnabled = eventBufferingEnabled
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

    @JvmName("setUrlStrategy1")
    fun setUrlStrategy(urlStrategy: String?) {
        if (urlStrategy == null || urlStrategy.isEmpty()) {
            logger.error("Invalid url strategy")
            return
        }
        if (urlStrategy != URL_STRATEGY_INDIA
            && urlStrategy != URL_STRATEGY_CHINA
            && urlStrategy != DATA_RESIDENCY_EU
            && urlStrategy != DATA_RESIDENCY_TR
            && urlStrategy != DATA_RESIDENCY_US
        ) {
            logger.warn("Unrecognised url strategy $urlStrategy")
        }
        this.urlStrategy = urlStrategy
    }

    @JvmName("getUrlStrategy1")
    fun getUrlStrategy(): String? {
        return urlStrategy
    }

    fun setLogLevel(logLevel: LogLevel, environment: String) {
        logger.setLogLevel(logLevel, ENVIRONMENT_PRODUCTION == environment)
    }

    fun setLogLevel(logLevel: LogLevel) {
        logger.setLogLevel(logLevel, true)
    }

}