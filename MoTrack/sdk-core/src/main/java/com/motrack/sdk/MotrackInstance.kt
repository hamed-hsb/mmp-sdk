package com.motrack.sdk

import android.content.Context
import android.net.Uri
import org.json.JSONObject

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

class MotrackInstance {

    /**
     * Push notifications token.
     */
    private var pushToken: String? = null

    /**
     * Is SDK enabled or not.
     */
    private var startEnabled: Boolean? = null

    /**
     * Is SDK offline or not.
     */
    private var startOffline: Boolean = false

    private val preLaunchActions = PreLaunchActions()

    /**
     * Base path for Motrack packages.
     */
    private var basePath: String? = null

    /**
     * Path for GDPR package.
     */
    private var gdprPath: String? = null

    private var subscriptionPath: String? = null

    private lateinit var logger: ILogger

    /**
     * ActivityHandler instance.
     */
    private var activityHandler: IActivityHandler? = null

    companion object {
        public class PreLaunchActions {
            var preLaunchActionsArray: ArrayList<IRunActivityHandler> = ArrayList()
            var preLaunchMotrackThirdPartyArray: ArrayList<MotrackThirdPartySharing> = ArrayList()
            var lastMeasurementConsentTracked: Boolean? = null
        }

    }

    /**
     * Called upon SDK initialisation.
     *
     * @param motrackConfig MotrackConfig object used for SDK initialisation
     */
    public fun onCreate(motrackConfig: MotrackConfig?) {
        logger = MotrackFactory.getLogger()
        if (motrackConfig == null) {
            logger.error("MotrackConfig Missing")
            return
        }
        if (!motrackConfig.isValid()) {
            logger.error("Motrack config not initialized correctly")
            return
        }

        motrackConfig.preLaunchActions = preLaunchActions
        motrackConfig.pushToken = pushToken
        motrackConfig.startEnabled = startEnabled
        motrackConfig.startOffline = startOffline
        motrackConfig.basePath = this.basePath
        motrackConfig.dgprPath = this.gdprPath
        motrackConfig.subscriptionPath = this.subscriptionPath

        activityHandler = MotrackFactory.getActivityHandler(motrackConfig)
        setSendingReferrersAsNotSent(motrackConfig.context!!)
    }

    /**
     * Called to track event.
     *
     * @param event MotrackEvent object to be tracked
     */
    fun trackEvent(event: MotrackEvent) {
        if (!checkActivityHandler("trackEvent")) {
            return
        }
        activityHandler!!.trackEvent(event)
    }

    /**
     * Called upon each Activity's onResume() method call.
     */
    fun onResume() {
        if (!checkActivityHandler("onResume")) {
            return
        }
        activityHandler!!.onResume()
    }

    /**
     * Called upon each Activity's onPause() method call.
     */
    fun onPause() {
        if (!checkActivityHandler("onPause")) {
            return
        }
        activityHandler!!.onPause()
    }

    /**
     * Called to disable/enable SDK.
     *
     * @param enabled boolean indicating whether SDK should be enabled or disabled
     */
    fun setEnabled(enabled: Boolean) {
        startEnabled = enabled
        if (checkActivityHandler(enabled, "enabled mode", "disabled mode")) {
            activityHandler!!.setEnabled(enabled)
        }
    }


    /**
     * Get information if SDK is enabled or not.
     *
     * @return boolean indicating whether SDK is enabled or not
     */
    fun isEnabled(): Boolean {
        return if (!checkActivityHandler("isEnabled")) {
            isInstanceEnabled()
        } else activityHandler!!.isEnabled()
    }

    /**
     * Called to process deep link.
     *
     * @param url Deep link URL to process
     */
    fun appWillOpenUrl(url: Uri?) {
        if (!checkActivityHandler("appWillOpenUrl")) {
            return
        }
        val clickTime = System.currentTimeMillis()
        activityHandler!!.readOpenUrl(url!!, clickTime)
    }

    /**
     * Called to process deep link.
     *
     * @param url     Deep link URL to process
     * @param context Application context
     */
    fun appWillOpenUrl(url: Uri?, context: Context?) {
        // Check for deep link validity. If invalid, return.
        if (url == null || url.toString().isEmpty()) {
            logger.warn(
                "Skipping deep link processing (null or empty)"
            )
            return
        }
        val clickTime = System.currentTimeMillis()
        if (!checkActivityHandler("appWillOpenUrl", true)) {
            saveDeeplink(url, clickTime, context!!)
            return
        }
        activityHandler!!.readOpenUrl(url, clickTime)
    }

    /**
     * Called to process referrer information sent with INSTALL_REFERRER intent.
     *
     * @param rawReferrer Raw referrer content
     * @param context     Application context
     */
    fun sendReferrer(rawReferrer: String?, context: Context?) {
        val clickTime = System.currentTimeMillis()

        // Check for referrer validity. If invalid, return.
        if (rawReferrer == null || rawReferrer.isEmpty()) {
            logger.warn(
                "Skipping INSTALL_REFERRER intent referrer processing (null or empty)"
            )
            return
        }
        saveRawReferrer(rawReferrer, clickTime, context!!)
        if (checkActivityHandler("referrer", true)) {
            if (activityHandler!!.isEnabled()) {
                activityHandler!!.sendRefTagReferrer()
            }
        }
    }

    /**
     * Save referrer to shared preferences.
     *
     * @param clickTime   Referrer click time
     * @param rawReferrer Raw referrer content
     * @param context     Application context
     */
    private fun saveRawReferrer(rawReferrer: String, clickTime: Long, context: Context) {
        val command = Runnable {
            val sharedPreferencesManager = SharedPreferencesManager(context)
            sharedPreferencesManager.saveRawReferrer(rawReferrer, clickTime)
        }
        Util.runInBackground(command)
    }


    /**
     * Called to process preinstall payload information sent with SYSTEM_INSTALLER_REFERRER intent.
     *
     * @param referrer    Preinstall referrer content
     * @param context     Application context
     */
    fun sendPreinstallReferrer(referrer: String?, context: Context?) {
        // Check for referrer validity. If invalid, return.
        if (referrer == null || referrer.isEmpty()) {
            logger.warn(
                "Skipping SYSTEM_INSTALLER_REFERRER preinstall referrer processing (null or empty)"
            )
            return
        }
        savePreinstallReferrer(referrer, context!!)
        if (checkActivityHandler("preinstall referrer", true)) {
            if (activityHandler!!.isEnabled()) {
                activityHandler!!.sendPreinstallReferrer()
            }
        }
    }

    /**
     * Save preinstall referrer to shared preferences.
     *
     * @param referrer    Preinstall referrer content
     * @param context     Application context
     */
    private fun savePreinstallReferrer(referrer: String, context: Context) {
        val command = Runnable {
            val sharedPreferencesManager = SharedPreferencesManager(context)
            sharedPreferencesManager.savePreinstallReferrer(referrer)
        }
        Util.runInBackground(command)
    }

    /**
     * Called to set SDK to offline or online mode.
     *
     * @param enabled boolean indicating should SDK be in offline mode (true) or not (false)
     */
    fun setOfflineMode(enabled: Boolean) {
        if (!checkActivityHandler(enabled, "offline mode", "online mode")) {
            startOffline = enabled
        } else {
            activityHandler!!.setOfflineMode(enabled)
        }
    }

    /**
     * Called if SDK initialisation was delayed and you would like to stop waiting for timer.
     */
    fun sendFirstPackages() {
        if (!checkActivityHandler("sendFirstPackages")) {
            return
        }
        activityHandler!!.sendFirstPackages()
    }

    /**
     * Called to add global callback parameter that will be sent with each session and event.
     *
     * @param key   Global callback parameter key
     * @param value Global callback parameter value
     */
    fun addSessionCallbackParameter(key: String?, value: String?) {
        if (checkActivityHandler("adding session callback parameter", true)) {
            activityHandler!!.addSessionCallbackParameter(key, value)
            return
        }
        preLaunchActions.preLaunchActionsArray.add(object : IRunActivityHandler {
            override fun run(activityHandler: ActivityHandler?) {
                activityHandler!!.addSessionCallbackParameterI(key, value)

            }
        })
    }

    /**
     * Called to add global partner parameter that will be sent with each session and event.
     *
     * @param key   Global partner parameter key
     * @param value Global partner parameter value
     */
    fun addSessionPartnerParameter(key: String?, value: String?) {
        if (checkActivityHandler("adding session partner parameter", true)) {
            activityHandler!!.addSessionPartnerParameter(key, value)
            return
        }
        preLaunchActions.preLaunchActionsArray.add(object : IRunActivityHandler {
            override fun run(activityHandler: ActivityHandler?) {
                activityHandler!!.addSessionPartnerParameterI(key, value)
            }
        })
    }

    /**
     * Called to remove global callback parameter from session and event packages.
     *
     * @param key Global callback parameter key
     */
    fun removeSessionCallbackParameter(key: String?) {
        if (checkActivityHandler("removing session callback parameter", true)) {
            activityHandler!!.removeSessionCallbackParameter(key)
            return
        }
        preLaunchActions.preLaunchActionsArray.add(object : IRunActivityHandler {
            override fun run(activityHandler: ActivityHandler?) {
                activityHandler!!.removeSessionCallbackParameterI(key)
            }
        })
    }

    /**
     * Called to remove global partner parameter from session and event packages.
     *
     * @param key Global partner parameter key
     */
    fun removeSessionPartnerParameter(key: String?) {
        if (checkActivityHandler("removing session partner parameter", true)) {
            activityHandler!!.removeSessionPartnerParameter(key)
            return
        }
        preLaunchActions.preLaunchActionsArray.add(object : IRunActivityHandler {
            override fun run(activityHandler: ActivityHandler?) {
                activityHandler!!.removeSessionPartnerParameterI(key)
            }
        })
    }

    /**
     * Called to remove all added global callback parameters.
     */
    fun resetSessionCallbackParameters() {
        if (checkActivityHandler("resetting session callback parameters", true)) {
            activityHandler!!.resetSessionCallbackParameters()
            return
        }
        preLaunchActions.preLaunchActionsArray.add(object : IRunActivityHandler {
            override fun run(activityHandler: ActivityHandler?) {
                activityHandler!!.resetSessionCallbackParametersI()
            }
        })
    }

    /**
     * Called to remove all added global partner parameters.
     */
    fun resetSessionPartnerParameters() {
        if (checkActivityHandler("resetting session partner parameters", true)) {
            activityHandler!!.resetSessionPartnerParameters()
            return
        }
        preLaunchActions.preLaunchActionsArray.add(object : IRunActivityHandler {
            override fun run(activityHandler: ActivityHandler?) {
                activityHandler!!.resetSessionPartnerParametersI()
            }
        })
    }

    /**
     * Called to teardown SDK state.
     * Used only for Motrack tests, shouldn't be used in client apps.
     */
    fun teardown() {
        if (!checkActivityHandler("teardown")) {
            return
        }
        activityHandler!!.teardown()
        activityHandler = null
    }

    /**
     * Called to set user's push notifications token.
     *
     * @param token Push notifications token
     */
    fun setPushToken(token: String?) {
        if (!checkActivityHandler("push token", true)) {
            pushToken = token
        } else {
            activityHandler!!.setPushToken(token!!, false)
        }
    }

    /**
     * Called to set user's push notifications token.
     *
     * @param token   Push notifications token
     * @param context Application context
     */
    fun setPushToken(token: String, context: Context) {
        savePushToken(token, context)
        if (checkActivityHandler("push token", true)) {
            if (activityHandler!!.isEnabled()) {
                activityHandler!!.setPushToken(token, true)
            }
        }
    }

    /**
     * Save push token to shared preferences.
     *
     * @param pushToken Push notifications token
     * @param context   Application context
     */
    private fun savePushToken(pushToken: String, context: Context) {
        val command = Runnable {
            val sharedPreferencesManager = SharedPreferencesManager(context)
            sharedPreferencesManager.savePushToken(pushToken)
        }
        Util.runInBackground(command)
    }

    /**
     * Called to forget the user in accordance with GDPR law.
     *
     * @param context Application context
     */
    fun gdprForgetMe(context: Context?) {
        saveGdprForgetMe(context!!)
        if (checkActivityHandler("gdpr", true)) {
            if (activityHandler!!.isEnabled()) {
                activityHandler!!.gdprForgetMe()
            }
        }
    }

    /**
     * Save GDPR forget me choice to shared preferences.
     *
     * @param context Application context
     */
    private fun saveGdprForgetMe(context: Context) {
        val command = Runnable {
            val sharedPreferencesManager = SharedPreferencesManager(context)
            sharedPreferencesManager.setGdprForgetMe()
        }
        Util.runInBackground(command)
    }

    /**
     * Called to disable the third party sharing.
     *
     * @param context Application context
     */
    fun disableThirdPartySharing(context: Context) {
        if (!checkActivityHandler("disable third party sharing", true)) {
            saveDisableThirdPartySharing(context)
            return
        }
        activityHandler!!.disableThirdPartySharing()
    }

    /**
     * Save disable third party sharing choice to shared preferences.
     *
     * @param context Application context
     */
    private fun saveDisableThirdPartySharing(context: Context) {
        val command = Runnable {
            val sharedPreferencesManager = SharedPreferencesManager(context)
            sharedPreferencesManager.setDisableThirdPartySharing()
        }
        Util.runInBackground(command)
    }

    fun trackThirdPartySharing(motrackThirdPartySharing: MotrackThirdPartySharing) {
        if (!checkActivityHandler("third party sharing", true)) {
            preLaunchActions.preLaunchMotrackThirdPartyArray.add(motrackThirdPartySharing)
            return
        }
        activityHandler!!.trackThirdPartySharing(motrackThirdPartySharing)
    }

    fun trackMeasurementConsent(consentMeasurement: Boolean) {
        if (!checkActivityHandler("measurement consent", true)) {
            preLaunchActions.lastMeasurementConsentTracked = consentMeasurement
            return
        }
        activityHandler!!.trackMeasurementConsent(consentMeasurement)
    }

    /**
     * Track ad revenue from a source provider
     *
     * @param source Source of ad revenue information, see MotrackConfig.AD_REVENUE_* for some possible sources
     * @param adRevenueJson JsonObject content of the ad revenue information
     */
    fun trackAdRevenue(source: String?, adRevenueJson: JSONObject?) {
        if (!checkActivityHandler("trackAdRevenue")) {
            return
        }
        activityHandler!!.trackAdRevenue(source!!, adRevenueJson!!)
    }

    /**
     * Track ad revenue from a source provider
     *
     * @param motrackAdRevenue Motrack ad revenue information like source, revenue, currency etc
     */
    fun trackAdRevenue(motrackAdRevenue: MotrackAdRevenue) {
        if (!checkActivityHandler("trackAdRevenue")) {
            return
        }
        activityHandler!!.trackAdRevenue(motrackAdRevenue)
    }

    /**
     * Track subscription from Google Play.
     *
     * @param subscription MotrackPlayStoreSubscription object to be tracked
     */
    fun trackPlayStoreSubscription(subscription: MotrackPlayStoreSubscription) {
        if (!checkActivityHandler("trackPlayStoreSubscription")) {
            return
        }
        activityHandler!!.trackPlayStoreSubscription(subscription)
    }

    /**
     * Called to get value of unique Motrack device identifier.
     *
     * @return Unique Motrack device identifier
     */
    fun getAdid(): String? {
        return if (!checkActivityHandler("getAdid")) {
            null
        } else activityHandler!!.getAdid()
    }

    /**
     * Called to get user's current attribution value.
     *
     * @return MotrackAttribution object with current attribution value
     */
    fun getAttribution(): MotrackAttribution? {
        return if (!checkActivityHandler("getAttribution")) {
            null
        } else activityHandler!!.getAttribution()
    }

    /**
     * Called to get native SDK version string.
     *
     * @return Native SDK version string.
     */
    fun getSdkVersion(): String {
        return Util.getSdkVersion()
    }

    /**
     * Flag stored referrers as still not sent.
     *
     * @param context Application context
     */
    private fun setSendingReferrersAsNotSent(context: Context) {
        val command = Runnable {
            val sharedPreferencesManager = SharedPreferencesManager(context)
            sharedPreferencesManager.setSendingReferrersAsNotSent()
        }
        Util.runInBackground(command)
    }

    /**
     * Check if MotrackInstance enable flag is set or not.
     *
     * @return boolean indicating whether MotrackInstance is enabled or not
     */
    private fun isInstanceEnabled(): Boolean {
        return startEnabled == null || startEnabled!!
    }

    /**
     * Check if ActivityHandler instance is set or not.
     *
     * @return boolean indicating whether ActivityHandler instance is set or not
     */
    private fun checkActivityHandler(action: String): Boolean {
        return checkActivityHandler(action, false)
    }

    /**
     * Check if ActivityHandler instance is set or not.
     *
     * @param status       Is SDK enabled or not
     * @param trueMessage  Log message to display in case SDK is enabled
     * @param falseMessage Log message to display in case SDK is disabled
     * @return boolean indicating whether ActivityHandler instance is set or not
     */
    private fun checkActivityHandler(
        status: Boolean,
        trueMessage: String,
        falseMessage: String
    ): Boolean {
        return if (status) {
            checkActivityHandler(trueMessage, true)
        } else {
            checkActivityHandler(falseMessage, true)
        }
    }

    /**
     * Save deep link to shared preferences.
     *
     * @param deeplink  Deeplink Uri object
     * @param clickTime Time when appWillOpenUrl(Uri, Context) method was called
     * @param context   Application context
     */
    private fun saveDeeplink(deeplink: Uri, clickTime: Long, context: Context) {
        val command = Runnable {
            val sharedPreferencesManager = SharedPreferencesManager(context)
            sharedPreferencesManager.saveDeeplink(deeplink, clickTime)
        }
        Util.runInBackground(command)
    }

    /**
     * Check if ActivityHandler instance is set or not.
     *
     * @param action Log message to indicate action that was asked to perform when SDK was disabled
     * @return boolean indicating whether ActivityHandler instance is set or not
     */
    private fun checkActivityHandler(action: String?, actionSaved: Boolean): Boolean {
        if (activityHandler != null) {
            return true
        }
        if (action == null) {
            logger.error("Motrack not initialized correctly")
            return false
        }
        if (actionSaved) {
            logger.warn("Motrack not initialized, but $action saved for launch")
        } else {
            logger.warn("Motrack not initialized, can't perform $action")
        }
        return false
    }

    /**
     * Used for testing purposes only. Do NOT use this method.
     *
     * @param testOptions Adjust integration tests options
     */
    fun setTestOptions(testOptions: MotrackTestOptions) {
        testOptions.basePath?.let {
            basePath = it
        }
        testOptions.gdprPath?.let {
            gdprPath = it
        }
        testOptions.subscriptionPath?.let {
            subscriptionPath = it
        }

        testOptions.baseUrl?.let {
            MotrackFactory.baseUrl = it
        }

        testOptions.gdprUrl?.let {
            MotrackFactory.gdprUrl = it
        }

        testOptions.subscriptionUrl?.let {
            MotrackFactory.subscriptionUrl = it
        }

        testOptions.timerIntervalInMilliseconds?.let {
            MotrackFactory.timerInterval = it
        }

        testOptions.timerStartInMilliseconds?.let {
            MotrackFactory.timerStart = it
        }
        testOptions.sessionIntervalInMilliseconds?.let {
            MotrackFactory.sessionInterval = it
        }
        testOptions.subsessionIntervalInMilliseconds?.let {
            MotrackFactory.subsessionInterval = it
        }

        testOptions.tryInstallReferrer?.let {
            MotrackFactory.tryInstallReferrer = it
        }
        testOptions.noBackoffWait?.let {
            MotrackFactory.packageHandlerBackoffStrategy = BackoffStrategy.NO_WAIT
            MotrackFactory.sdkClickBackoffStrategy = BackoffStrategy.NO_WAIT
        }

        testOptions.enableSigning?.let {
            if (it) {
                MotrackFactory.enableSigning()
            }
        }
        testOptions.disableSigning?.let {
            if (it) {
                MotrackFactory.disableSigning()
            }
        }
    }
}