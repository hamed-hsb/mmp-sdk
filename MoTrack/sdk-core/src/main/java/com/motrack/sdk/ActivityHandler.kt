package com.motrack.sdk

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Process
import com.motrack.sdk.Constants.Companion.ACTIVITY_STATE_FILENAME
import com.motrack.sdk.Constants.Companion.ATTRIBUTION_FILENAME
import com.motrack.sdk.Constants.Companion.SESSION_CALLBACK_PARAMETERS_FILENAME
import com.motrack.sdk.Constants.Companion.SESSION_PARTNER_PARAMETERS_FILENAME
import com.motrack.sdk.network.ActivityPackageSender
import com.motrack.sdk.network.IActivityPackageSender
import com.motrack.sdk.scheduler.SingleThreadCachedScheduler
import com.motrack.sdk.scheduler.ThreadExecutor
import com.motrack.sdk.scheduler.TimerCycle
import com.motrack.sdk.scheduler.TimerOnce
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

class ActivityHandler private constructor(private var motrackConfig: MotrackConfig?) :
    IActivityHandler {


    private var executor: ThreadExecutor? = null
    private var packageHandler: IPackageHandler? = null
    private var activityState: ActivityState? = null
    private var logger: ILogger? = null
    private var foregroundTimer: TimerCycle? = null
    private var backgroundTimer: TimerOnce? = null
    private var delayStartTimer: TimerOnce? = null
    var internalState: InternalState? = null
    private val basePath: String? = null
    private val gdprPath: String? = null
    private val subscriptionPath: String? = null

    private var deviceInfo: DeviceInfo? = null
    private var attribution: MotrackAttribution? = null
    private var attributionHandler: IAttributionHandler? = null
    private var sdkClickHandler: ISdkClickHandler? = null
    private var sessionParameters: SessionParameters? = null
    private var installReferrer: InstallReferrer? = null
    private var installReferrerHuawei: InstallReferrerHuawei? = null

    init {
        motrackConfig?.let { init(it) }

        logger = MotrackFactory.getLogger()
        logger!!.lockLogLevel()

        executor = SingleThreadCachedScheduler("ActivityHandler")

        internalState = InternalState()
        // enabled by default
        internalState!!.isEnabled = motrackConfig?.startEnabled ?: true

        // online by default
        internalState!!.isOffline = motrackConfig?.startOffline!!

        // in the background by default
        internalState!!.isInBackground = true

        // delay start not configured by default
        internalState!!.isInDelayedStart = false

        // does not need to update packages by default
        internalState!!.updatePackages = false

        // does not have the session response by default
        internalState!!.sessionResponseProcessed = false

        // does not have first start by default
        internalState!!.firstSdkStart = false

        // preinstall has not been read by default
        internalState!!.preinstallHasBeenRead = false

        executor!!.submit { initI() }
    }

    private fun initI() {
        SESSION_INTERVAL = MotrackFactory.sessionInterval
        SUBSESSION_INTERVAL = MotrackFactory.subsessionInterval
        // get timer values
        FOREGROUND_TIMER_INTERVAL = MotrackFactory.timerInterval
        FOREGROUND_TIMER_START = MotrackFactory.timerStart
        BACKGROUND_TIMER_INTERVAL = MotrackFactory.timerInterval

        // has to be read in the background

        // has to be read in the background
        readAttributionI(motrackConfig!!.context!!)
        readActivityStateI(motrackConfig!!.context!!)

        sessionParameters = SessionParameters()
        readSessionCallbackParametersI(motrackConfig!!.context!!)
        readSessionPartnerParametersI(motrackConfig!!.context!!)

        motrackConfig!!.startEnabled?.let {
            motrackConfig!!.preLaunchActions!!.preLaunchActionsArray.add(object :
                IRunActivityHandler {
                override fun run(activityHandler: ActivityHandler?) {
                    activityHandler!!.setEnabledI(motrackConfig!!.startEnabled!!)
                }
            })
        }

        if (internalState!!.hasFirstSdkStartOccurred()) {
            internalState!!.isEnabled = activityState!!.enabled
            internalState!!.updatePackages = activityState!!.updatePackages
            internalState!!.isFirstLaunch = false
        } else {
            internalState!!.isFirstLaunch = true // first launch if activity state is null
        }

        readConfigFile(motrackConfig!!.context!!)

        deviceInfo = DeviceInfo(motrackConfig!!.context!!, motrackConfig!!.sdkPrefix)

        if (motrackConfig!!.eventBufferingEnabled) {
            logger!!.info("Event buffering is enabled")
        }

        deviceInfo!!.reloadPlayIds(motrackConfig!!.context)
        if (deviceInfo!!.playAdId == null) {
            logger!!.warn("Unable to get Google Play Services Advertising ID at start time")
            if (deviceInfo!!.macSha1.isNullOrEmpty() && deviceInfo!!.macShortMd5.isNullOrEmpty() && deviceInfo!!.androidId.isNullOrEmpty()) {
                logger!!.error("Unable to get any device id's. Please check if Proguard is correctly set with Motrack SDK")
            }
        } else {
            logger!!.info("Google Play Services Advertising ID read correctly at start time")
        }

        motrackConfig?.let { it.defaultTracker?.let { defTracker -> logger!!.info("Default tracker: '$defTracker'") } }

        if (motrackConfig!!.pushToken != null) {
            logger!!.info("Push token: '${motrackConfig!!.pushToken}'")
            if (internalState!!.hasFirstSdkStartOccurred()) {
                // since sdk has already started, try to send current push token
                motrackConfig!!.pushToken?.let { setPushToken(it, false) }
            } else {
                // since sdk has not yet started, save current push token for when it does
                val sharedPreferencesManager = SharedPreferencesManager(getContext())
                sharedPreferencesManager.savePushToken(motrackConfig!!.pushToken!!)
            }
        } else {
            // since sdk has already started, check if there is a saved push from previous runs
            if (internalState!!.hasFirstSdkStartOccurred()) {
                val sharedPreferencesManager = SharedPreferencesManager(getContext())
                val savedPushToken: String? = sharedPreferencesManager.getPushToken()
                savedPushToken?.let { setPushToken(savedPushToken, true) }
            }
        }

        // GDPR
        if (internalState!!.hasFirstSdkStartOccurred()) {
            val sharedPreferencesManager = SharedPreferencesManager(getContext())
            if (sharedPreferencesManager.getGdprForgetMe()) {
                gdprForgetMe()
            } else {
                if (sharedPreferencesManager.getDisableThirdPartySharing()) {
                    disableThirdPartySharing()
                }
                for (motrackThirdPartySharing in motrackConfig!!.preLaunchActions!!.preLaunchMotrackThirdPartyArray) {
                    trackThirdPartySharing(motrackThirdPartySharing)
                }
                if (motrackConfig?.preLaunchActions?.lastMeasurementConsentTracked != null) {
                    trackMeasurementConsent(
                        motrackConfig!!.preLaunchActions!!.lastMeasurementConsentTracked!!
                    )
                }
                motrackConfig!!.preLaunchActions?.preLaunchMotrackThirdPartyArray =
                    ArrayList<MotrackThirdPartySharing>()
                motrackConfig!!.preLaunchActions!!.lastMeasurementConsentTracked = null
            }
        }
        foregroundTimer = TimerCycle(
            { foregroundTimerFired() },
            FOREGROUND_TIMER_START,
            FOREGROUND_TIMER_INTERVAL,
            FOREGROUND_TIMER_NAME
        )

        // create background timer
        motrackConfig?.let {
            if (it.sendInBackground) {
                logger!!.info("Send in background configured")
                backgroundTimer = TimerOnce({ backgroundTimerFired() }, BACKGROUND_TIMER_NAME)
            }
        }

        // configure delay start timer
        if (internalState!!.hasFirstSdkStartNotOccurred() && motrackConfig!!.delayStart != null && motrackConfig!!.delayStart!! > 0.0) {
            logger!!.info("Delay start configured")
            internalState!!.isInDelayedStart = true
            delayStartTimer = TimerOnce({ sendFirstPackages() }, DELAY_START_TIMER_NAME)
        }

//        UtilNetworking.setUserAgent(motrackConfig!!.userAgent)

        val packageHandlerActivitySender: IActivityPackageSender = ActivityPackageSender(
            motrackConfig!!.getUrlStrategy(),
            motrackConfig!!.basePath,
            motrackConfig!!.gdprPath,
            motrackConfig!!.subscriptionPath,
            deviceInfo!!.clientSdk
        )
        packageHandler = MotrackFactory.getPackageHandler(
            this,
            motrackConfig!!.context,
            toSendI(false),
            packageHandlerActivitySender
        )

        val attributionHandlerActivitySender: IActivityPackageSender = ActivityPackageSender(
            motrackConfig!!.getUrlStrategy(),
            motrackConfig!!.basePath,
            motrackConfig!!.gdprPath,
            motrackConfig!!.subscriptionPath,
            deviceInfo!!.clientSdk
        )

        attributionHandler = MotrackFactory.getAttributionHandler(
            this,
            toSendI(false),
            attributionHandlerActivitySender
        )

        val sdkClickHandlerActivitySender: IActivityPackageSender = ActivityPackageSender(
            motrackConfig!!.getUrlStrategy(),
            motrackConfig!!.basePath,
            motrackConfig!!.gdprPath,
            motrackConfig!!.subscriptionPath,
            deviceInfo!!.clientSdk
        )

        sdkClickHandler = MotrackFactory.getSdkClickHandler(
            this,
            toSendI(true),
            sdkClickHandlerActivitySender
        )


        if (isToUpdatePackagesI()) {
            updatePackagesI()
        }

        installReferrer =
            InstallReferrer(motrackConfig!!.context!!, object : InstallReferrerReadListener {
                override fun onInstallReferrerRead(
                    referrerDetails: ReferrerDetails?,
                    referrerApi: String
                ) {
                    sendInstallReferrer(referrerDetails!!, referrerApi)
                }
            })

        installReferrerHuawei =
            InstallReferrerHuawei(motrackConfig!!.context, object : InstallReferrerReadListener {
                override fun onInstallReferrerRead(
                    referrerDetails: ReferrerDetails?,
                    referrerApi: String
                ) {
                    sendInstallReferrer(referrerDetails!!, referrerApi)
                }
            })

        preLaunchActionsI(motrackConfig!!.preLaunchActions!!.preLaunchActionsArray)
        sendReftagReferrerI()

    }

    private fun preLaunchActionsI(preLaunchActionsArray: List<IRunActivityHandler>?) {
        if (preLaunchActionsArray == null) {
            return
        }
        for (preLaunchAction in preLaunchActionsArray) {
            preLaunchAction.run(this)
        }
    }

    private fun sendReftagReferrerI() {
        if (!isEnabledI()) {
            return
        }
        if (internalState!!.hasFirstSdkStartNotOccurred()) {
            return
        }
        sdkClickHandler!!.sendReftagReferrers()
    }

    private fun readActivityStateI(context: Context) {
        activityState = try {
            Util.readObject(
                context, ACTIVITY_STATE_FILENAME, ACTIVITY_STATE_NAME,
                ActivityState::class.java
            )
        } catch (e: Exception) {
            logger!!.error("Failed to read $ACTIVITY_STATE_NAME file (${e.message!!})")
            null
        }
        if (activityState != null) {
            internalState!!.firstSdkStart = true
        }
    }

    private fun readAttributionI(context: Context) {
        attribution = try {
            Util.readObject(
                context, ATTRIBUTION_FILENAME, ATTRIBUTION_NAME,
                MotrackAttribution::class.java
            )
        } catch (e: Exception) {
            logger!!.error("Failed to read $ATTRIBUTION_NAME file (${e.message!!})")
            null
        }
    }

    private fun readSessionCallbackParametersI(context: Context) {
        try {
            sessionParameters!!.callbackParameters = Util.readObject(
                context,
                SESSION_CALLBACK_PARAMETERS_FILENAME,
                SESSION_CALLBACK_PARAMETERS_NAME,
                HashMap::class.java as Class<HashMap<String, String>>
            )

        } catch (e: Exception) {
            logger!!.error("Failed to read $SESSION_CALLBACK_PARAMETERS_NAME file (${e.message!!})")
            sessionParameters!!.callbackParameters = null
        }
    }

    private fun readSessionPartnerParametersI(context: Context) {
        try {
            sessionParameters!!.partnerParameters = Util.readObject(
                context,
                SESSION_PARTNER_PARAMETERS_FILENAME,
                SESSION_PARTNER_PARAMETERS_NAME,
                HashMap::class.java as Class<HashMap<String, String>?>
            )
        } catch (e: Exception) {
            logger!!.error("Failed to read $SESSION_PARTNER_PARAMETERS_NAME file (${e.message!!})")
            sessionParameters!!.partnerParameters = null
        }
    }

    private fun readConfigFile(context: Context) {
        val properties: Properties
        try {
            val inputStream = context.assets.open(DefaultConfig.MOTRACK_CONFIG_FILE_NAME)
            properties = Properties()
            properties.load(inputStream)
        } catch (e: java.lang.Exception) {
            logger!!.debug("${e.message!!} file not found in this app")
            return
        }
        logger!!.verbose("${DefaultConfig.MOTRACK_CONFIG_FILE_NAME} file read and loaded")
        val defaultTracker = properties.getProperty("defaultTracker")
        if (defaultTracker != null) {
            motrackConfig!!.defaultTracker = defaultTracker
        }
    }


    override fun getMotrackConfig(): MotrackConfig {
        return motrackConfig!!
    }

    override fun getDeviceInfo(): DeviceInfo {
        return deviceInfo!!
    }

    override fun getActivityState(): ActivityState? {
        return activityState
    }

    override fun getSessionParameters(): SessionParameters {
        return sessionParameters!!
    }

    override fun init(motrackConfig: MotrackConfig) {
        this.motrackConfig = motrackConfig
    }


    private fun updatePackagesI() {
        // update activity packages
        packageHandler!!.updatePackages(sessionParameters)
        // no longer needs to update packages
        internalState!!.updatePackages = false
        if (activityState != null) {
            activityState!!.updatePackages = false
            writeActivityStateI()
        }
    }

    private fun isToUpdatePackagesI(): Boolean {
        return if (activityState != null) {
            activityState!!.updatePackages
        } else {
            internalState!!.itHasToUpdatePackages()
        }
    }

    private fun updateHandlersStatusAndSendI() {
        // check if it should stop sending
        if (!toSendI()) {
            pauseSendingI()
            return
        }
        resumeSendingI()

        // if event buffering is not enabled
        if (!motrackConfig!!.eventBufferingEnabled ||  // or if it's the first launch and it hasn't received the session response
            internalState!!.isFirstLaunch && internalState!!.hasSessionResponseNotBeenProcessed()
        ) {
            // try to send
            packageHandler!!.sendFirstPackage()
        }
    }


    private fun pauseSendingI() {
        attributionHandler!!.pauseSending()
        packageHandler!!.pauseSending()
        // the conditions to pause the sdk click handler are less restrictive
        // it's possible for the sdk click handler to be active while others are paused
        if (!toSendI(true)) {
            sdkClickHandler!!.pauseSending()
        } else {
            sdkClickHandler!!.resumeSending()
        }
    }

    private fun resumeSendingI() {
        attributionHandler!!.resumeSending()
        packageHandler!!.resumeSending()
        sdkClickHandler!!.resumeSending()
    }

    override fun onResume() {
        internalState!!.isInBackground = false
        executor!!.submit {
            delayStartI()
            stopBackgroundTimerI()
            startForegroundTimerI()
            logger!!.verbose("Subsession start")
            startI()
        }
    }

    private fun delayStartI() {
        // it's not configured to start delayed or already finished
        if (internalState!!.isNotInDelayedStart()) {
            return
        }

        // the delay has already started
        if (isToUpdatePackagesI()) {
            return
        }

        // check against max start delay
        var delayStartSeconds =
            if (motrackConfig!!.delayStart != null) motrackConfig!!.delayStart else 0.0
        val maxDelayStartMilli: Long = MotrackFactory.maxDelayStart
        var delayStartMilli = (delayStartSeconds!! * 1000).toLong()
        if (delayStartMilli > maxDelayStartMilli) {
            val maxDelayStartSeconds = (maxDelayStartMilli / 1000).toDouble()
            val delayStartFormatted = Util.SecondsDisplayFormat.format(delayStartSeconds)
            val maxDelayStartFormatted = Util.SecondsDisplayFormat.format(maxDelayStartSeconds)
            logger!!.warn("Delay start of $delayStartFormatted seconds bigger than max allowed value of $maxDelayStartFormatted seconds")
            delayStartMilli = maxDelayStartMilli
            delayStartSeconds = maxDelayStartSeconds
        }
        val delayStartFormatted = Util.SecondsDisplayFormat.format(delayStartSeconds)
        logger!!.info("Waiting $delayStartFormatted seconds before starting first session")
        delayStartTimer!!.startIn(delayStartMilli)
        internalState!!.updatePackages = true
        if (activityState != null) {
            activityState!!.updatePackages = true
            writeActivityStateI()
        }
    }


    override fun onPause() {
        internalState!!.isInBackground = true
        executor!!.submit {
            stopForegroundTimerI()
            startBackgroundTimerI()
            logger!!.verbose("Subsession end")
            endI()
        }
    }

    private fun endI() {
        // pause sending if it's not allowed to send
        if (!toSendI()) {
            pauseSendingI()
        }
        if (updateActivityStateI(System.currentTimeMillis())) {
            writeActivityStateI()
        }
    }

    override fun trackEvent(event: MotrackEvent) {
        executor!!.submit {
            if (internalState!!.hasFirstSdkStartNotOccurred()) {
                logger!!.warn(
                    """
                         Event tracked before first activity resumed.
                         If it was triggered in the Application class, it might timestamp or even send an install long before the user opens the app.
                         Please check https://github.com/motrack/AndroidSdk#can-i-trigger-an-event-at-application-launch for more information.
                         """.trimIndent()
                )
                startI()
            }
            trackEventI(event)
        }
    }

    private fun trackEventI(event: MotrackEvent) {
        if (!checkActivityStateI(activityState)) return
        if (!isEnabledI()) return
        if (!checkEventI(event)) return
        if (!checkOrderIdI(event.orderId)) return
        if (activityState!!.isGdprForgotten) return
        val now = System.currentTimeMillis()
        activityState!!.eventCount++
        updateActivityStateI(now)
        val eventBuilder = PackageBuilder(motrackConfig!!,deviceInfo!!, activityState, sessionParameters, now)
        val eventPackage = eventBuilder.buildEventPackage(event, internalState!!.isInDelayedStart)
        packageHandler!!.addPackage(eventPackage)
        if (motrackConfig!!.eventBufferingEnabled) {
            logger!!.info("Buffered event ${eventPackage.suffix!!}")
        } else {
            packageHandler!!.sendFirstPackage()
        }

        // if it is in the background and it can send, start the background timer
        if (motrackConfig!!.sendInBackground && internalState!!.isInBackground) {
            startBackgroundTimerI()
        }
        writeActivityStateI()
    }

    private fun checkOrderIdI(orderId: String?): Boolean {
        if (orderId == null || orderId.isEmpty()) {
            return true // no order ID given
        }
        if (activityState!!.findOrderId(orderId)) {
            logger!!.info("Skipping duplicated order ID '$orderId'")
            return false // order ID found -> used already
        }
        activityState!!.addOrderId(orderId)
        logger!!.verbose("Added order ID '$orderId'")
        // activity state will get written by caller
        return true
    }

    private fun checkEventI(event: MotrackEvent?): Boolean {
        if (event == null) {
            logger!!.error("Event missing")
            return false
        }
        if (!event.isValid()) {
            logger!!.error("Event not initialized correctly")
            return false
        }
        return true
    }

    private fun startI() {
        // check if it's the first sdk start
        if (internalState!!.hasFirstSdkStartNotOccurred()) {
            MotrackSigner.onResume(logger!!)
            startFirstSessionI()
            return
        }

        // it shouldn't start if it was disabled after a first session
        if (!activityState!!.enabled) {
            return
        }
        MotrackSigner.onResume(logger!!)
        updateHandlersStatusAndSendI()
        processSessionI()
        checkAttributionStateI()
        processCachedDeeplinkI()
    }

    private fun checkAttributionStateI() {
        if (!checkActivityStateI(activityState)) {
            return
        }

        // if it's the first launch
        if (internalState!!.isFirstLaunch) {
            // and it hasn't received the session response
            if (internalState!!.hasSessionResponseNotBeenProcessed()) {
                return
            }
        }

        // if there is already an attribution saved and there was no attribution being asked
        if (attribution != null && !activityState!!.askingAttribution) {
            return
        }
        attributionHandler!!.getAttribution()
    }


    private fun startFirstSessionI() {
        activityState = ActivityState()
        internalState!!.firstSdkStart = true

        // still update handlers status
        updateHandlersStatusAndSendI()
        val now = System.currentTimeMillis()
        val sharedPreferencesManager = SharedPreferencesManager(getContext())
        activityState!!.pushToken = sharedPreferencesManager.getPushToken()
        // activityState.isGdprForgotten = sharedPreferencesManager.getGdprForgetMe();

        // track the first session package only if it's enabled
        if (internalState!!.isEnabled) {
            if (sharedPreferencesManager.getGdprForgetMe()) {
                gdprForgetMeI()
            } else {
                // check if disable third party sharing request came, then send it first
                if (sharedPreferencesManager.getDisableThirdPartySharing()) {
                    disableThirdPartySharingI()
                }
                for (motrackThirdPartySharing in motrackConfig!!.preLaunchActions!!.preLaunchMotrackThirdPartyArray) {
                    trackThirdPartySharingI(motrackThirdPartySharing)
                }
                if (motrackConfig!!.preLaunchActions!!.lastMeasurementConsentTracked != null) {
                    trackMeasurementConsentI(
                        motrackConfig!!.preLaunchActions!!.lastMeasurementConsentTracked!!
                    )
                }
                motrackConfig!!.preLaunchActions!!.preLaunchMotrackThirdPartyArray = ArrayList()
                motrackConfig!!.preLaunchActions!!.lastMeasurementConsentTracked = null
                activityState!!.sessionCount = 1 // this is the first session
                transferSessionPackageI(now)
                checkAfterNewStartI(sharedPreferencesManager)
            }
        }
        activityState!!.resetSessionAttributes(now)
        activityState!!.enabled = internalState!!.isEnabled
        activityState!!.updatePackages = internalState!!.itHasToUpdatePackages()
        writeActivityStateI()
        sharedPreferencesManager.removePushToken()
        sharedPreferencesManager.removeGdprForgetMe()
        sharedPreferencesManager.removeDisableThirdPartySharing()

        // check for cached deep links
        processCachedDeeplinkI()

        // don't check attribution right after first sdk start
    }

    private fun processCachedDeeplinkI() {
        if (!checkActivityStateI(activityState)) {
            return
        }
        val sharedPreferencesManager = SharedPreferencesManager(getContext())
        val cachedDeeplinkUrl = sharedPreferencesManager.getDeeplinkUrl()
        val cachedDeeplinkClickTime = sharedPreferencesManager.getDeeplinkClickTime()
        if (cachedDeeplinkUrl == null) {
            return
        }
        if (cachedDeeplinkClickTime == -1L) {
            return
        }
        readOpenUrl(Uri.parse(cachedDeeplinkUrl), cachedDeeplinkClickTime)
        sharedPreferencesManager.removeDeeplink()
    }

    private fun processSessionI() {
        if (activityState!!.isGdprForgotten) {
            return
        }
        val now = System.currentTimeMillis()
        val lastInterval = now - activityState!!.lastActivity
        if (lastInterval < 0) {
            logger!!.error(TIME_TRAVEL)
            activityState!!.lastActivity = now
            writeActivityStateI()
            return
        }

        // new session
        if (lastInterval > SESSION_INTERVAL) {
            trackNewSessionI(now)
            checkAfterNewStartI()
            return
        }

        // new subsession
        if (lastInterval > SUBSESSION_INTERVAL) {
            activityState!!.subsessionCount++
            activityState!!.sessionLength += lastInterval
            activityState!!.lastActivity = now
            logger!!.verbose("Started subsession ${activityState!!.subsessionCount} of session ${activityState!!.sessionCount}")
            writeActivityStateI()
            checkForPreinstallI()

            // Try to check if there's new referrer information.
            installReferrer!!.startConnection()
            installReferrerHuawei!!.readReferrer()
            return
        }
        logger!!.verbose("Time span since last activity too short for a new subsession")
    }

    override fun finishedTrackingActivity(responseData: ResponseData) {
        // redirect session responses to attribution handler to check for attribution information
        if (responseData is SessionResponseData) {
            logger!!.debug("Finished tracking session")
            attributionHandler!!.checkSessionResponse((responseData as SessionResponseData?)!!)
            return
        }
        // redirect sdk click responses to attribution handler to check for attribution information
        if (responseData is SdkClickResponseData) {
            checkForInstallReferrerInfo(responseData)
            attributionHandler!!.checkSdkClickResponse((responseData as SdkClickResponseData?)!!)
            return
        }
        // check if it's an event response
        if (responseData is EventResponseData) {
            launchEventResponseTasks((responseData as EventResponseData?)!!)
            return
        }
    }

    private fun checkForInstallReferrerInfo(responseData: SdkClickResponseData) {
        if (!responseData.isInstallReferrer) {
            return
        }

        val isInstallReferrerHuaweiAds = responseData.referrerApi != null &&
                responseData.referrerApi.equals(
                    Constants.REFERRER_API_HUAWEI_ADS,
                    ignoreCase = true
                )
        if (isInstallReferrerHuaweiAds) {
            activityState!!.clickTimeHuawei = responseData.clickTime!!
            activityState!!.installBeginHuawei = responseData.installBegin!!
            activityState!!.installReferrerHuawei = responseData.installReferrer
            writeActivityStateI()
            return
        }

        val isInstallReferrerHuaweiAppGallery = responseData.referrerApi != null &&
                responseData.referrerApi.equals(
                    Constants.REFERRER_API_HUAWEI_APP_GALLERY,
                    ignoreCase = true
                )

        if (isInstallReferrerHuaweiAppGallery) {
            activityState!!.clickTimeHuawei = responseData.clickTime!!
            activityState!!.installBeginHuawei = responseData.installBegin!!
            activityState!!.installReferrerHuaweiAppGallery = responseData.installReferrer
            writeActivityStateI()
            return
        }

        activityState!!.clickTime = responseData.clickTime!!
        activityState!!.installBegin = responseData.installBegin!!
        activityState!!.installReferrer = responseData.installReferrer
        activityState!!.clickTimeServer = responseData.clickTimeServer!!
        activityState!!.installBeginServer = responseData.installBeginServer!!
        activityState!!.installVersion = responseData.installVersion
        activityState!!.googlePlayInstant = responseData.googlePlayInstant

        writeActivityStateI()
    }

    override fun setEnabled(enabled: Boolean) {
        executor!!.submit { setEnabledI(enabled) }
    }

    override fun setOfflineMode(enabled: Boolean) {
        executor!!.submit { setOfflineModeI(enabled) }
    }

    private fun setOfflineModeI(offline: Boolean) {
        // compare with the internal state
        if (!hasChangedStateI(
                internalState!!.isOffline, offline,
                "Motrack already in offline mode",
                "Motrack already in online mode"
            )
        ) {
            return
        }
        internalState!!.isOffline = offline
        if (internalState!!.hasFirstSdkStartNotOccurred()) {
            updateStatusI(
                offline,
                "Handlers will start paused due to SDK being offline",
                "Handlers will still start as paused",
                "Handlers will start as active due to SDK being online"
            )
            return
        }
        updateStatusI(
            offline,
            "Pausing handlers to put SDK offline mode",
            "Handlers remain paused",
            "Resuming handlers to put SDK in online mode"
        )
    }

    override fun isEnabled(): Boolean {
        return isEnabledI()
    }

    private fun isEnabledI(): Boolean {
        return if (activityState != null) {
            activityState!!.enabled
        } else {
            internalState!!.isEnabled
        }
    }

    override fun readOpenUrl(url: Uri, clickTime: Long) {
        executor!!.submit { readOpenUrlI(url, clickTime) }
    }

    private fun readOpenUrlI(url: Uri, clickTime: Long) {
        if (!isEnabledI()) {
            return
        }
        if (Util.isUrlFilteredOut(url)) {
            logger!!.debug("Deep link ($url) processing skipped")
            return
        }
        val sdkClickPackage = PackageFactory.buildDeeplinkSdkClickPackage(
            url,
            clickTime,
            activityState!!,
            motrackConfig!!,
            deviceInfo!!,
            sessionParameters!!
        ) ?: return
        sdkClickHandler!!.sendSdkClick(sdkClickPackage)
    }

    override fun updateAttributionI(attribution: MotrackAttribution?): Boolean {
        if (attribution == null) {
            return false
        }
        if (attribution == this.attribution) {
            return false
        }
        this.attribution = attribution
        writeAttributionI()
        return true
    }

    override fun launchEventResponseTasks(eventResponseData: EventResponseData) {
        executor!!.submit { launchEventResponseTasksI(eventResponseData) }
    }

    override fun launchSessionResponseTasks(sessionResponseData: SessionResponseData) {
        executor!!.submit { launchSessionResponseTasksI(sessionResponseData) }
    }

    private fun launchSessionResponseTasksI(sessionResponseData: SessionResponseData) {
        logger!!.debug("Launching SessionResponse tasks")

        // try to update adid from response
        updateAdidI(sessionResponseData.adid)

        // use the same handler to ensure that all tasks are executed sequentially
        val handler = Handler(motrackConfig!!.context!!.mainLooper)

        // try to update the attribution
        val attributionUpdated = updateAttributionI(sessionResponseData.attribution)

        // if attribution changed, launch attribution changed delegate
        if (attributionUpdated) {
            launchAttributionListenerI(handler)
        }

        // if attribution didn't update and it's still null
        // ask for attribution
        if (attribution == null && !activityState!!.askingAttribution) {
            attributionHandler!!.getAttribution()
        }

        // mark install as tracked on success
        if (sessionResponseData.success) {
            val sharedPreferencesManager = SharedPreferencesManager(getContext())
            sharedPreferencesManager.setInstallTracked()
        }

        // launch Session tracking listener if available
        launchSessionResponseListenerI(sessionResponseData, handler)

        // mark session response has processed
        internalState!!.sessionResponseProcessed = true
    }

    private fun launchSessionResponseListenerI(
        sessionResponseData: SessionResponseData,
        handler: Handler
    ) {
        // success callback
        if (sessionResponseData.success && motrackConfig!!.onSessionTrackingSucceededListener != null) {
            logger!!.debug("Launching success session tracking listener")
            // add it to the handler queue
            val runnable = Runnable {
                if (motrackConfig == null) {
                    return@Runnable
                }
                if (motrackConfig!!.onSessionTrackingSucceededListener == null) {
                    return@Runnable
                }
                sessionResponseData.getSuccessResponseData()?.let {
                    motrackConfig!!.onSessionTrackingSucceededListener!!.onFinishedSessionTrackingSucceeded(
                        it
                    )
                }
            }
            handler.post(runnable)
            return
        }
        // failure callback
        if (!sessionResponseData.success && motrackConfig!!.onSessionTrackingFailedListener != null) {
            logger!!.debug("Launching failed session tracking listener")
            // add it to the handler queue
            val runnable = Runnable {
                if (motrackConfig == null) {
                    return@Runnable
                }
                if (motrackConfig!!.onSessionTrackingFailedListener == null) {
                    return@Runnable
                }
                sessionResponseData.getFailureResponseData()?.let {
                    motrackConfig!!.onSessionTrackingFailedListener!!.onFinishedSessionTrackingFailed(
                        it
                    )
                }
            }
            handler.post(runnable)
            return
        }
    }


    private fun launchEventResponseTasksI(eventResponseData: EventResponseData) {
        // try to update adid from response
        updateAdidI(eventResponseData.adid)
        val handler = Handler(motrackConfig!!.context!!.mainLooper)

        // success callback
        if (eventResponseData.success && motrackConfig!!.onEventTrackingSucceededListener != null) {
            logger!!.debug("Launching success event tracking listener")
            // add it to the handler queue
            val runnable = Runnable {
                if (motrackConfig == null) {
                    return@Runnable
                }
                if (motrackConfig!!.onEventTrackingSucceededListener == null) {
                    return@Runnable
                }
                motrackConfig!!.onEventTrackingSucceededListener!!.onFinishedEventTrackingSucceeded(
                    eventResponseData.getSuccessResponseData()!!
                )
            }
            handler.post(runnable)
            return
        }
        // failure callback
        if (!eventResponseData.success && motrackConfig!!.onEventTrackingFailedListener != null) {
            logger!!.debug("Launching failed event tracking listener")
            // add it to the handler queue
            val runnable = Runnable {
                if (motrackConfig == null) {
                    return@Runnable
                }
                if (motrackConfig!!.onEventTrackingFailedListener == null) {
                    return@Runnable
                }
                motrackConfig!!.onEventTrackingFailedListener!!.onFinishedEventTrackingFailed(
                    eventResponseData.getFailureResponseData()!!
                )
            }
            handler.post(runnable)
            return
        }
    }

    private fun launchAttributionListenerI(handler: Handler) {
        if (motrackConfig!!.onAttributionChangedListener == null) {
            return
        }
        // add it to the handler queue
        val runnable = Runnable {
            if (motrackConfig == null) {
                return@Runnable
            }
            if (motrackConfig!!.onAttributionChangedListener == null) {
                return@Runnable
            }
            attribution?.let {
                motrackConfig!!.onAttributionChangedListener!!.onAttributionChanged(
                    it
                )
            }
        }
        handler.post(runnable)
    }

    override fun launchSdkClickResponseTasks(sdkClickResponseData: SdkClickResponseData) {
        executor!!.submit { launchSdkClickResponseTasksI(sdkClickResponseData) }
    }

    private fun launchSdkClickResponseTasksI(sdkClickResponseData: SdkClickResponseData) {
        // try to update adid from response
        updateAdidI(sdkClickResponseData.adid)

        // use the same handler to ensure that all tasks are executed sequentially
        val handler = Handler(motrackConfig!!.context!!.mainLooper)

        // try to update the attribution
        val attributionUpdated = updateAttributionI(sdkClickResponseData.attribution)

        // if attribution changed, launch attribution changed delegate
        if (attributionUpdated) {
            launchAttributionListenerI(handler)
        }
    }

    private fun updateAdidI(adid: String?) {
        if (adid == null) {
            return
        }
        if (adid == activityState!!.adid) {
            return
        }
        activityState!!.adid = adid
        writeActivityStateI()
        return
    }

    override fun launchAttributionResponseTasks(attributionResponseData: AttributionResponseData) {
        executor!!.submit { launchAttributionResponseTasksI(attributionResponseData) }
    }

    private fun launchAttributionResponseTasksI(attributionResponseData: AttributionResponseData) {
        // try to update adid from response
        updateAdidI(attributionResponseData.adid)
        val handler = Handler(motrackConfig!!.context!!.mainLooper)

        // try to update the attribution
        val attributionUpdated = updateAttributionI(attributionResponseData.attribution)

        // if attribution changed, launch attribution changed delegate
        if (attributionUpdated) {
            launchAttributionListenerI(handler)
        }

        // if there is any, try to launch the deeplink
        prepareDeeplinkI(attributionResponseData.deeplink, handler)
    }

    private fun prepareDeeplinkI(deeplink: Uri?, handler: Handler) {
        if (deeplink == null) {
            return
        }
        logger!!.info("Deferred deeplink received ($deeplink)")
        val deeplinkIntent: Intent = createDeeplinkIntentI(deeplink)
        val runnable = Runnable {
            if (motrackConfig == null) {
                return@Runnable
            }
            var toLaunchDeeplink = true
            if (motrackConfig!!.onDeeplinkResponseListener != null) {
                toLaunchDeeplink =
                    motrackConfig!!.onDeeplinkResponseListener!!.launchReceivedDeeplink(deeplink)
            }
            if (toLaunchDeeplink) {
                launchDeeplinkMain(deeplinkIntent, deeplink)
            }
        }
        handler.post(runnable)
    }

    private fun launchDeeplinkMain(deeplinkIntent: Intent, deeplink: Uri) {
        // Verify it resolves
        val packageManager: PackageManager = motrackConfig!!.context!!.packageManager
        val activities = packageManager.queryIntentActivities(deeplinkIntent, 0)
        val isIntentSafe = activities.size > 0

        // Start an activity if it's safe
        if (!isIntentSafe) {
            logger!!.error("Unable to open deferred deep link ($deeplink)")
            return
        }

        // add it to the handler queue
        logger!!.info("Open deferred deep link ($deeplink)")
        motrackConfig!!.context!!.startActivity(deeplinkIntent)
    }

    private fun createDeeplinkIntentI(deeplink: Uri): Intent {
        val mapIntent: Intent = if (motrackConfig!!.deepLinkComponent == null) {
            Intent(Intent.ACTION_VIEW, deeplink)
        } else {
            Intent(
                Intent.ACTION_VIEW,
                deeplink,
                motrackConfig!!.context,
                motrackConfig!!.deepLinkComponent
            )
        }
        mapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        mapIntent.setPackage(motrackConfig!!.context!!.packageName)
        return mapIntent
    }

    override fun sendRefTagReferrer() {
        executor!!.submit { sendReftagReferrerI() }
    }

    override fun sendPreinstallReferrer() {
        executor!!.submit { sendPreinstallReferrerI() }
    }


    override fun sendInstallReferrer(
        referrerDetails: ReferrerDetails,
        referrerApi: String
    ) {
        executor!!.submit { sendInstallReferrerI(referrerDetails, referrerApi) }
    }


    override fun setAskingAttribution(askingAttribution: Boolean) {
        executor!!.submit { setAskingAttributionI(askingAttribution) }
    }

    private fun setAskingAttributionI(askingAttribution: Boolean) {
        activityState!!.askingAttribution = askingAttribution
        writeActivityStateI()
    }

    override fun sendFirstPackages() {
        executor!!.submit { sendFirstPackagesI() }
    }

    private fun sendFirstPackagesI() {
        if (internalState!!.isNotInDelayedStart()) {
            logger!!.info("Start delay expired or never configured")
            return
        }

        // update packages in queue
        updatePackagesI()
        // no longer is in delay start
        internalState!!.isInDelayedStart = false
        // cancel possible still running timer if it was called by user
        delayStartTimer!!.cancel()
        // and release timer
        delayStartTimer = null
        // update the status and try to send first package
        updateHandlersStatusAndSendI()
    }

    override fun addSessionCallbackParameter(key: String?, value: String?) {
        executor!!.submit { addSessionCallbackParameterI(key, value) }
    }

    fun addSessionCallbackParameterI(key: String?, value: String?) {
        if (!Util.isValidParameter(key, "key", "Session Callback")) return
        if (!Util.isValidParameter(value, "value", "Session Callback")) return
        if (sessionParameters!!.callbackParameters == null) {
            sessionParameters!!.callbackParameters = LinkedHashMap()
        }
        val oldValue = sessionParameters!!.callbackParameters!![key]
        if (value == oldValue) {
            logger!!.verbose("Key ${key!!} already present with the same value")
            return
        }
        if (oldValue != null) {
            logger!!.warn("Key ${key!!} will be overwritten")
        }
        sessionParameters!!.callbackParameters!![key!!] = value!!
        writeSessionCallbackParametersI()
    }

    override fun addSessionPartnerParameter(key: String?, value: String?) {
        executor!!.submit { addSessionPartnerParameterI(key, value) }
    }

    fun addSessionPartnerParameterI(key: String?, value: String?) {
        if (!Util.isValidParameter(key, "key", "Session Partner")) return
        if (!Util.isValidParameter(value, "value", "Session Partner")) return
        if (sessionParameters!!.partnerParameters == null) {
            sessionParameters!!.partnerParameters = LinkedHashMap()
        }
        val oldValue = sessionParameters!!.partnerParameters!![key]
        if (value == oldValue) {
            logger!!.verbose("Key ${key!!} already present with the same value")
            return
        }
        if (oldValue != null) {
            logger!!.warn("Key ${key!!} will be overwritten")
        }
        sessionParameters!!.partnerParameters!![key!!] = value!!
        writeSessionPartnerParametersI()
    }

    override fun removeSessionCallbackParameter(key: String?) {
        executor!!.submit { removeSessionCallbackParameterI(key) }
    }

    fun removeSessionCallbackParameterI(key: String?) {
        if (!Util.isValidParameter(key, "key", "Session Callback")) return
        if (sessionParameters!!.callbackParameters == null) {
            logger!!.warn("Session Callback parameters are not set")
            return
        }
        val oldValue = sessionParameters!!.callbackParameters!!.remove(key)
        if (oldValue == null) {
            logger!!.warn("Key ${key!!} does not exist")
            return
        }
        logger!!.debug("Key ${key!!} will be removed")
        writeSessionCallbackParametersI()
    }

    override fun removeSessionPartnerParameter(key: String?) {
        executor!!.submit { removeSessionPartnerParameterI(key) }
    }

    fun removeSessionPartnerParameterI(key: String?) {
        if (!Util.isValidParameter(key, "key", "Session Partner")) return
        if (sessionParameters!!.partnerParameters == null) {
            logger!!.warn("Session Partner parameters are not set")
            return
        }
        val oldValue = sessionParameters!!.partnerParameters!!.remove(key)
        if (oldValue == null) {
            logger!!.warn("Key ${key!!}does not exist")
            return
        }
        logger!!.debug("Key ${key!!} will be removed")
        writeSessionPartnerParametersI()
    }

    override fun resetSessionCallbackParameters() {
        executor!!.submit { resetSessionCallbackParametersI() }
    }

    fun resetSessionCallbackParametersI() {
        if (sessionParameters!!.callbackParameters == null) {
            logger!!.warn("Session Callback parameters are not set")
        }
        sessionParameters!!.callbackParameters = null
        writeSessionCallbackParametersI()
    }

    override fun resetSessionPartnerParameters() {
        executor!!.submit { resetSessionPartnerParametersI() }
    }

    fun resetSessionPartnerParametersI() {
        if (sessionParameters!!.partnerParameters == null) {
            logger!!.warn("Session Partner parameters are not set")
        }
        sessionParameters!!.partnerParameters = null
        writeSessionPartnerParametersI()
    }


    override fun teardown() {
        backgroundTimer?.teardown()
        foregroundTimer?.teardown()
        delayStartTimer?.teardown()
        executor?.teardown()
        packageHandler?.teardown()
        attributionHandler?.teardown()
        sdkClickHandler?.teardown()
        if (sessionParameters != null) {
            sessionParameters!!.callbackParameters?.clear()
            sessionParameters!!.callbackParameters?.clear()
        }

        teardownActivityStateS()
        teardownAttributionS()
        teardownAllSessionParametersS()

        packageHandler = null
        logger = null
        foregroundTimer = null
        executor = null
        backgroundTimer = null
        delayStartTimer = null
        internalState = null
        deviceInfo = null
        motrackConfig = null
        attributionHandler = null
        sdkClickHandler = null
        sessionParameters = null
    }

    private fun updateActivityStateI(now: Long): Boolean {
        if (!checkActivityStateI(activityState)) {
            return false
        }
        val lastInterval = now - activityState!!.lastActivity

        // ignore late updates
        if (lastInterval > SESSION_INTERVAL) {
            return false
        }
        activityState!!.lastActivity = now
        if (lastInterval < 0) {
            logger!!.error(TIME_TRAVEL)
        } else {
            activityState!!.sessionLength += lastInterval
            activityState!!.timeSpent += lastInterval
        }
        return true
    }

    override fun setPushToken(token: String, preSaved: Boolean) {
        executor!!.submit(Runnable {
            if (!preSaved) {
                val sharedPreferencesManager = SharedPreferencesManager(getContext())
                sharedPreferencesManager.savePushToken(token)
            }
            if (internalState!!.hasFirstSdkStartNotOccurred()) {
                // No install has been tracked so far.
                // Push token is saved, ready for the session package to pick it up.
                return@Runnable
            } else {
                setPushTokenI(token)
            }
        })
    }

    private fun setPushTokenI(token: String?) {
        if (!checkActivityStateI(activityState)) {
            return
        }
        if (!isEnabledI()) {
            return
        }
        if (activityState!!.isGdprForgotten) {
            return
        }
        if (token == null) {
            return
        }
        if (token == activityState!!.pushToken) {
            return
        }

        // save new push token
        activityState!!.pushToken = token
        writeActivityStateI()
        val now = System.currentTimeMillis()
        val infoPackageBuilder =
            PackageBuilder(motrackConfig!!, deviceInfo!!, activityState, sessionParameters, now)
        val infoPackage: ActivityPackage = infoPackageBuilder.buildInfoPackage(Constants.PUSH)
        packageHandler!!.addPackage(infoPackage)

        // If push token was cached, remove it.
        val sharedPreferencesManager = SharedPreferencesManager(getContext())
        sharedPreferencesManager.removePushToken()
        if (motrackConfig!!.eventBufferingEnabled) {
            logger!!.info("Buffered event ${infoPackage.suffix!!}")
        } else {
            packageHandler!!.sendFirstPackage()
        }
    }

    private fun setEnabledI(enabled: Boolean) {
        // compare with the saved or internal state
        if (!hasChangedStateI(
                isEnabledI(), enabled,
                "Motrack already enabled", "Motrack already disabled"
            )
        ) {
            return
        }
        if (enabled) {
            if (activityState != null && activityState!!.isGdprForgotten) {
                logger!!.error("Re-enabling SDK not possible for forgotten user")
                return
            }
        }

        // save new enabled state in internal state
        internalState!!.isEnabled = enabled
        if (internalState!!.hasFirstSdkStartNotOccurred()) {
            updateStatusI(
                !enabled,
                "Handlers will start as paused due to the SDK being disabled",
                "Handlers will still start as paused",
                "Handlers will start as active due to the SDK being enabled"
            )
            return
        }
        activityState!!.enabled = enabled
        writeActivityStateI()
        if (enabled) {
            val sharedPreferencesManager = SharedPreferencesManager(getContext())
            if (sharedPreferencesManager.getGdprForgetMe()) {
                gdprForgetMeI()
            } else {
                if (sharedPreferencesManager.getDisableThirdPartySharing()) {
                    disableThirdPartySharingI()
                }
                for (motrackThirdPartySharing in motrackConfig?.preLaunchActions!!.preLaunchMotrackThirdPartyArray) {
                    trackThirdPartySharingI(motrackThirdPartySharing)
                }
                if (motrackConfig!!.preLaunchActions!!.lastMeasurementConsentTracked != null) {
                    trackMeasurementConsentI(
                        motrackConfig!!.preLaunchActions!!.lastMeasurementConsentTracked!!
                    )
                }
                motrackConfig!!.preLaunchActions!!.preLaunchMotrackThirdPartyArray = ArrayList()
                motrackConfig!!.preLaunchActions!!.lastMeasurementConsentTracked = null
            }

            // check if install was tracked
            if (!sharedPreferencesManager.getInstallTracked()) {
                logger!!.debug("Detected that install was not tracked at enable time")
                val now = System.currentTimeMillis()
                trackNewSessionI(now)
            }
            checkAfterNewStartI(sharedPreferencesManager)
        }
        updateStatusI(
            !enabled,
            "Pausing handlers due to SDK being disabled",
            "Handlers remain paused",
            "Resuming handlers due to SDK being enabled"
        )
    }

    private fun checkAfterNewStartI() {
        val sharedPreferencesManager = SharedPreferencesManager(getContext())
        checkAfterNewStartI(sharedPreferencesManager)
    }

    private fun checkAfterNewStartI(sharedPreferencesManager: SharedPreferencesManager) {
        // check if there is a saved push token to send
        val pushToken = sharedPreferencesManager.getPushToken()
        if (pushToken != null && pushToken != activityState!!.pushToken) {
            // queue set push token
            setPushToken(pushToken, true)
        }

        // check if there are token to send
        val referrers: Any = sharedPreferencesManager.getRawReferrerArray()
        if (referrers != null) {
            // queue send referrer tag
            sendRefTagReferrer()
        }
        checkForPreinstallI()

        // try to read and send the install referrer
        installReferrer!!.startConnection()
        installReferrerHuawei!!.readReferrer()
    }

    private fun checkForPreinstallI() {
        if (activityState == null) return
        if (!activityState!!.enabled) return
        if (activityState!!.isGdprForgotten) return

        // sending preinstall referrer doesn't require preinstall tracking flag to be enabled
        sendPreinstallReferrerI()

        if (!motrackConfig!!.preinstallTrackingEnabled) return
        if (internalState!!.hasPreinstallBeenRead()) return

        if (deviceInfo!!.packageName == null || deviceInfo!!.packageName!!.isEmpty()) {
            logger!!.debug("Can't read preinstall payload, invalid package name")
            return
        }

        val sharedPreferencesManager = SharedPreferencesManager(getContext())
        var readStatus = sharedPreferencesManager.getPreinstallPayloadReadStatus()

        if (PreinstallUtil.hasAllLocationsBeenRead(readStatus)) {
            internalState!!.preinstallHasBeenRead = true
            return
        }

        // 1. try reading preinstall payload from standard system property
        if (PreinstallUtil.hasNotBeenRead(Constants.SYSTEM_PROPERTIES, readStatus)) {
            val payloadSystemProperty = PreinstallUtil.getPayloadFromSystemProperty(
                deviceInfo!!.packageName!!, logger!!
            )
            if (payloadSystemProperty != null && payloadSystemProperty.isNotEmpty()) {
                sdkClickHandler!!.sendPreinstallPayload(
                    payloadSystemProperty,
                    Constants.SYSTEM_PROPERTIES
                )
            } else {
                readStatus = PreinstallUtil.markAsRead(Constants.SYSTEM_PROPERTIES, readStatus)
            }
        }

        // 2. try reading preinstall payload from system property using reflection
        if (PreinstallUtil.hasNotBeenRead(Constants.SYSTEM_PROPERTIES_REFLECTION, readStatus)) {
            val payloadSystemPropertyReflection =
                PreinstallUtil.getPayloadFromSystemPropertyReflection(
                    deviceInfo!!.packageName!!, logger!!
                )
            if (payloadSystemPropertyReflection != null && payloadSystemPropertyReflection.isNotEmpty()) {
                sdkClickHandler!!.sendPreinstallPayload(
                    payloadSystemPropertyReflection,
                    Constants.SYSTEM_PROPERTIES_REFLECTION
                )
            } else {
                readStatus =
                    PreinstallUtil.markAsRead(Constants.SYSTEM_PROPERTIES_REFLECTION, readStatus)
            }
        }


        // 3. try reading preinstall payload from system property file path
        if (PreinstallUtil.hasNotBeenRead(Constants.SYSTEM_PROPERTIES_PATH, readStatus)) {
            val payloadSystemPropertyFilePath = PreinstallUtil.getPayloadFromSystemPropertyFilePath(
                deviceInfo!!.packageName!!, logger!!
            )
            if (payloadSystemPropertyFilePath != null && payloadSystemPropertyFilePath.isNotEmpty()) {
                sdkClickHandler!!.sendPreinstallPayload(
                    payloadSystemPropertyFilePath,
                    Constants.SYSTEM_PROPERTIES_PATH
                )
            } else {
                readStatus = PreinstallUtil.markAsRead(Constants.SYSTEM_PROPERTIES_PATH, readStatus)
            }
        }

        // 4. try reading preinstall payload from system property file path using reflection

        // 4. try reading preinstall payload from system property file path using reflection
        if (PreinstallUtil.hasNotBeenRead(
                Constants.SYSTEM_PROPERTIES_PATH_REFLECTION,
                readStatus
            )
        ) {
            val payloadSystemPropertyFilePathReflection =
                PreinstallUtil.getPayloadFromSystemPropertyFilePathReflection(
                    deviceInfo!!.packageName!!, logger!!
                )
            if (payloadSystemPropertyFilePathReflection != null && payloadSystemPropertyFilePathReflection.isNotEmpty()) {
                sdkClickHandler!!.sendPreinstallPayload(
                    payloadSystemPropertyFilePathReflection,
                    Constants.SYSTEM_PROPERTIES_PATH_REFLECTION
                )
            } else {
                readStatus = PreinstallUtil.markAsRead(
                    Constants.SYSTEM_PROPERTIES_PATH_REFLECTION,
                    readStatus
                )
            }
        }

        // 5. try reading preinstall payload from default content uri
        if (PreinstallUtil.hasNotBeenRead(Constants.CONTENT_PROVIDER, readStatus)) {
            val payloadContentProviderDefault = PreinstallUtil.getPayloadFromContentProviderDefault(
                motrackConfig!!.context!!,
                deviceInfo!!.packageName!!,
                logger!!
            )
            if (payloadContentProviderDefault != null && payloadContentProviderDefault.isNotEmpty()) {
                sdkClickHandler!!.sendPreinstallPayload(
                    payloadContentProviderDefault,
                    Constants.CONTENT_PROVIDER
                )
            } else {
                readStatus = PreinstallUtil.markAsRead(Constants.CONTENT_PROVIDER, readStatus)
            }
        }

        // 6. try reading preinstall payload from all content provider with intent action and with install permission
        if (PreinstallUtil.hasNotBeenRead(Constants.CONTENT_PROVIDER_INTENT_ACTION, readStatus)) {
            val payloadListContentProviderIntentAction =
                PreinstallUtil.getPayloadsFromContentProviderIntentAction(
                    motrackConfig!!.context!!,
                    deviceInfo!!.packageName!!,
                    logger!!
                )
            if (payloadListContentProviderIntentAction != null && payloadListContentProviderIntentAction.isNotEmpty()) {
                for (payload in payloadListContentProviderIntentAction) {
                    sdkClickHandler!!.sendPreinstallPayload(
                        payload,
                        Constants.CONTENT_PROVIDER_INTENT_ACTION
                    )
                }
            } else {
                readStatus =
                    PreinstallUtil.markAsRead(Constants.CONTENT_PROVIDER_INTENT_ACTION, readStatus)
            }
        }

        // 7. try reading preinstall payload from all content provider with intent action and without install permission
        if (PreinstallUtil.hasNotBeenRead(Constants.CONTENT_PROVIDER_NO_PERMISSION, readStatus)) {
            val payloadListContentProviderIntentAction =
                PreinstallUtil.getPayloadsFromContentProviderNoPermission(
                    motrackConfig!!.context!!,
                    deviceInfo!!.packageName!!,
                    logger!!
                )
            if (payloadListContentProviderIntentAction != null && payloadListContentProviderIntentAction.isNotEmpty()) {
                for (payload in payloadListContentProviderIntentAction) {
                    sdkClickHandler!!.sendPreinstallPayload(
                        payload,
                        Constants.CONTENT_PROVIDER_NO_PERMISSION
                    )
                }
            } else {
                readStatus =
                    PreinstallUtil.markAsRead(Constants.CONTENT_PROVIDER_NO_PERMISSION, readStatus)
            }
        }

        // 8. try reading preinstall payload from file system (world readable)
        if (PreinstallUtil.hasNotBeenRead(Constants.FILE_SYSTEM, readStatus)) {
            val payloadFileSystem = PreinstallUtil.getPayloadFromFileSystem(
                deviceInfo!!.packageName!!,
                motrackConfig!!.preinstallFilePath,
                logger!!
            )
            if (payloadFileSystem != null && payloadFileSystem.isNotEmpty()) {
                sdkClickHandler!!.sendPreinstallPayload(payloadFileSystem, Constants.FILE_SYSTEM)
            } else {
                readStatus = PreinstallUtil.markAsRead(Constants.FILE_SYSTEM, readStatus)
            }
        }

        sharedPreferencesManager.setPreinstallPayloadReadStatus(readStatus)

        internalState!!.preinstallHasBeenRead = true
    }

    private fun sendPreinstallReferrerI() {
        if (!isEnabledI()) {
            return
        }
        if (internalState!!.hasFirstSdkStartNotOccurred()) {
            return
        }
        val sharedPreferencesManager = SharedPreferencesManager(getContext())
        val referrerPayload = sharedPreferencesManager.getPreinstallReferrer()
        if (referrerPayload == null || referrerPayload.isEmpty()) {
            return
        }
        sdkClickHandler!!.sendPreinstallPayload(
            referrerPayload,
            Constants.SYSTEM_INSTALLER_REFERRER
        )
    }

    private fun sendInstallReferrerI(referrerDetails: ReferrerDetails, referrerApi: String) {
        if (!isEnabledI()) {
            return
        }
        if (!isValidReferrerDetails(referrerDetails)) {
            return
        }
        if (Util.isEqualReferrerDetails(referrerDetails, referrerApi, activityState!!)) {
            // Same click already sent before, nothing to be done.
            return
        }

        // Create sdk click
        val sdkClickPackage: ActivityPackage = PackageFactory.buildInstallReferrerSdkClickPackage(
            referrerDetails,
            referrerApi,
            activityState!!,
            motrackConfig!!,
            deviceInfo!!,
            sessionParameters!!
        )
        sdkClickHandler!!.sendSdkClick(sdkClickPackage)
    }

    private fun isValidReferrerDetails(referrerDetails: ReferrerDetails?): Boolean {
        if (referrerDetails == null) {
            return false
        }
        return referrerDetails.installReferrer.isNotEmpty()
    }

    private fun trackNewSessionI(now: Long) {
        val lastInterval = now - activityState!!.lastActivity
        activityState!!.sessionCount++
        activityState!!.lastInterval = lastInterval
        transferSessionPackageI(now)
        activityState!!.resetSessionAttributes(now)
        writeActivityStateI()
    }

    private fun transferSessionPackageI(now: Long) {
        val builder = PackageBuilder(
            motrackConfig!!, deviceInfo!!, activityState,
            sessionParameters, now
        )
        val sessionPackage: ActivityPackage =
            builder.buildSessionPackage(internalState!!.isInDelayedStart)
        packageHandler!!.addPackage(sessionPackage)
        packageHandler!!.sendFirstPackage()
    }

    private fun hasChangedStateI(
        previousState: Boolean, newState: Boolean,
        trueMessage: String, falseMessage: String
    ): Boolean {
        if (previousState != newState) {
            return true
        }
        if (previousState) {
            logger!!.debug(trueMessage)
        } else {
            logger!!.debug(falseMessage)
        }
        return false
    }

    private fun updateStatusI(
        pausingState: Boolean, pausingMessage: String,
        remainsPausedMessage: String, unPausingMessage: String
    ) {
        // it is changing from an active state to a pause state
        if (pausingState) {
            logger!!.info(pausingMessage)
        } else if (pausedI(false)) {
            // including the sdk click handler
            if (pausedI(true)) {
                logger!!.info(remainsPausedMessage)
            } else {
                logger!!.info("$remainsPausedMessage, except the Sdk Click Handler")
            }
        } else {
            // it is changing from a pause state to an active state
            logger!!.info(unPausingMessage)
        }
        updateHandlersStatusAndSendI()
    }

    private fun checkActivityStateI(activityState: ActivityState?): Boolean {
        internalState?.let {
            if (it.hasFirstSdkStartNotOccurred()) {
                logger!!.error("Sdk did not yet start")
                return false
            }
        }
        return true
    }

    private fun writeActivityStateI() {
        synchronized(ActivityState::class.java) {
            if (activityState == null) {
                return
            }
            Util.writeObject(
                activityState,
                motrackConfig!!.context!!,
                ACTIVITY_STATE_FILENAME,
                ACTIVITY_STATE_NAME
            )
        }
    }

    private fun teardownActivityStateS() {
        synchronized(ActivityState::class.java) {
            if (activityState == null) {
                return
            }
            activityState = null
        }
    }

    private fun writeAttributionI() {
        synchronized(MotrackAttribution::class.java) {
            if (attribution == null) {
                return
            }
            Util.writeObject(
                attribution,
                motrackConfig!!.context!!,
                ATTRIBUTION_FILENAME,
                ATTRIBUTION_NAME
            )
        }
    }

    private fun teardownAttributionS() {
        synchronized(MotrackAttribution::class.java) {
            if (attribution == null) {
                return
            }
            attribution = null
        }
    }

    private fun writeSessionCallbackParametersI() {
        synchronized(SessionParameters::class.java) {
            if (sessionParameters == null) {
                return
            }
            Util.writeObject(
                sessionParameters!!.callbackParameters,
                motrackConfig!!.context!!,
                SESSION_CALLBACK_PARAMETERS_FILENAME,
                SESSION_CALLBACK_PARAMETERS_NAME
            )
        }
    }

    private fun writeSessionPartnerParametersI() {
        synchronized(SessionParameters::class.java) {
            if (sessionParameters == null) {
                return
            }
            Util.writeObject(
                sessionParameters!!.partnerParameters,
                motrackConfig!!.context!!,
                SESSION_PARTNER_PARAMETERS_FILENAME,
                SESSION_PARTNER_PARAMETERS_NAME
            )
        }
    }

    private fun teardownAllSessionParametersS() {
        synchronized(SessionParameters::class.java) {
            if (sessionParameters == null) {
                return
            }
            sessionParameters = null
        }
    }

    fun foregroundTimerFired() {
        executor!!.submit { foregroundTimerFiredI() }
    }

    fun backgroundTimerFired() {
        executor!!.submit { backgroundTimerFiredI() }
    }

    private fun startForegroundTimerI() {
        // don't start the timer if it's disabled
        if (!isEnabledI()) {
            return
        }
        foregroundTimer!!.start()
    }

    private fun stopForegroundTimerI() {
        foregroundTimer!!.suspend()
    }

    private fun foregroundTimerFiredI() {
        // stop the timer cycle if it's disabled
        if (!isEnabledI()) {
            stopForegroundTimerI()
            return
        }
        if (toSendI()) {
            packageHandler!!.sendFirstPackage()
        }
        if (updateActivityStateI(System.currentTimeMillis())) {
            writeActivityStateI()
        }
    }

    private fun startBackgroundTimerI() {
        if (backgroundTimer == null) {
            return
        }

        // check if it can send in the background
        if (!toSendI()) {
            return
        }

        // background timer already started
        if (backgroundTimer!!.getFireIn() > 0) {
            return
        }
        backgroundTimer!!.startIn(BACKGROUND_TIMER_INTERVAL)
    }

    private fun stopBackgroundTimerI() {
        if (backgroundTimer == null) {
            return
        }
        backgroundTimer!!.cancel()
    }

    private fun backgroundTimerFiredI() {
        if (toSendI()) {
            packageHandler!!.sendFirstPackage()
        }
    }

    private fun pausedI(): Boolean {
        return pausedI(false)
    }

    private fun pausedI(sdkClickHandlerOnly: Boolean): Boolean {
        return if (sdkClickHandlerOnly) {
            // sdk click handler is paused if either:
            internalState!!.isOffline ||         // it's offline
                    !isEnabledI()                // is disabled
        } else {
            // other handlers are paused if either:
            internalState!!.isOffline ||             // it's offline
                    !isEnabledI() ||                 // is disabled
                    internalState!!.isInDelayedStart // is in delayed start
        }
    }

    private fun toSendI(): Boolean {
        return toSendI(false)
    }

    private fun toSendI(sdkClickHandlerOnly: Boolean): Boolean {
        // don't send when it's paused
        if (pausedI(sdkClickHandlerOnly)) {
            return false
        }

        // has the option to send in the background -> is to send
        return if (motrackConfig!!.sendInBackground) {
            true
        } else internalState!!.isInForeground()

        // doesn't have the option -> depends on being on the background/foreground
    }


    override fun gdprForgetMe() {
        executor!!.submit { gdprForgetMeI() }
    }

    private fun gdprForgetMeI() {
        if (!checkActivityStateI(activityState)) {
            return
        }
        if (!isEnabledI()) {
            return
        }
        if (activityState!!.isGdprForgotten) {
            return
        }
        activityState!!.isGdprForgotten = true
        writeActivityStateI()
        val now = System.currentTimeMillis()
        val gdprPackageBuilder = PackageBuilder(
            motrackConfig!!,
            deviceInfo!!, activityState, sessionParameters, now
        )
        val gdprPackage = gdprPackageBuilder.buildGdprPackage()
        packageHandler!!.addPackage(gdprPackage)

        // If GDPR choice was cached, remove it.
        val sharedPreferencesManager = SharedPreferencesManager(getContext())
        sharedPreferencesManager.removeGdprForgetMe()
        if (motrackConfig!!.eventBufferingEnabled) {
            logger!!.info("Buffered event ${gdprPackage.suffix!!}")
        } else {
            packageHandler!!.sendFirstPackage()
        }
    }

    override fun disableThirdPartySharing() {
        executor!!.submit { disableThirdPartySharingI() }
    }

    private fun disableThirdPartySharingI() {
        // cache the disable third party sharing request, so that the request order maintains
        // even this call returns before making server request
        val sharedPreferencesManager = SharedPreferencesManager(getContext())
        sharedPreferencesManager.setDisableThirdPartySharing()
        if (!checkActivityStateI(activityState)) {
            return
        }
        if (!isEnabledI()) {
            return
        }
        if (activityState!!.isGdprForgotten) {
            return
        }
        if (activityState!!.isThirdPartySharingDisabled) {
            return
        }
        activityState!!.isThirdPartySharingDisabled = true
        writeActivityStateI()
        val now = System.currentTimeMillis()
        val packageBuilder =
            PackageBuilder(motrackConfig!!, deviceInfo!!, activityState, sessionParameters, now)
        val activityPackage: ActivityPackage = packageBuilder.buildDisableThirdPartySharingPackage()
        packageHandler!!.addPackage(activityPackage)

        // Removed the cached disable third party sharing flag.
        sharedPreferencesManager.removeDisableThirdPartySharing()
        if (motrackConfig!!.eventBufferingEnabled) {
            logger!!.info("Buffered event ${activityPackage.suffix!!}")
        } else {
            packageHandler!!.sendFirstPackage()
        }
    }

    override fun trackThirdPartySharing(motrackThirdPartySharing: MotrackThirdPartySharing) {
        executor!!.submit { trackThirdPartySharingI(motrackThirdPartySharing) }
    }

    private fun trackThirdPartySharingI(motrackThirdPartySharing: MotrackThirdPartySharing) {
        if (!checkActivityStateI(activityState)) {
            motrackConfig!!.preLaunchActions!!.preLaunchMotrackThirdPartyArray.add(
                motrackThirdPartySharing
            )
            return
        }
        if (!isEnabledI()) {
            return
        }
        if (activityState!!.isGdprForgotten) {
            return
        }
        val now = System.currentTimeMillis()
        val packageBuilder =
            PackageBuilder(motrackConfig!!, deviceInfo!!, activityState, sessionParameters, now)
        val activityPackage: ActivityPackage =
            packageBuilder.buildThirdPartySharingPackage(motrackThirdPartySharing)
        packageHandler!!.addPackage(activityPackage)
        if (motrackConfig!!.eventBufferingEnabled) {
            logger!!.info("Buffered event ${activityPackage.suffix!!}")
        } else {
            packageHandler!!.sendFirstPackage()
        }
    }

    override fun trackMeasurementConsent(consentMeasurement: Boolean) {
        executor!!.submit { trackMeasurementConsentI(consentMeasurement) }
    }

    private fun trackMeasurementConsentI(consentMeasurement: Boolean) {
        if (!checkActivityStateI(activityState)) {
            motrackConfig!!.preLaunchActions!!.lastMeasurementConsentTracked = consentMeasurement
            return
        }

        if (!isEnabledI()) {
            return
        }
        if (activityState!!.isGdprForgotten) {
            return
        }
        val now = System.currentTimeMillis()
        val packageBuilder =
            PackageBuilder(motrackConfig!!, deviceInfo!!, activityState, sessionParameters, now)
        val activityPackage: ActivityPackage =
            packageBuilder.buildMeasurementConsentPackage(consentMeasurement)
        packageHandler!!.addPackage(activityPackage)
        if (motrackConfig!!.eventBufferingEnabled) {
            logger!!.info("Buffered event ${activityPackage.suffix!!}")
        } else {
            packageHandler!!.sendFirstPackage()
        }
    }

    override fun trackAdRevenue(source: String, adRevenueJson: JSONObject) {
        executor!!.submit { trackAdRevenueI(source, adRevenueJson) }
    }

    private fun trackAdRevenueI(source: String, adRevenueJson: JSONObject) {
        if (!checkActivityStateI(activityState)) {
            return
        }
        if (!isEnabledI()) {
            return
        }
        if (activityState!!.isGdprForgotten) {
            return
        }
        val now = System.currentTimeMillis()
        val packageBuilder = PackageBuilder(
            motrackConfig!!,
            deviceInfo!!, activityState, sessionParameters, now
        )
        val adRevenuePackage = packageBuilder.buildAdRevenuePackage(source, adRevenueJson)
        packageHandler!!.addPackage(adRevenuePackage)
        packageHandler!!.sendFirstPackage()
    }


    override fun trackAdRevenue(motrackAdRevenue: MotrackAdRevenue) {
        executor!!.submit { trackAdRevenueI(motrackAdRevenue) }
    }

    private fun trackAdRevenueI(motrackAdRevenue: MotrackAdRevenue) {
        if (!checkActivityStateI(activityState)) {
            return
        }
        if (!isEnabledI()) {
            return
        }
        if (!checkMotrackAdRevenue(motrackAdRevenue)) {
            return
        }
        if (activityState!!.isGdprForgotten) {
            return
        }
        val now = System.currentTimeMillis()
        val packageBuilder = PackageBuilder(
            motrackConfig!!,
            deviceInfo!!, activityState, sessionParameters, now
        )
        val adRevenuePackage: ActivityPackage = packageBuilder.buildAdRevenuePackage(
            motrackAdRevenue,
            internalState!!.isInDelayedStart
        )
        packageHandler!!.addPackage(adRevenuePackage)
        packageHandler!!.sendFirstPackage()
    }

    private fun checkMotrackAdRevenue(motrackAdRevenue: MotrackAdRevenue?): Boolean {
        if (motrackAdRevenue == null) {
            logger!!.error("Ad revenue object missing")
            return false
        }
        if (!motrackAdRevenue.isValid()) {
            logger!!.error("Ad revenue object not initialized correctly")
            return false
        }
        return true
    }

    override fun trackPlayStoreSubscription(motrackPlayStoreSubscription: MotrackPlayStoreSubscription) {
        executor!!.submit { trackSubscriptionI(motrackPlayStoreSubscription) }
    }

    private fun trackSubscriptionI(subscription: MotrackPlayStoreSubscription) {
        if (!checkActivityStateI(activityState)) {
            return
        }
        if (!isEnabledI()) {
            return
        }
        if (activityState!!.isGdprForgotten) {
            return
        }
        val now = System.currentTimeMillis()
        val packageBuilder = PackageBuilder(
            motrackConfig!!,
            deviceInfo!!, activityState, sessionParameters, now
        )
        val subscriptionPackage = packageBuilder.buildSubscriptionPackage(
            subscription,
            internalState!!.isInDelayedStart
        )
        packageHandler!!.addPackage(subscriptionPackage)
        packageHandler!!.sendFirstPackage()
    }


    override fun gotOptOutResponse() {
        executor!!.submit { gotOptOutResponseI() }
    }

    private fun gotOptOutResponseI() {
        activityState!!.isGdprForgotten = true
        writeActivityStateI()
        packageHandler!!.flush()
        setEnabledI(false)
    }

    override fun getContext(): Context {
        return motrackConfig!!.context!!
    }


    override fun getAdid(): String? {
        return activityState?.adid
    }

    override fun getAttribution(): MotrackAttribution? {
        return attribution
    }

    class InternalState {
        var isEnabled = false
        var isOffline = false
        var isInBackground = false
        var isInDelayedStart = false
        var updatePackages = false
        var isFirstLaunch = false
        var sessionResponseProcessed = false
        var firstSdkStart = false
        var preinstallHasBeenRead = false

        fun isDisabled(): Boolean {
            return !isEnabled
        }

        fun isOnline(): Boolean {
            return !isOffline
        }

        fun isInForeground(): Boolean {
            return !isInBackground
        }

        fun isNotInDelayedStart(): Boolean {
            return !isInDelayedStart
        }

        fun itHasToUpdatePackages(): Boolean {
            return updatePackages
        }

        fun isNotFirstLaunch(): Boolean {
            return !isFirstLaunch
        }

        fun hasSessionResponseNotBeenProcessed(): Boolean {
            return !sessionResponseProcessed
        }

        fun hasFirstSdkStartOccurred(): Boolean {
            return firstSdkStart
        }

        fun hasFirstSdkStartNotOccurred(): Boolean {
            return !firstSdkStart
        }

        fun hasPreinstallBeenRead(): Boolean {
            return preinstallHasBeenRead
        }
    }

    companion object {
        private var FOREGROUND_TIMER_INTERVAL: Long = 0
        private var FOREGROUND_TIMER_START: Long = 0
        private var BACKGROUND_TIMER_INTERVAL: Long = 0
        private var SESSION_INTERVAL: Long = 0
        private var SUBSESSION_INTERVAL: Long = 0
        private const val TIME_TRAVEL = "Time travel!"
        private const val ACTIVITY_STATE_NAME = "Activity state"
        private const val ATTRIBUTION_NAME = "Attribution"
        private const val FOREGROUND_TIMER_NAME = "Foreground timer"
        private const val BACKGROUND_TIMER_NAME = "Background timer"
        private const val DELAY_START_TIMER_NAME = "Delay Start timer"
        private const val SESSION_CALLBACK_PARAMETERS_NAME = "Session Callback parameters"
        private const val SESSION_PARTNER_PARAMETERS_NAME = "Session Partner parameters"

        public fun getInstance(motrackConfig: MotrackConfig?): ActivityHandler? {
            if (motrackConfig == null) {
                MotrackFactory.getLogger().error("MotrackConfig missing")
                return null
            }
            if (!motrackConfig.isValid()) {
                MotrackFactory.getLogger().error("MotrackConfig not initialized correctly")
                return null
            }
            if (motrackConfig.processName != null) {
                val currentPid = Process.myPid()
                val manager =
                    motrackConfig.context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                for (processInfo in manager.runningAppProcesses) {
                    if (processInfo.pid == currentPid) {
                        if (!processInfo.processName.equals(
                                motrackConfig.processName,
                                ignoreCase = true
                            )
                        ) {
                            MotrackFactory.getLogger().info(
                                "Skipping initialization in background process (${processInfo.processName})"
                            )
                            return null
                        }
                        break
                    }
                }
            }
            return ActivityHandler(motrackConfig)
        }

        fun deleteState(context: Context) {
            deleteActivityState(context)
            deleteAttribution(context)
            deleteSessionCallbackParameters(context)
            deleteSessionPartnerParameters(context)
            val sharedPreferencesManager = SharedPreferencesManager(context)
            sharedPreferencesManager.clear()
        }

        fun deleteActivityState(context: Context): Boolean {
            return context.deleteFile(ACTIVITY_STATE_FILENAME)
        }

        fun deleteAttribution(context: Context): Boolean {
            return context.deleteFile(ATTRIBUTION_FILENAME)
        }

        fun deleteSessionCallbackParameters(context: Context): Boolean {
            return context.deleteFile(SESSION_CALLBACK_PARAMETERS_FILENAME)
        }

        fun deleteSessionPartnerParameters(context: Context): Boolean {
            return context.deleteFile(SESSION_PARTNER_PARAMETERS_FILENAME)
        }

    }
}