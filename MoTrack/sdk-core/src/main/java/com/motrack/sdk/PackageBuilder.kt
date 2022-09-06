package com.motrack.sdk

import android.content.ContentResolver
import org.json.JSONObject
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 11th October 2021
 */

class PackageBuilder(
    private val motrackConfig: MotrackConfig,
    private val deviceInfo: DeviceInfo,
    activityState: ActivityState?,
    private val sessionParameters: SessionParameters?,
    private val createdAt: Long
) {

    private val logger: ILogger = MotrackFactory.getLogger()
    private var activityStateCopy: ActivityStateCopy? = null

    var clickTimeInSeconds: Long = -1
    var clickTimeInMilliseconds: Long = -1
    var installBeginTimeInSeconds: Long = -1
    var clickTimeServerInSeconds: Long = -1
    var installBeginTimeServerInSeconds: Long = -1
    var reftag: String? = null
    var deeplink: String? = null
    var referrer: String? = null
    var installVersion: String? = null
    var rawReferrer: String? = null
    var referrerApi: String? = null
    var preinstallPayload: String? = null
    var preinstallLocation: String? = null
    var googlePlayInstant: Boolean? = null
    var attribution: MotrackAttribution? = null
    var extraParameters: Map<String, String>? = null

    init {
        activityStateCopy = ActivityStateCopy(activityState)
    }

    fun buildSessionPackage(isInDelay: Boolean): ActivityPackage {
        val parameters: HashMap<String, String> = getSessionParameters(isInDelay)
        val sessionPackage: ActivityPackage = getDefaultActivityPackage(ActivityKind.SESSION)
        sessionPackage.path = "/sessions"
        sessionPackage.suffix = ""
        MotrackSigner.sign(
            parameters, ActivityKind.SESSION.toString(),
            sessionPackage.clientSdk, motrackConfig.context, logger
        )
        sessionPackage.parameters = parameters
        return sessionPackage
    }

    fun buildEventPackage(event: MotrackEvent, isInDelay: Boolean): ActivityPackage {
        val parameters: HashMap<String, String> = getEventParameters(event, isInDelay)
        val eventPackage = getDefaultActivityPackage(ActivityKind.EVENT)
        eventPackage.path = "/events"
        eventPackage.suffix = getEventSuffix(event)
        MotrackSigner.sign(
            parameters, ActivityKind.EVENT.toString(),
            eventPackage.clientSdk, motrackConfig.context!!, logger
        )
        eventPackage.parameters = parameters
        if (isInDelay) {
            eventPackage.callbackParameters = event.callbackParameters
            eventPackage.partnerParameters = event.partnerParameters
        }
        return eventPackage
    }

    fun buildInfoPackage(source: String?): ActivityPackage {
        val parameters: HashMap<String, String> = getInfoParameters(source!!)
        val infoPackage = getDefaultActivityPackage(ActivityKind.INFO)
        infoPackage.path = "/sdk_info"
        infoPackage.suffix = ""
        MotrackSigner.sign(
            parameters, ActivityKind.INFO.toString(),
            infoPackage.clientSdk, motrackConfig.context!!, logger
        )
        infoPackage.parameters = parameters
        return infoPackage
    }

    fun buildClickPackage(source: String?): ActivityPackage {
        val parameters: HashMap<String, String> = getClickParameters(source!!)
        val clickPackage = getDefaultActivityPackage(ActivityKind.CLICK)
        clickPackage.path = "/sdk_clicks"
        clickPackage.suffix = ""
        clickPackage.clickTimeInMilliseconds = clickTimeInMilliseconds
        clickPackage.clickTimeInSeconds = clickTimeInSeconds
        clickPackage.installBeginTimeInSeconds = installBeginTimeInSeconds
        clickPackage.clickTimeServerInSeconds = clickTimeServerInSeconds
        clickPackage.installBeginTimeServerInSeconds = installBeginTimeServerInSeconds
        clickPackage.installVersion = installVersion
        clickPackage.googlePlayInstant = googlePlayInstant
        MotrackSigner.sign(
            parameters, ActivityKind.CLICK.toString(),
            clickPackage.clientSdk, motrackConfig.context!!, logger
        )
        clickPackage.parameters = parameters
        return clickPackage
    }

    fun buildAttributionPackage(initiatedByDescription: String?): ActivityPackage {
        val parameters: HashMap<String, String> = getAttributionParameters(
            initiatedByDescription!!
        )
        val attributionPackage = getDefaultActivityPackage(ActivityKind.ATTRIBUTION)
        attributionPackage.path =
            "/attributions" // does not contain '/' because of Uri.Builder.appendPath
        attributionPackage.suffix = ""
        MotrackSigner.sign(
            parameters, ActivityKind.ATTRIBUTION.toString(),
            attributionPackage.clientSdk, motrackConfig.context!!, logger
        )
        attributionPackage.parameters = parameters
        return attributionPackage
    }

    fun buildGdprPackage(): ActivityPackage {
        val parameters: HashMap<String, String> = getGdprParameters()
        val gdprPackage = getDefaultActivityPackage(ActivityKind.GDPR)
        gdprPackage.path = "/gdpr_forget_device"
        gdprPackage.suffix = ""
        MotrackSigner.sign(
            parameters, ActivityKind.GDPR.toString(),
            gdprPackage.clientSdk, motrackConfig.context!!, logger
        )
        gdprPackage.parameters = parameters
        return gdprPackage
    }

    fun buildDisableThirdPartySharingPackage(): ActivityPackage {
        val parameters: HashMap<String, String> = getDisableThirdPartySharingParameters()
        val activityPackage = getDefaultActivityPackage(ActivityKind.DISABLE_THIRD_PARTY_SHARING)
        activityPackage.path = "/disable_third_party_sharing"
        activityPackage.suffix = ""
        MotrackSigner.sign(
            parameters, ActivityKind.DISABLE_THIRD_PARTY_SHARING.toString(),
            activityPackage.clientSdk, motrackConfig.context!!, logger
        )
        activityPackage.parameters = parameters
        return activityPackage
    }

    fun buildThirdPartySharingPackage(
        motrackThirdPartySharing: MotrackThirdPartySharing
    ): ActivityPackage {
        val parameters: HashMap<String, String> =
            getThirdPartySharingParameters(motrackThirdPartySharing)
        val activityPackage = getDefaultActivityPackage(ActivityKind.THIRD_PARTY_SHARING)
        activityPackage.path = "/third_party_sharing"
        activityPackage.suffix = ""
        MotrackSigner.sign(
            parameters, ActivityKind.THIRD_PARTY_SHARING.toString(),
            activityPackage.clientSdk, motrackConfig.context!!, logger
        )
        activityPackage.parameters = parameters
        return activityPackage
    }

    fun buildMeasurementConsentPackage(consentMeasurement: Boolean): ActivityPackage {
        val parameters: HashMap<String, String> =
            getMeasurementConsentParameters(consentMeasurement)
        val activityPackage = getDefaultActivityPackage(ActivityKind.MEASUREMENT_CONSENT)
        activityPackage.path = "/measurement_consent"
        activityPackage.suffix = ""
        MotrackSigner.sign(
            parameters, ActivityKind.MEASUREMENT_CONSENT.toString(),
            activityPackage.clientSdk, motrackConfig.context!!, logger
        )
        activityPackage.parameters = parameters
        return activityPackage
    }

    fun buildAdRevenuePackage(source: String?, adRevenueJson: JSONObject?): ActivityPackage {
        val parameters: HashMap<String, String> = getAdRevenueParameters(
            source!!, adRevenueJson!!
        )
        val adRevenuePackage = getDefaultActivityPackage(ActivityKind.AD_REVENUE)
        adRevenuePackage.path = "/ad_revenue"
        adRevenuePackage.suffix = ""
        MotrackSigner.sign(
            parameters, ActivityKind.AD_REVENUE.toString(),
            adRevenuePackage.clientSdk, motrackConfig.context!!, logger
        )
        adRevenuePackage.parameters = parameters
        return adRevenuePackage
    }

    fun buildAdRevenuePackage(
        motrackAdRevenue: MotrackAdRevenue,
        isInDelay: Boolean
    ): ActivityPackage {
        val parameters: HashMap<String, String> =
            getAdRevenueParameters(motrackAdRevenue, isInDelay)
        val adRevenuePackage = getDefaultActivityPackage(ActivityKind.AD_REVENUE)
        adRevenuePackage.path = "/ad_revenue"
        adRevenuePackage.suffix = ""
        MotrackSigner.sign(
            parameters, ActivityKind.AD_REVENUE.toString(),
            adRevenuePackage.clientSdk, motrackConfig.context!!, logger
        )
        adRevenuePackage.parameters = parameters
        if (isInDelay) {
            adRevenuePackage.callbackParameters = motrackAdRevenue.callbackParameters
            adRevenuePackage.partnerParameters = motrackAdRevenue.partnerParameters
        }
        return adRevenuePackage
    }

    fun buildSubscriptionPackage(
        subscription: MotrackPlayStoreSubscription,
        isInDelay: Boolean
    ): ActivityPackage {
        val parameters: HashMap<String, String> = getSubscriptionParameters(subscription, isInDelay)
        val subscriptionPackage = getDefaultActivityPackage(ActivityKind.SUBSCRIPTION)
        subscriptionPackage.path = "/v2/purchase"
        subscriptionPackage.suffix = ""
        MotrackSigner.sign(
            parameters, ActivityKind.SUBSCRIPTION.toString(),
            subscriptionPackage.clientSdk, motrackConfig.context!!, logger
        )
        subscriptionPackage.parameters = parameters
        return subscriptionPackage
    }

    private class ActivityStateCopy {
        var eventCount = -1
        var sessionCount = -1
        var subsessionCount = -1
        var timeSpent: Long = -1
        var lastInterval: Long = -1
        var sessionLength: Long = -1
        var uuid: String? = null
        var pushToken: String? = null

        constructor(activityState: ActivityState?) {
            if (activityState == null) {
                return
            }
            eventCount = activityState.eventCount
            sessionCount = activityState.sessionCount
            subsessionCount = activityState.subsessionCount
            timeSpent = activityState.timeSpent
            lastInterval = activityState.lastInterval
            sessionLength = activityState.sessionLength
            uuid = activityState.uuid
            pushToken = activityState.pushToken
        }
    }

    private fun getSessionParameters(isInDelay: Boolean): HashMap<String, String> {
        val contentResolver: ContentResolver = motrackConfig.context!!.contentResolver
        val parameters: HashMap<String, String> = HashMap()
        val session: HashMap<String, String> = HashMap()
        val imeiParameters =
            Util.getImeiParameters(motrackConfig, logger)





        // Check if plugin is used and if yes, add read parameters.
        if (imeiParameters != null) {
            parameters.putAll(imeiParameters)
        }

        // Check if oaid plugin is used and if yes, add the parameter
        val oaidParameters =
           Util.getOaidParameters(motrackConfig, logger)
        if (oaidParameters != null) {
            parameters.putAll(oaidParameters)
        }

        // Callback and partner parameters.
        if (!isInDelay) {
            addMapJson(
                parameters,
                "callback_params",
                sessionParameters!!.callbackParameters
            )
            addMapJson(
                parameters,
                "partner_params",
                sessionParameters.partnerParameters
            )
        }

        // Device identifiers.
        deviceInfo.reloadPlayIds(motrackConfig)

        addBoolean(session, "needs_response_details", true)

        addString(parameters, "android_uuid", activityStateCopy!!.uuid)
        addString(parameters, "gps_adid", deviceInfo.playAdId)
        addLong(parameters, "gps_adid_attempt", deviceInfo.playAdIdAttempt.toLong())
        addString(parameters, "gps_adid_src", deviceInfo.playAdIdSource)
        addBoolean(parameters, "tracking_enabled", deviceInfo.isTrackingEnabled)
        addString(
            parameters,
            "fire_adid",
            Util.getFireAdvertisingId(motrackConfig.context!!.contentResolver)
        )
        addBoolean(
            parameters,
            "fire_tracking_enabled",
          Util.getFireTrackingEnabled(motrackConfig.context!!.contentResolver)
        )
        if (!containsPlayIds(parameters) && !containsFireIds(parameters)) {
            logger.warn(
                "Google Advertising ID or Fire Advertising ID not detected, " +
                        "fallback to non Google Play and Fire identifiers will take place"
            )
            deviceInfo.reloadNonPlayIds(motrackConfig);
            addString(parameters, "android_id", deviceInfo.androidId)
        }

        // Rest of the parameters.
        addString(parameters, "api_level", deviceInfo.apiLevel)
        addString(parameters, "app_secret", motrackConfig.appSecret)
        addString(parameters, "app_token", motrackConfig.appToken)
        addString(parameters, "app_version", deviceInfo.appVersion)
        addBoolean(parameters, "attribution_deeplink", true)
        addLong(
            parameters,
            "connectivity_type",
            AndroidUtil.getConnectivityType(motrackConfig.context!!).toLong()
        )
        addString(parameters, "country", deviceInfo.country)
        addString(parameters, "cpu_type", deviceInfo.abi)
        addDateInMilliseconds(parameters, "created_at", createdAt)
        addString(parameters, "default_tracker", motrackConfig.defaultTracker)
        addBoolean(parameters, "device_known", motrackConfig.deviceKnown)
        addBoolean(parameters, "needs_cost", motrackConfig.needsCost)
        addString(parameters, "device_manufacturer", deviceInfo.deviceManufacturer)
        addString(parameters, "device_name", deviceInfo.deviceName)
        addString(parameters, "device_type", deviceInfo.deviceType)
        addLong(parameters, "ui_mode", deviceInfo.uiMode?.toLong())
        addString(parameters, "display_height", deviceInfo.displayHeight)
        addString(parameters, "display_width", deviceInfo.displayWidth)
        addString(parameters, "environment", motrackConfig.environment)
        addBoolean(
            parameters,
            "event_buffering_enabled",
            motrackConfig.eventBufferingEnabled
        )
        addString(parameters, "external_device_id", motrackConfig.externalDeviceId)
        addString(parameters, "fb_id", deviceInfo.fbAttributionId)
        addString(parameters, "hardware_name", deviceInfo.hardwareName)
        addString(parameters, "installed_at", deviceInfo.appInstallTime)
        addString(parameters, "language", deviceInfo.language)
        addDuration(parameters, "last_interval", activityStateCopy!!.lastInterval)
        addString(parameters, "mcc", AndroidUtil.getMcc(motrackConfig.context!!))
        addString(parameters, "mnc", AndroidUtil.getMnc(motrackConfig.context!!))


        addString(parameters, "os_build", deviceInfo.buildName)
        addString(parameters, "os_name", deviceInfo.osName)
        addString(parameters, "os_version", deviceInfo.osVersion)
        addString(parameters, "package_name", deviceInfo.packageName)
        addString(parameters, "push_token", activityStateCopy!!.pushToken)
        addString(parameters, "screen_density", deviceInfo.screenDensity)
        addString(parameters, "screen_format", deviceInfo.screenFormat)
        addString(parameters, "screen_size", deviceInfo.screenSize)
        addString(parameters, "secret_id", motrackConfig.secretId)
        addLong(
            parameters,
            "session_count",
            activityStateCopy!!.sessionCount.toLong()
        )
        addDuration(parameters, "session_length", activityStateCopy!!.sessionLength)
        addLong(
            parameters,
            "subsession_count",
            activityStateCopy!!.subsessionCount.toLong()
        )
        addDuration(parameters, "time_spent", activityStateCopy!!.timeSpent)
        addString(parameters, "updated_at", deviceInfo.appUpdateTime)

        addMapJson(session,"parameters",parameters)
        checkDeviceIds(session)
        return session
    }

    fun getEventParameters(event: MotrackEvent, isInDelay: Boolean): HashMap<String, String> {
        val contentResolver: ContentResolver = motrackConfig.context!!.contentResolver
        val parameters: HashMap<String, String> = HashMap()
        val eventParameters: HashMap<String, String> = HashMap()
        val imeiParameters =
            Util.getImeiParameters(motrackConfig, logger)

        // Check if plugin is used and if yes, add read parameters.
        if (imeiParameters != null) {
            parameters.putAll(imeiParameters)
        }

        // Check if oaid plugin is used and if yes, add the parameter
        val oaidParameters =
            Util.getOaidParameters(motrackConfig, logger)
        if (oaidParameters != null) {
            parameters.putAll(oaidParameters)
        }

        // Callback and partner parameters.
        if (!isInDelay) {
            addMapJson(
                parameters, "callback_params", Util.mergeParameters(
                    sessionParameters!!.callbackParameters, event.callbackParameters, "Callback"
                )
            )
            addMapJson(
                parameters, "partner_params", Util.mergeParameters(
                    sessionParameters.partnerParameters, event.partnerParameters, "Partner"
                )
            )
        }

        // Device identifiers.
        deviceInfo.reloadPlayIds(motrackConfig)

        addBoolean(eventParameters, "needs_response_details", true)

        addString(parameters, "android_uuid", activityStateCopy!!.uuid)
        addString(parameters, "gps_adid", deviceInfo.playAdId)
        addLong(parameters, "gps_adid_attempt", deviceInfo.playAdIdAttempt.toLong())
        addString(parameters, "gps_adid_src", deviceInfo.playAdIdSource)
        addBoolean(parameters, "tracking_enabled", deviceInfo.isTrackingEnabled)
        addString(
            parameters,
            "fire_adid",
            Util.getFireAdvertisingId(motrackConfig.context!!.contentResolver)
        )
        addBoolean(
            parameters,
            "fire_tracking_enabled",
            Util.getFireTrackingEnabled(motrackConfig.context!!.contentResolver)
        )
        if (!containsPlayIds(parameters) && !containsFireIds(parameters)) {
            logger.warn(
                "Google Advertising ID or Fire Advertising ID not detected, " +
                        "fallback to non Google Play and Fire identifiers will take place"
            )
            deviceInfo.reloadNonPlayIds(motrackConfig);
            addString(parameters, "android_id", deviceInfo.androidId)
        }

        // Rest of the parameters.
        addString(parameters, "api_level", deviceInfo.apiLevel)
        addString(parameters, "app_secret", motrackConfig.appSecret)
        addString(parameters, "app_token", motrackConfig.appToken)
        addString(parameters, "app_version", deviceInfo.appVersion)
        addBoolean(parameters, "attribution_deeplink", true)
        addLong(
            parameters,
            "connectivity_type",
            AndroidUtil.getConnectivityType(motrackConfig.context!!).toLong()
        )
        addString(parameters, "country", deviceInfo.country)
        addString(parameters, "cpu_type", deviceInfo.abi)
        addDateInMilliseconds(parameters, "created_at", createdAt)
        addString(parameters, "currency", event.currency)
        addBoolean(parameters, "device_known", motrackConfig.deviceKnown)
        addBoolean(parameters, "needs_cost", motrackConfig.needsCost)
        addString(parameters, "device_manufacturer", deviceInfo.deviceManufacturer)
        addString(parameters, "device_name", deviceInfo.deviceName)
        addString(parameters, "device_type", deviceInfo.deviceType)
        addLong(parameters, "ui_mode", deviceInfo.uiMode?.toLong())
        addString(parameters, "display_height", deviceInfo.displayHeight)
        addString(parameters, "display_width", deviceInfo.displayWidth)
        addString(parameters, "environment", motrackConfig.environment)
        addString(parameters, "event_callback_id", event.callbackId)
        addLong(parameters, "event_count", activityStateCopy!!.eventCount.toLong())
        addBoolean(
            parameters,
            "event_buffering_enabled",
            motrackConfig.eventBufferingEnabled
        )
        addString(parameters, "event_token", event.eventToken)
        addString(parameters, "external_device_id", motrackConfig.externalDeviceId)
        addString(parameters, "fb_id", deviceInfo.fbAttributionId)
        addString(parameters, "hardware_name", deviceInfo.hardwareName)
        addString(parameters, "language", deviceInfo.language)
        addString(parameters, "mcc", AndroidUtil.getMcc(motrackConfig.context!!))
        addString(parameters, "mnc", AndroidUtil.getMnc(motrackConfig.context!!))


        addString(parameters, "os_build", deviceInfo.buildName)
        addString(parameters, "os_name", deviceInfo.osName)
        addString(parameters, "os_version", deviceInfo.osVersion)
        addString(parameters, "package_name", deviceInfo.packageName)
        addString(parameters, "push_token", activityStateCopy!!.pushToken)
        addDouble(parameters, "revenue", event.revenue)
        addString(parameters, "deduplication_id", event.orderId);
        addString(parameters, "screen_density", deviceInfo.screenDensity)
        addString(parameters, "screen_format", deviceInfo.screenFormat)
        addString(parameters, "screen_size", deviceInfo.screenSize)
        addString(parameters, "secret_id", motrackConfig.secretId)
        addLong(
            parameters,
            "session_count",
            activityStateCopy!!.sessionCount.toLong()
        )
        addDuration(parameters, "session_length", activityStateCopy!!.sessionLength)
        addLong(
            parameters,
            "subsession_count",
            activityStateCopy!!.subsessionCount.toLong()
        )
        addDuration(parameters, "time_spent", activityStateCopy!!.timeSpent)
        addMapJson(eventParameters,"parameters",parameters)
        checkDeviceIds(eventParameters)
        return eventParameters
    }

    private fun getInfoParameters(source: String): HashMap<String, String> {
        val contentResolver: ContentResolver = motrackConfig.context!!.contentResolver
        val parameters: HashMap<String, String> = HashMap()
        val imeiParameters =
            Util.getImeiParameters(motrackConfig, logger)

        // Check if plugin is used and if yes, add read parameters.
        if (imeiParameters != null) {
            parameters.putAll(imeiParameters)
        }

        // Check if oaid plugin is used and if yes, add the parameter
        val oaidParameters =
           Util.getOaidParameters(motrackConfig, logger)
        if (oaidParameters != null) {
            parameters.putAll(oaidParameters)
        }

        // Device identifiers.
        deviceInfo.reloadPlayIds(motrackConfig)
        addString(parameters, "android_uuid", activityStateCopy!!.uuid)
        addString(parameters, "gps_adid", deviceInfo.playAdId)
        addLong(parameters, "gps_adid_attempt", deviceInfo.playAdIdAttempt.toLong())
        addString(parameters, "gps_adid_src", deviceInfo.playAdIdSource)
        addBoolean(parameters, "tracking_enabled", deviceInfo.isTrackingEnabled)
        addString(
            parameters,
            "fire_adid",
            Util.getFireAdvertisingId(motrackConfig.context!!.contentResolver)
        )
        addBoolean(
            parameters,
            "fire_tracking_enabled",
           Util.getFireTrackingEnabled(motrackConfig.context!!.contentResolver)
        )
        if (!containsPlayIds(parameters) && !containsFireIds(parameters)) {
            logger.warn(
                "Google Advertising ID or Fire Advertising ID not detected, " +
                        "fallback to non Google Play and Fire identifiers will take place"
            )
            deviceInfo.reloadNonPlayIds(motrackConfig);
            addString(parameters, "android_id", deviceInfo.androidId)
        }

        // Rest of the parameters.
        addString(parameters, "app_secret", motrackConfig.appSecret)
        addString(parameters, "app_token", motrackConfig.appToken)
        addBoolean(parameters, "attribution_deeplink", true)
        addDateInMilliseconds(parameters, "created_at", createdAt)
        addBoolean(parameters, "device_known", motrackConfig.deviceKnown)
        addBoolean(parameters, "needs_cost", motrackConfig.needsCost)
        addString(parameters, "environment", motrackConfig.environment)
        addBoolean(
            parameters,
            "event_buffering_enabled",
            motrackConfig.eventBufferingEnabled
        )
        addString(parameters, "external_device_id", motrackConfig.externalDeviceId)
        addBoolean(parameters, "needs_response_details", true)
        addString(parameters, "push_token", activityStateCopy!!.pushToken)
        addString(parameters, "secret_id", motrackConfig.secretId)
        addString(parameters, "source", source)
        checkDeviceIds(parameters)
        return parameters
    }

    private fun getClickParameters(source: String): HashMap<String, String> {
        val contentResolver: ContentResolver = motrackConfig.context!!.contentResolver
        val parameters: HashMap<String, String> = HashMap()
        val sdkClick: HashMap<String, String> = HashMap()
        val imeiParameters =
            Util.getImeiParameters(motrackConfig, logger)




        // Check if plugin is used and if yes, add read parameters.
        if (imeiParameters != null) {
            parameters.putAll(imeiParameters)
        }

        // Check if oaid plugin is used and if yes, add the parameter
        val oaidParameters =
           Util.getOaidParameters(motrackConfig, logger)
        if (oaidParameters != null) {
            parameters.putAll(oaidParameters)
        }

        // Device identifiers.
        deviceInfo.reloadPlayIds(motrackConfig)

        addBoolean(sdkClick, "needs_response_details", true)

        addString(parameters, "android_uuid", activityStateCopy!!.uuid)
        addString(parameters, "gps_adid", deviceInfo.playAdId)
        addLong(parameters, "gps_adid_attempt", deviceInfo.playAdIdAttempt.toLong())
        addString(parameters, "gps_adid_src", deviceInfo.playAdIdSource)
        addBoolean(parameters, "tracking_enabled", deviceInfo.isTrackingEnabled)
        addString(
            parameters,
            "fire_adid",
            Util.getFireAdvertisingId(motrackConfig.context!!.contentResolver)
        )
        addBoolean(
            parameters,
            "fire_tracking_enabled",
          Util.getFireTrackingEnabled(motrackConfig.context!!.contentResolver)
        )
        if (!containsPlayIds(parameters) && !containsFireIds(parameters)) {
            logger.warn(
                "Google Advertising ID or Fire Advertising ID not detected, " +
                        "fallback to non Google Play and Fire identifiers will take place"
            )
            deviceInfo.reloadNonPlayIds(motrackConfig);
            addString(parameters, "android_id", deviceInfo.androidId)
        }

        // Attribution parameters.
        if (attribution != null) {
            addString(parameters, "tracker", attribution!!.trackerName)
            addString(parameters, "campaign", attribution!!.campaign)
            addString(parameters, "adgroup", attribution!!.adgroup)
            addString(parameters, "creative", attribution!!.creative)
        }

        // Rest of the parameters.
        addString(parameters, "api_level", deviceInfo.apiLevel)
        addString(parameters, "app_secret", motrackConfig.appSecret)
        addString(parameters, "app_token", motrackConfig.appToken)
        addString(parameters, "app_version", deviceInfo.appVersion)
        addBoolean(parameters, "attribution_deeplink", true)
        addMapJson(
            parameters,
            "callback_params",
            sessionParameters!!.callbackParameters
        )
        addDateInMilliseconds(parameters, "click_time", clickTimeInMilliseconds)
        addDateInSeconds(parameters, "click_time", clickTimeInSeconds)
        addDateInSeconds(parameters, "click_time_server", clickTimeServerInSeconds)
        addLong(
            parameters,
            "connectivity_type",
            AndroidUtil.getConnectivityType(motrackConfig.context!!).toLong()
        )
        addString(parameters, "country", deviceInfo.country)
        addString(parameters, "cpu_type", deviceInfo.abi)
        addDateInMilliseconds(parameters, "created_at", createdAt)
        addString(parameters, "deeplink", deeplink)
        addBoolean(parameters, "device_known", motrackConfig.deviceKnown)
        addBoolean(parameters, "needs_cost", motrackConfig.needsCost)
        addString(parameters, "device_manufacturer", deviceInfo.deviceManufacturer)
        addString(parameters, "device_name", deviceInfo.deviceName)
        addString(parameters, "device_type", deviceInfo.deviceType)
        addLong(parameters, "ui_mode", deviceInfo.uiMode?.toLong())
        addString(parameters, "display_height", deviceInfo.displayHeight)
        addString(parameters, "display_width", deviceInfo.displayWidth)
        addString(parameters, "environment", motrackConfig.environment)
        addBoolean(
            parameters,
            "event_buffering_enabled",
            motrackConfig.eventBufferingEnabled
        )
        addString(parameters, "external_device_id", motrackConfig.externalDeviceId)
        addString(parameters, "fb_id", deviceInfo.fbAttributionId)
        addBoolean(parameters, "google_play_instant", googlePlayInstant)
        addString(parameters, "hardware_name", deviceInfo.hardwareName)
        addDateInSeconds(parameters, "install_begin_time", installBeginTimeInSeconds)
        addDateInSeconds(
            parameters,
            "install_begin_time_server",
            installBeginTimeServerInSeconds
        )
        addString(parameters, "install_version", installVersion)
        addString(parameters, "installed_at", deviceInfo.appInstallTime)
        addString(parameters, "language", deviceInfo.language)
        addDuration(parameters, "last_interval", activityStateCopy!!.lastInterval)
        addString(parameters, "mcc", AndroidUtil.getMcc(motrackConfig.context!!))
        addString(parameters, "mnc", AndroidUtil.getMnc(motrackConfig.context!!))


        addString(parameters, "os_build", deviceInfo.buildName)
        addString(parameters, "os_name", deviceInfo.osName)
        addString(parameters, "os_version", deviceInfo.osVersion)
        addString(parameters, "package_name", deviceInfo.packageName)
        addMapJson(parameters, "params", extraParameters)
        addMapJson(
            parameters,
            "partner_params",
            sessionParameters.partnerParameters
        )
        addString(parameters, "push_token", activityStateCopy!!.pushToken)
        addString(parameters, "raw_referrer", rawReferrer)
        addString(parameters, "referrer", referrer)
        addString(parameters, "referrer_api", referrerApi)
        addString(parameters, "reftag", reftag)
        addString(parameters, "screen_density", deviceInfo.screenDensity)
        addString(parameters, "screen_format", deviceInfo.screenFormat)
        addString(parameters, "screen_size", deviceInfo.screenSize)
        addString(parameters, "secret_id", motrackConfig.secretId)
        addLong(
            parameters,
            "session_count",
            activityStateCopy!!.sessionCount.toLong()
        )
        addDuration(parameters, "session_length", activityStateCopy!!.sessionLength)
        addString(parameters, "source", source)
        addLong(
            parameters,
            "subsession_count",
            activityStateCopy!!.subsessionCount.toLong()
        )
        addDuration(parameters, "time_spent", activityStateCopy!!.timeSpent)
        addString(parameters, "updated_at", deviceInfo.appUpdateTime)
        addString(parameters, "payload", preinstallPayload)
        addString(parameters, "found_location", preinstallLocation)

        addMapJson(sdkClick,"parameters",parameters)

        checkDeviceIds(sdkClick)
        return sdkClick
    }

    private fun getAttributionParameters(initiatedBy: String): HashMap<String, String> {
        val contentResolver: ContentResolver = motrackConfig.context!!.contentResolver
        val parameters: HashMap<String, String> = HashMap()
        val attribution: HashMap<String, String> = HashMap()

        val imeiParameters =
            Util.getImeiParameters(motrackConfig, logger)

        // Check if plugin is used and if yes, add read parameters.
        if (imeiParameters != null) {
            parameters.putAll(imeiParameters)
        }

        // Check if oaid plugin is used and if yes, add the parameter
        val oaidParameters =
           Util.getOaidParameters(motrackConfig, logger)
        if (oaidParameters != null) {
            parameters.putAll(oaidParameters)
        }

        // Device identifiers.
        deviceInfo.reloadPlayIds(motrackConfig)



        addBoolean(
            parameters,
            "fire_tracking_enabled",
            Util.getFireTrackingEnabled(motrackConfig.context!!.contentResolver)
        )
        if (!containsPlayIds(attribution) && !containsFireIds(attribution)) {
            logger.warn(
                "Google Advertising ID or Fire Advertising ID not detected, " +
                        "fallback to non Google Play and Fire identifiers will take place"
            )
            deviceInfo.reloadNonPlayIds(motrackConfig);
            addString(parameters, "android_id", deviceInfo.androidId)
        }



        logger.info("*************************************************************************************")
        // Rest of the parameters.
        addBoolean(attribution, "needs_response_details", true)
        addString(parameters, "fire_adid", Util.getFireAdvertisingId(motrackConfig.context!!.contentResolver))
        addString(parameters, "android_uuid", activityStateCopy!!.uuid)
        addString(parameters, "gps_adid", deviceInfo.playAdId)
        addLong(parameters, "gps_adid_attempt", deviceInfo.playAdIdAttempt.toLong())
        addString(parameters, "gps_adid_src", deviceInfo.playAdIdSource)
        addBoolean(parameters, "tracking_enabled", deviceInfo.isTrackingEnabled)
        addString(parameters, "api_level", deviceInfo.apiLevel)
        addString(parameters, "app_secret", motrackConfig.appSecret)
        addString(parameters, "app_token", motrackConfig.appToken)
        addString(parameters, "app_version", deviceInfo.appVersion)
        addBoolean(parameters, "attribution_deeplink", true)
        addDateInMilliseconds(parameters, "created_at", createdAt)
        addBoolean(parameters, "device_known", motrackConfig.deviceKnown)
        addBoolean(parameters, "needs_cost", motrackConfig.needsCost)
        addString(parameters, "device_name", deviceInfo.deviceName)
        addString(parameters, "device_type", deviceInfo.deviceType)
        addLong(parameters, "ui_mode", deviceInfo.uiMode?.toLong())
        addString(parameters, "environment", motrackConfig.environment)
        addBoolean(parameters, "event_buffering_enabled", motrackConfig.eventBufferingEnabled)
        addString(parameters, "external_device_id", motrackConfig.externalDeviceId)
        addString(parameters, "initiated_by", initiatedBy)
        addString(parameters, "os_name", deviceInfo.osName)
        addString(parameters, "os_version", deviceInfo.osVersion)
        addString(parameters, "package_name", deviceInfo.packageName)
        addString(parameters, "push_token", activityStateCopy!!.pushToken)
        addString(parameters, "secret_id", motrackConfig.secretId)



        addMapJson(attribution,"parameters",parameters)
        checkDeviceIds(attribution)
        return attribution
    }

    private fun getGdprParameters(): HashMap<String, String> {
        val contentResolver: ContentResolver = motrackConfig.context!!.contentResolver
        val parameters: HashMap<String, String> = HashMap()
        val imeiParameters =
            Util.getImeiParameters(motrackConfig, logger)

        // Check if plugin is used and if yes, add read parameters.
        if (imeiParameters != null) {
            parameters.putAll(imeiParameters)
        }

        // Check if oaid plugin is used and if yes, add the parameter
        val oaidParameters =
           Util.getOaidParameters(motrackConfig, logger)
        if (oaidParameters != null) {
            parameters.putAll(oaidParameters)
        }

        // Device identifiers.
        deviceInfo.reloadPlayIds(motrackConfig)
        addString(parameters, "android_uuid", activityStateCopy!!.uuid)
        addString(parameters, "gps_adid", deviceInfo.playAdId)
        addLong(parameters, "gps_adid_attempt", deviceInfo.playAdIdAttempt.toLong())
        addString(parameters, "gps_adid_src", deviceInfo.playAdIdSource)
        addBoolean(parameters, "tracking_enabled", deviceInfo.isTrackingEnabled)
        addString(
            parameters,
            "fire_adid",
            Util.getFireAdvertisingId(motrackConfig.context!!.contentResolver)
        )
        addBoolean(
            parameters,
            "fire_tracking_enabled",
          Util.getFireTrackingEnabled(motrackConfig.context!!.contentResolver)
        )
        if (!containsPlayIds(parameters) && !containsFireIds(parameters)) {
            logger.warn(
                "Google Advertising ID or Fire Advertising ID not detected, " +
                        "fallback to non Google Play and Fire identifiers will take place"
            )
            deviceInfo.reloadNonPlayIds(motrackConfig);
            addString(parameters, "android_id", deviceInfo.androidId)
        }

        // Rest of the parameters.
        addString(parameters, "api_level", deviceInfo.apiLevel)
        addString(parameters, "app_secret", motrackConfig.appSecret)
        addString(parameters, "app_token", motrackConfig.appToken)
        addString(parameters, "app_version", deviceInfo.appVersion)
        addBoolean(parameters, "attribution_deeplink", true)
        addDateInMilliseconds(parameters, "created_at", createdAt)
        addBoolean(parameters, "device_known", motrackConfig.deviceKnown)
        addBoolean(parameters, "needs_cost", motrackConfig.needsCost)
        addString(parameters, "device_name", deviceInfo.deviceName)
        addString(parameters, "device_type", deviceInfo.deviceType)
        addLong(parameters, "ui_mode", deviceInfo.uiMode?.toLong())
        addString(parameters, "environment", motrackConfig.environment)
        addBoolean(
            parameters,
            "event_buffering_enabled",
            motrackConfig.eventBufferingEnabled
        )
        addString(parameters, "external_device_id", motrackConfig.externalDeviceId)
        addBoolean(parameters, "needs_response_details", true)
        addString(parameters, "os_name", deviceInfo.osName)
        addString(parameters, "os_version", deviceInfo.osVersion)
        addString(parameters, "package_name", deviceInfo.packageName)
        addString(parameters, "push_token", activityStateCopy!!.pushToken)
        addString(parameters, "secret_id", motrackConfig.secretId)
        addBoolean(parameters, "ff_play_store_kids_app", motrackConfig.playStoreKidsAppEnabled)
        addBoolean(parameters, "ff_coppa", motrackConfig.coppaCompliantEnabled)
        checkDeviceIds(parameters)
        return parameters
    }

    private fun getDisableThirdPartySharingParameters(): HashMap<String, String> {
        val contentResolver: ContentResolver = motrackConfig.context!!.contentResolver
        val parameters: HashMap<String, String> = HashMap()
        val imeiParameters =
            Util.getImeiParameters(motrackConfig, logger)

        // Check if plugin is used and if yes, add read parameters.
        if (imeiParameters != null) {
            parameters.putAll(imeiParameters)
        }

        // Check if oaid plugin is used and if yes, add the parameter
        val oaidParameters =
           Util.getOaidParameters(motrackConfig, logger)
        if (oaidParameters != null) {
            parameters.putAll(oaidParameters)
        }

        // Device identifiers.
        deviceInfo.reloadPlayIds(motrackConfig)
        addString(parameters, "android_uuid", activityStateCopy!!.uuid)
        addString(parameters, "gps_adid", deviceInfo.playAdId)
        addLong(parameters, "gps_adid_attempt", deviceInfo.playAdIdAttempt.toLong())
        addString(parameters, "gps_adid_src", deviceInfo.playAdIdSource)
        addBoolean(parameters, "tracking_enabled", deviceInfo.isTrackingEnabled)
        addString(
            parameters,
            "fire_adid",
            Util.getFireAdvertisingId(motrackConfig.context!!.contentResolver)
        )
        addBoolean(
            parameters,
            "fire_tracking_enabled",
          Util.getFireTrackingEnabled(motrackConfig.context!!.contentResolver)
        )
        if (!containsPlayIds(parameters) && !containsFireIds(parameters)) {
            logger.warn(
                "Google Advertising ID or Fire Advertising ID not detected, " +
                        "fallback to non Google Play and Fire identifiers will take place"
            )
            deviceInfo.reloadNonPlayIds(motrackConfig);
            addString(parameters, "android_id", deviceInfo.androidId)
        }

        // Rest of the parameters.
        addString(parameters, "api_level", deviceInfo.apiLevel)
        addString(parameters, "app_secret", motrackConfig.appSecret)
        addString(parameters, "app_token", motrackConfig.appToken)
        addString(parameters, "app_version", deviceInfo.appVersion)
        addBoolean(parameters, "attribution_deeplink", true)
        addDateInMilliseconds(parameters, "created_at", createdAt)
        addBoolean(parameters, "device_known", motrackConfig.deviceKnown)
        addBoolean(parameters, "needs_cost", motrackConfig.needsCost)
        addString(parameters, "device_name", deviceInfo.deviceName)
        addString(parameters, "device_type", deviceInfo.deviceType)
        addLong(parameters, "ui_mode", deviceInfo.uiMode?.toLong())
        addString(parameters, "environment", motrackConfig.environment)
        addBoolean(
            parameters,
            "event_buffering_enabled",
            motrackConfig.eventBufferingEnabled
        )
        addString(parameters, "external_device_id", motrackConfig.externalDeviceId)
        addBoolean(parameters, "needs_response_details", true)
        addString(parameters, "os_name", deviceInfo.osName)
        addString(parameters, "os_version", deviceInfo.osVersion)
        addString(parameters, "package_name", deviceInfo.packageName)
        addString(parameters, "push_token", activityStateCopy!!.pushToken)
        addString(parameters, "secret_id", motrackConfig.secretId)
        addBoolean(parameters, "ff_play_store_kids_app", motrackConfig.playStoreKidsAppEnabled)
        addBoolean(parameters, "ff_coppa", motrackConfig.coppaCompliantEnabled)
        checkDeviceIds(parameters)
        return parameters
    }

    private fun getThirdPartySharingParameters(motrackThirdPartySharing: MotrackThirdPartySharing): HashMap<String, String> {
        val contentResolver: ContentResolver = motrackConfig.context!!.contentResolver
        val parameters: HashMap<String, String> = HashMap()
        val imeiParameters =
            Util.getImeiParameters(motrackConfig, logger)

        // Check if plugin is used and if yes, add read parameters.
        if (imeiParameters != null) {
            parameters.putAll(imeiParameters)
        }

        // Check if oaid plugin is used and if yes, add the parameter
        val oaidParameters =
           Util.getOaidParameters(motrackConfig, logger)
        if (oaidParameters != null) {
            parameters.putAll(oaidParameters)
        }

        // Third Party Sharing
        if (motrackThirdPartySharing.isEnabled != null) {
            addString(
                parameters, "sharing",
                if (motrackThirdPartySharing.isEnabled) "enable" else "disable"
            )
        }
        addMapJson(
            parameters, "granular_third_party_sharing_options",
            motrackThirdPartySharing.granularOptions
        )

        // Device identifiers.
        deviceInfo.reloadPlayIds(motrackConfig)
        addString(parameters, "android_uuid", activityStateCopy!!.uuid)
        addString(parameters, "gps_adid", deviceInfo.playAdId)
        addLong(parameters, "gps_adid_attempt", deviceInfo.playAdIdAttempt.toLong())
        addString(parameters, "gps_adid_src", deviceInfo.playAdIdSource)
        addBoolean(parameters, "tracking_enabled", deviceInfo.isTrackingEnabled)
        addString(
            parameters,
            "fire_adid",
            Util.getFireAdvertisingId(motrackConfig.context!!.contentResolver)
        )
        addBoolean(
            parameters,
            "fire_tracking_enabled",
          Util.getFireTrackingEnabled(motrackConfig.context!!.contentResolver)
        )
        if (!containsPlayIds(parameters) && !containsFireIds(parameters)) {
            logger.warn(
                "Google Advertising ID or Fire Advertising ID not detected, " +
                        "fallback to non Google Play and Fire identifiers will take place"
            )
            deviceInfo.reloadNonPlayIds(motrackConfig);
            addString(parameters, "android_id", deviceInfo.androidId)
        }

        // Rest of the parameters.
        addString(parameters, "api_level", deviceInfo.apiLevel)
        addString(parameters, "app_secret", motrackConfig.appSecret)
        addString(parameters, "app_token", motrackConfig.appToken)
        addString(parameters, "app_version", deviceInfo.appVersion)
        addBoolean(parameters, "attribution_deeplink", true)
        addDateInMilliseconds(parameters, "created_at", createdAt)
        addBoolean(parameters, "device_known", motrackConfig.deviceKnown)
        addString(parameters, "device_name", deviceInfo.deviceName)
        addString(parameters, "device_type", deviceInfo.deviceType)
        addLong(parameters, "ui_mode", deviceInfo.uiMode?.toLong())
        addString(parameters, "environment", motrackConfig.environment)
        addBoolean(
            parameters,
            "event_buffering_enabled",
            motrackConfig.eventBufferingEnabled
        )
        addString(parameters, "external_device_id", motrackConfig.externalDeviceId)
        addBoolean(parameters, "needs_response_details", true)
        addString(parameters, "os_name", deviceInfo.osName)
        addString(parameters, "os_version", deviceInfo.osVersion)
        addString(parameters, "package_name", deviceInfo.packageName)
        addString(parameters, "push_token", activityStateCopy!!.pushToken)
        addString(parameters, "secret_id", motrackConfig.secretId)
        addBoolean(parameters, "ff_play_store_kids_app", motrackConfig.playStoreKidsAppEnabled)
        addBoolean(parameters, "ff_coppa", motrackConfig.coppaCompliantEnabled)
        checkDeviceIds(parameters)
        return parameters
    }

    private fun getMeasurementConsentParameters(
        consentMeasurement: Boolean
    ): HashMap<String, String> {
        val contentResolver: ContentResolver = motrackConfig.context!!.contentResolver
        val parameters: HashMap<String, String> = HashMap()
        val imeiParameters =
            Util.getImeiParameters(motrackConfig, logger)

        // Check if plugin is used and if yes, add read parameters.
        if (imeiParameters != null) {
            parameters.putAll(imeiParameters)
        }

        // Check if oaid plugin is used and if yes, add the parameter
        val oaidParameters =
           Util.getOaidParameters(motrackConfig, logger)
        if (oaidParameters != null) {
            parameters.putAll(oaidParameters)
        }

        // Measurement Consent
        addString(
            parameters, "measurement",
            if (consentMeasurement) "enable" else "disable"
        )

        // Device identifiers.
        deviceInfo.reloadPlayIds(motrackConfig)
        addString(parameters, "android_uuid", activityStateCopy!!.uuid)
        addString(parameters, "gps_adid", deviceInfo.playAdId)
        addLong(parameters, "gps_adid_attempt", deviceInfo.playAdIdAttempt.toLong())
        addString(parameters, "gps_adid_src", deviceInfo.playAdIdSource)
        addBoolean(parameters, "tracking_enabled", deviceInfo.isTrackingEnabled)
        addString(
            parameters,
            "fire_adid",
            Util.getFireAdvertisingId(motrackConfig.context!!.contentResolver)
        )
        addBoolean(
            parameters,
            "fire_tracking_enabled",
          Util.getFireTrackingEnabled(motrackConfig.context!!.contentResolver)
        )
        if (!containsPlayIds(parameters) && !containsFireIds(parameters)) {
            logger.warn(
                "Google Advertising ID or Fire Advertising ID not detected, " +
                        "fallback to non Google Play and Fire identifiers will take place"
            )
            deviceInfo.reloadNonPlayIds(motrackConfig);
            addString(parameters, "android_id", deviceInfo.androidId)
        }

        // Rest of the parameters.
        addString(parameters, "api_level", deviceInfo.apiLevel)
        addString(parameters, "app_secret", motrackConfig.appSecret)
        addString(parameters, "app_token", motrackConfig.appToken)
        addString(parameters, "app_version", deviceInfo.appVersion)
        addBoolean(parameters, "attribution_deeplink", true)
        addDateInMilliseconds(parameters, "created_at", createdAt)
        addBoolean(parameters, "device_known", motrackConfig.deviceKnown)
        addString(parameters, "device_name", deviceInfo.deviceName)
        addString(parameters, "device_type", deviceInfo.deviceType)
        addLong(parameters, "ui_mode", deviceInfo.uiMode?.toLong())
        addString(parameters, "environment", motrackConfig.environment)
        addBoolean(
            parameters,
            "event_buffering_enabled",
            motrackConfig.eventBufferingEnabled
        )
        addString(parameters, "external_device_id", motrackConfig.externalDeviceId)
        addBoolean(parameters, "needs_response_details", true)
        addString(parameters, "os_name", deviceInfo.osName)
        addString(parameters, "os_version", deviceInfo.osVersion)
        addString(parameters, "package_name", deviceInfo.packageName)
        addString(parameters, "push_token", activityStateCopy!!.pushToken)
        addString(parameters, "secret_id", motrackConfig.secretId)
        addBoolean(parameters, "ff_play_store_kids_app", motrackConfig.playStoreKidsAppEnabled)
        addBoolean(parameters, "ff_coppa", motrackConfig.coppaCompliantEnabled)
        checkDeviceIds(parameters)
        return parameters
    }

    private fun getAdRevenueParameters(
        source: String,
        adRevenueJson: JSONObject
    ): HashMap<String, String> {
        val contentResolver: ContentResolver = motrackConfig.context!!.contentResolver
        val parameters: HashMap<String, String> = HashMap()
        val imeiParameters =
            Util.getImeiParameters(motrackConfig, logger)

        // Check if plugin is used and if yes, add read parameters.
        if (imeiParameters != null) {
            parameters.putAll(imeiParameters)
        }

        // Check if oaid plugin is used and if yes, add the parameter
        val oaidParameters =
           Util.getOaidParameters(motrackConfig, logger)
        if (oaidParameters != null) {
            parameters.putAll(oaidParameters)
        }

        // Device identifiers.
        deviceInfo.reloadPlayIds(motrackConfig)
        addString(parameters, "android_uuid", activityStateCopy!!.uuid)
        addString(parameters, "gps_adid", deviceInfo.playAdId)
        addLong(parameters, "gps_adid_attempt", deviceInfo.playAdIdAttempt.toLong())
        addString(parameters, "gps_adid_src", deviceInfo.playAdIdSource)
        addBoolean(parameters, "tracking_enabled", deviceInfo.isTrackingEnabled)
        addString(
            parameters,
            "fire_adid",
            Util.getFireAdvertisingId(motrackConfig.context!!.contentResolver)
        )
        addBoolean(
            parameters,
            "fire_tracking_enabled",
          Util.getFireTrackingEnabled(motrackConfig.context!!.contentResolver)
        )
        if (!containsPlayIds(parameters) && !containsFireIds(parameters)) {
            logger.warn(
                "Google Advertising ID or Fire Advertising ID not detected, " +
                        "fallback to non Google Play and Fire identifiers will take place"
            )
            deviceInfo.reloadNonPlayIds(motrackConfig);
            addString(parameters, "android_id", deviceInfo.androidId)
        }

        // Rest of the parameters.
        addString(parameters, "api_level", deviceInfo.apiLevel)
        addString(parameters, "app_secret", motrackConfig.appSecret)
        addString(parameters, "app_token", motrackConfig.appToken)
        addString(parameters, "app_version", deviceInfo.appVersion)
        addBoolean(parameters, "attribution_deeplink", true)
        addLong(
            parameters,
            "connectivity_type",
            AndroidUtil.getConnectivityType(motrackConfig.context!!).toLong()
        )
        addString(parameters, "country", deviceInfo.country)
        addString(parameters, "cpu_type", deviceInfo.abi)
        addDateInMilliseconds(parameters, "created_at", createdAt)
        addString(parameters, "default_tracker", motrackConfig.defaultTracker)
        addBoolean(parameters, "device_known", motrackConfig.deviceKnown)
        addBoolean(parameters, "needs_cost", motrackConfig.needsCost)
        addString(parameters, "device_manufacturer", deviceInfo.deviceManufacturer)
        addString(parameters, "device_name", deviceInfo.deviceName)
        addString(parameters, "device_type", deviceInfo.deviceType)
        addLong(parameters, "ui_mode", deviceInfo.uiMode?.toLong())
        addString(parameters, "display_height", deviceInfo.displayHeight)
        addString(parameters, "display_width", deviceInfo.displayWidth)
        addString(parameters, "environment", motrackConfig.environment)
        addBoolean(
            parameters,
            "event_buffering_enabled",
            motrackConfig.eventBufferingEnabled
        )
        addString(parameters, "external_device_id", motrackConfig.externalDeviceId)
        addString(parameters, "fb_id", deviceInfo.fbAttributionId)
        addString(parameters, "hardware_name", deviceInfo.hardwareName)
        addString(parameters, "installed_at", deviceInfo.appInstallTime)
        addString(parameters, "language", deviceInfo.language)
        addDuration(parameters, "last_interval", activityStateCopy!!.lastInterval)
        addString(parameters, "mcc", AndroidUtil.getMcc(motrackConfig.context!!))
        addString(parameters, "mnc", AndroidUtil.getMnc(motrackConfig.context!!))
        addBoolean(parameters, "needs_response_details", true)

        addString(parameters, "os_build", deviceInfo.buildName)
        addString(parameters, "os_name", deviceInfo.osName)
        addString(parameters, "os_version", deviceInfo.osVersion)
        addString(parameters, "package_name", deviceInfo.packageName)
        addString(parameters, "push_token", activityStateCopy!!.pushToken)
        addString(parameters, "screen_density", deviceInfo.screenDensity)
        addString(parameters, "screen_format", deviceInfo.screenFormat)
        addString(parameters, "screen_size", deviceInfo.screenSize)
        addString(parameters, "secret_id", motrackConfig.secretId)
        addString(parameters, "source", source)
        addJsonObject(parameters, "payload", adRevenueJson)
        addLong(
            parameters,
            "session_count",
            activityStateCopy!!.sessionCount.toLong()
        )
        addDuration(parameters, "session_length", activityStateCopy!!.sessionLength)
        addLong(
            parameters,
            "subsession_count",
            activityStateCopy!!.subsessionCount.toLong()
        )
        addDuration(parameters, "time_spent", activityStateCopy!!.timeSpent)
        addString(parameters, "updated_at", deviceInfo.appUpdateTime)
        addBoolean(parameters, "ff_play_store_kids_app", motrackConfig.playStoreKidsAppEnabled)
        addBoolean(parameters, "ff_coppa", motrackConfig.coppaCompliantEnabled)
        checkDeviceIds(parameters)
        return parameters
    }

    private fun getAdRevenueParameters(
        motrackAdRevenue: MotrackAdRevenue,
        isInDelay: Boolean
    ): HashMap<String, String> {
        val contentResolver: ContentResolver = motrackConfig.context!!.contentResolver
        val parameters: HashMap<String, String> = HashMap()
        val imeiParameters =
            Util.getImeiParameters(motrackConfig, logger)

        // Check if plugin is used and if yes, add read parameters.
        if (imeiParameters != null) {
            parameters.putAll(imeiParameters)
        }

        // Check if oaid plugin is used and if yes, add the parameter
        val oaidParameters =
           Util.getOaidParameters(motrackConfig, logger)
        if (oaidParameters != null) {
            parameters.putAll(oaidParameters)
        }

        // Callback and partner parameters.
        if (!isInDelay) {
            addMapJson(
                parameters, "callback_params", Util.mergeParameters(
                    sessionParameters!!.callbackParameters,
                    motrackAdRevenue.callbackParameters,
                    "Callback"
                )
            )
            addMapJson(
                parameters, "partner_params", Util.mergeParameters(
                    sessionParameters.partnerParameters,
                    motrackAdRevenue.partnerParameters,
                    "Partner"
                )
            )
        }

        // Device identifiers.
        deviceInfo.reloadPlayIds(motrackConfig)
        addString(parameters, "android_uuid", activityStateCopy!!.uuid)
        addString(parameters, "gps_adid", deviceInfo.playAdId)
        addLong(parameters, "gps_adid_attempt", deviceInfo.playAdIdAttempt.toLong())
        addString(parameters, "gps_adid_src", deviceInfo.playAdIdSource)
        addBoolean(parameters, "tracking_enabled", deviceInfo.isTrackingEnabled)
        addString(
            parameters,
            "fire_adid",
            Util.getFireAdvertisingId(motrackConfig.context!!.contentResolver)
        )
        addBoolean(
            parameters,
            "fire_tracking_enabled",
          Util.getFireTrackingEnabled(motrackConfig.context!!.contentResolver)
        )
        if (!containsPlayIds(parameters) && !containsFireIds(parameters)) {
            logger.warn(
                "Google Advertising ID or Fire Advertising ID not detected, " +
                        "fallback to non Google Play and Fire identifiers will take place"
            )
            deviceInfo.reloadNonPlayIds(motrackConfig);
            addString(parameters, "android_id", deviceInfo.androidId)
        }

        // Rest of the parameters.
        addString(parameters, "api_level", deviceInfo.apiLevel)
        addString(parameters, "app_secret", motrackConfig.appSecret)
        addString(parameters, "app_token", motrackConfig.appToken)
        addString(parameters, "app_version", deviceInfo.appVersion)
        addBoolean(parameters, "attribution_deeplink", true)
        addLong(
            parameters,
            "connectivity_type",
            AndroidUtil.getConnectivityType(motrackConfig.context!!).toLong()
        )
        addString(parameters, "country", deviceInfo.country)
        addString(parameters, "cpu_type", deviceInfo.abi)
        addDateInMilliseconds(parameters, "created_at", createdAt)
        addString(parameters, "default_tracker", motrackConfig.defaultTracker)
        addBoolean(parameters, "device_known", motrackConfig.deviceKnown)
        addBoolean(parameters, "needs_cost", motrackConfig.needsCost)
        addString(parameters, "device_manufacturer", deviceInfo.deviceManufacturer)
        addString(parameters, "device_name", deviceInfo.deviceName)
        addString(parameters, "device_type", deviceInfo.deviceType)
        addLong(parameters, "ui_mode", deviceInfo.uiMode?.toLong())
        addString(parameters, "display_height", deviceInfo.displayHeight)
        addString(parameters, "display_width", deviceInfo.displayWidth)
        addString(parameters, "environment", motrackConfig.environment)
        addBoolean(
            parameters,
            "event_buffering_enabled",
            motrackConfig.eventBufferingEnabled
        )
        addString(parameters, "external_device_id", motrackConfig.externalDeviceId)
        addString(parameters, "fb_id", deviceInfo.fbAttributionId)
        addString(parameters, "hardware_name", deviceInfo.hardwareName)
        addString(parameters, "installed_at", deviceInfo.appInstallTime)
        addString(parameters, "language", deviceInfo.language)
        addDuration(parameters, "last_interval", activityStateCopy!!.lastInterval)
        addString(parameters, "mcc", AndroidUtil.getMcc(motrackConfig.context!!))
        addString(parameters, "mnc", AndroidUtil.getMnc(motrackConfig.context!!))
        addBoolean(parameters, "needs_response_details", true)

        addString(parameters, "os_build", deviceInfo.buildName)
        addString(parameters, "os_name", deviceInfo.osName)
        addString(parameters, "os_version", deviceInfo.osVersion)
        addString(parameters, "package_name", deviceInfo.packageName)
        addString(parameters, "push_token", activityStateCopy!!.pushToken)
        addString(parameters, "screen_density", deviceInfo.screenDensity)
        addString(parameters, "screen_format", deviceInfo.screenFormat)
        addString(parameters, "screen_size", deviceInfo.screenSize)
        addString(parameters, "secret_id", motrackConfig.secretId)
        addString(parameters, "source", motrackAdRevenue.source)
        addDoubleWithoutRounding(parameters, "revenue", motrackAdRevenue.revenue)
        addString(parameters, "currency", motrackAdRevenue.currency)
        addInteger(
            parameters,
            "ad_impressions_count",
            motrackAdRevenue.adImpressionsCount
        )
        addString(parameters, "ad_revenue_network", motrackAdRevenue.adRevenueNetwork)
        addString(parameters, "ad_revenue_unit", motrackAdRevenue.adRevenueUnit)
        addString(
            parameters,
            "ad_revenue_placement",
            motrackAdRevenue.adRevenuePlacement
        )
        addLong(
            parameters,
            "session_count",
            activityStateCopy!!.sessionCount.toLong()
        )
        addDuration(parameters, "session_length", activityStateCopy!!.sessionLength)
        addLong(
            parameters,
            "subsession_count",
            activityStateCopy!!.subsessionCount.toLong()
        )
        addDuration(parameters, "time_spent", activityStateCopy!!.timeSpent)
        addString(parameters, "updated_at", deviceInfo.appUpdateTime)
        addBoolean(parameters, "ff_play_store_kids_app", motrackConfig.playStoreKidsAppEnabled)
        addBoolean(parameters, "ff_coppa", motrackConfig.coppaCompliantEnabled)
        checkDeviceIds(parameters)
        return parameters
    }

    private fun getSubscriptionParameters(
        subscription: MotrackPlayStoreSubscription,
        isInDelay: Boolean
    ): HashMap<String, String> {
        val contentResolver: ContentResolver = motrackConfig.context!!.contentResolver
        val parameters: HashMap<String, String> = HashMap()
        val imeiParameters =
            Util.getImeiParameters(motrackConfig, logger)

        // Check if plugin is used and if yes, add read parameters.
        if (imeiParameters != null) {
            parameters.putAll(imeiParameters)
        }

        // Check if oaid plugin is used and if yes, add the parameter
        val oaidParameters =
           Util.getOaidParameters(motrackConfig, logger)
        if (oaidParameters != null) {
            parameters.putAll(oaidParameters)
        }

        // Device identifiers.
        deviceInfo.reloadPlayIds(motrackConfig)
        addString(parameters, "android_uuid", activityStateCopy!!.uuid)
        addString(parameters, "gps_adid", deviceInfo.playAdId)
        addLong(parameters, "gps_adid_attempt", deviceInfo.playAdIdAttempt.toLong())
        addString(parameters, "gps_adid_src", deviceInfo.playAdIdSource)
        addBoolean(parameters, "tracking_enabled", deviceInfo.isTrackingEnabled)
        addString(
            parameters,
            "fire_adid",
            Util.getFireAdvertisingId(motrackConfig.context!!.contentResolver)
        )
        addBoolean(
            parameters,
            "fire_tracking_enabled",
          Util.getFireTrackingEnabled(motrackConfig.context!!.contentResolver)
        )
        if (!containsPlayIds(parameters) && !containsFireIds(parameters)) {
            logger.warn(
                "Google Advertising ID or Fire Advertising ID not detected, " +
                        "fallback to non Google Play and Fire identifiers will take place"
            )
            deviceInfo.reloadNonPlayIds(motrackConfig);
            addString(parameters, "android_id", deviceInfo.androidId)
        }

        // Callback and partner parameters.
        if (!isInDelay) {
            addMapJson(
                parameters, "callback_params", Util.mergeParameters(
                    sessionParameters?.callbackParameters,
                    subscription.getCallbackParameters(),
                    "Callback"
                )
            )
            addMapJson(
                parameters, "partner_params", Util.mergeParameters(
                    sessionParameters?.partnerParameters,
                    subscription.getPartnerParameters(),
                    "Partner"
                )
            )
        }

        // Rest of the parameters.
        addString(parameters, "api_level", deviceInfo.apiLevel)
        addString(parameters, "app_secret", motrackConfig.appSecret)
        addString(parameters, "app_token", motrackConfig.appToken)
        addString(parameters, "app_version", deviceInfo.appVersion)
        addBoolean(parameters, "attribution_deeplink", true)
        addLong(
            parameters,
            "connectivity_type",
            AndroidUtil.getConnectivityType(motrackConfig.context!!).toLong()
        )
        addString(parameters, "country", deviceInfo.country)
        addString(parameters, "cpu_type", deviceInfo.abi)
        addDateInMilliseconds(parameters, "created_at", createdAt)
        addString(parameters, "default_tracker", motrackConfig.defaultTracker)
        addBoolean(parameters, "device_known", motrackConfig.deviceKnown)
        addBoolean(parameters, "needs_cost", motrackConfig.needsCost)
        addString(parameters, "device_manufacturer", deviceInfo.deviceManufacturer)
        addString(parameters, "device_name", deviceInfo.deviceName)
        addString(parameters, "device_type", deviceInfo.deviceType)
        addLong(parameters, "ui_mode", deviceInfo.uiMode?.toLong())
        addString(parameters, "display_height", deviceInfo.displayHeight)
        addString(parameters, "display_width", deviceInfo.displayWidth)
        addString(parameters, "environment", motrackConfig.environment)
        addBoolean(
            parameters,
            "event_buffering_enabled",
            motrackConfig.eventBufferingEnabled
        )
        addString(parameters, "external_device_id", motrackConfig.externalDeviceId)
        addString(parameters, "fb_id", deviceInfo.fbAttributionId)
        addString(parameters, "hardware_name", deviceInfo.hardwareName)
        addString(parameters, "installed_at", deviceInfo.appInstallTime)
        addString(parameters, "language", deviceInfo.language)
        addDuration(parameters, "last_interval", activityStateCopy!!.lastInterval)
        addString(parameters, "mcc", AndroidUtil.getMcc(motrackConfig.context!!))
        addString(parameters, "mnc", AndroidUtil.getMnc(motrackConfig.context!!))
        addBoolean(parameters, "needs_response_details", true)

        addString(parameters, "os_build", deviceInfo.buildName)
        addString(parameters, "os_name", deviceInfo.osName)
        addString(parameters, "os_version", deviceInfo.osVersion)
        addString(parameters, "package_name", deviceInfo.packageName)
        addString(parameters, "push_token", activityStateCopy!!.pushToken)
        addString(parameters, "screen_density", deviceInfo.screenDensity)
        addString(parameters, "screen_format", deviceInfo.screenFormat)
        addString(parameters, "screen_size", deviceInfo.screenSize)
        addString(parameters, "secret_id", motrackConfig.secretId)
        addLong(
            parameters,
            "session_count",
            activityStateCopy!!.sessionCount.toLong()
        )
        addDuration(parameters, "session_length", activityStateCopy!!.sessionLength)
        addLong(
            parameters,
            "subsession_count",
            activityStateCopy!!.subsessionCount.toLong()
        )
        addDuration(parameters, "time_spent", activityStateCopy!!.timeSpent)
        addString(parameters, "updated_at", deviceInfo.appUpdateTime)

        addBoolean(parameters, "ff_play_store_kids_app", motrackConfig.playStoreKidsAppEnabled)
        addBoolean(parameters, "ff_coppa", motrackConfig.coppaCompliantEnabled)

        // subscription specific parameters
        addString(parameters, "billing_store", subscription.getBillingStore())
        addString(parameters, "currency", subscription.getCurrency())
        addString(parameters, "product_id", subscription.getSku())
        addString(parameters, "purchase_token", subscription.getPurchaseToken())
        addString(parameters, "receipt", subscription.getSignature())
        addLong(parameters, "revenue", subscription.getPrice())
        addDateInMilliseconds(
            parameters,
            "transaction_date",
            subscription.getPurchaseTime()
        )
        addString(parameters, "transaction_id", subscription.getOrderId())
        addBoolean(parameters, "ff_play_store_kids_app", motrackConfig.playStoreKidsAppEnabled)
        addBoolean(parameters, "ff_coppa", motrackConfig.coppaCompliantEnabled)
        checkDeviceIds(parameters)
        return parameters
    }

    private fun checkDeviceIds(parameters: Map<String, String>?) {
        if (parameters != null && !parameters.containsKey("android_id")
            && !parameters.containsKey("gps_adid")
            && !parameters.containsKey("fire_adid")
            && !parameters.containsKey("oaid")
            && !parameters.containsKey("imei")
            && !parameters.containsKey("meid")
            && !parameters.containsKey("device_id")
            && !parameters.containsKey("imeis")
            && !parameters.containsKey("meids")
            && !parameters.containsKey("device_ids")
        ) {
            logger.error("Missing device id's. Please check if Proguard is correctly set with Motrack SDK")
        }
    }

    private fun containsPlayIds(parameters: Map<String, String>?): Boolean {
        return parameters?.containsKey("gps_adid") ?: false
    }

    private fun containsFireIds(parameters: Map<String, String>?): Boolean {
        return parameters?.containsKey("fire_adid") ?: false
    }

    private fun getDefaultActivityPackage(activityKind: ActivityKind): ActivityPackage {
        val activityPackage = ActivityPackage(activityKind)
        activityPackage.clientSdk = deviceInfo.clientSdk
        return activityPackage
    }

    companion object {
        fun addString(parameters: HashMap<String, String>, key: String, value: String?) {
            if (value.isNullOrEmpty()) {
                return
            }

            parameters[key] = value
        }

        fun addBoolean(parameters: HashMap<String, String>, key: String, value: Boolean?) {
            if (value == null) {
                return
            }
            val intValue = if (value) 1 else 0
            addLong(parameters, key, intValue.toLong())
        }

        fun addJsonObject(
            parameters: HashMap<String, String>,
            key: String,
            jsonObject: JSONObject?
        ) {
            if (jsonObject == null) {
                return
            }
            addString(parameters, key, jsonObject.toString())
        }

        fun addMapJson(parameters: HashMap<String, String>, key: String, map: Map<*, *>?) {
            if (map == null) {
                return
            }
            if (map.isEmpty()) {
                return
            }
            val jsonObject = JSONObject(map)
            val jsonString = jsonObject.toString()
            addString(parameters, key, jsonString)
        }

        fun addLong(parameters: HashMap<String, String>, key: String, value: Long?) {
            if (value == null) {
                return
            }

            if (value < 0) {
                return
            }
            val valueString = value.toString()
            addString(parameters, key, valueString)
        }

        private fun addDateInMilliseconds(
            parameters: HashMap<String, String>,
            key: String,
            value: Long
        ) {
            if (value <= 0) {
                return
            }
            val date = Date(value)
            addDate(parameters, key, date)
        }

        private fun addDateInSeconds(
            parameters: HashMap<String, String>,
            key: String,
            value: Long
        ) {
            if (value <= 0) {
                return
            }
            val date = Date(value * 1000)
            addDate(parameters, key, date)
        }

        private fun addDate(parameters: HashMap<String, String>, key: String, value: Date?) {
            if (value == null) {
                return
            }
            val dateString = Util.dateFormatter.format(value)
            addString(parameters, key, dateString)
        }

        private fun addDuration(
            parameters: HashMap<String, String>,
            key: String,
            durationInMilliSeconds: Long
        ) {
            if (durationInMilliSeconds < 0) {
                return
            }
            val durationInSeconds = (durationInMilliSeconds + 500) / 1000
            addLong(parameters, key, durationInSeconds)
        }

        private fun addDouble(parameters: HashMap<String, String>, key: String, value: Double?) {
            if (value == null) {
                return
            }
            val doubleString: String = String.format("%.5f", value)
            addString(parameters, key, doubleString)
        }

        private fun addDoubleWithoutRounding(
            parameters: HashMap<String, String>,
            key: String,
            value: Double?
        ) {
            if (value == null) {
                return
            }
            val doubleString = value.toString()
            addString(parameters, key, doubleString)
        }

        private fun addInteger(parameters: HashMap<String, String>, key: String, value: Int?) {
            if (value == null) {
                return
            }
            val intString = value.toString()
            addString(parameters, key, intString)
        }
    }

    private fun getEventSuffix(event: MotrackEvent): String {
        return if (event.revenue == null) {
            "'${event.eventToken}'"
        } else {
            String.format("(%.5f %s, '%s')", event.revenue, event.currency, event.eventToken)
        }
    }




}