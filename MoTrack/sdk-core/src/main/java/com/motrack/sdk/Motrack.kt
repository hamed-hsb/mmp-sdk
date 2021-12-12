import android.content.Context
import android.net.Uri
import com.motrack.sdk.*
import org.json.JSONObject

/**
 * The main interface to Motrack.
 * Use the methods of this class to tell Motrack about the usage of your app.
 * See the README for details.
 *
 * @author yaya (@yahyalmh)
 * @since 17th October 2021
 */

public class Motrack private constructor() {
    companion object {
        /**
         * Singleton Motrack SDK instance.
         */
        private var defaultInstance: MotrackInstance? = null

        /**
         * Method used to obtain Motrack SDK singleton instance.
         *
         * @return Motrack SDK singleton instance.
         */
        @Synchronized
        fun getDefaultInstance(): MotrackInstance {
            val VERSION = "!SDK-VERSION-STRING!:com.motrack.sdk:motrack-android:"

            if (defaultInstance == null) {
                defaultInstance = MotrackInstance()
            }
            return defaultInstance as MotrackInstance
        }

        /**
         * Called upon SDK initialisation.
         *
         * @param motrackConfig MotrackConfig object used for SDK initialisation
         */
        fun onCreate(motrackConfig: MotrackConfig) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.onCreate(motrackConfig)
        }

        /**
         * Called to track event.
         *
         * @param event MotrackEvent object to be tracked
         */
        fun trackEvent(event: MotrackEvent) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.trackEvent(event)
        }

        /**
         * Called upon each Activity's onResume() method call.
         */
        fun onResume() {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.onResume()
        }

        /**
         * Called upon each Activity's onPause() method call.
         */
        fun onPause() {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.onPause()
        }

        /**
         * Called to disable/enable SDK.
         *
         * @param enabled boolean indicating whether SDK should be enabled or disabled
         */
        fun setEnabled(enabled: Boolean) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.setEnabled(enabled)
        }

        /**
         * Get information if SDK is enabled or not.
         *
         * @return boolean indicating whether SDK is enabled or not
         */
        fun isEnabled(): Boolean {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            return motrackInstance.isEnabled()
        }

        /**
         * Called to process deep link.
         *
         * @param url Deep link URL to process
         *
         */
        @Deprecated("Use {@link #appWillOpenUrl(Uri, Context)}} instead.")
        fun appWillOpenUrl(url: Uri?) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.appWillOpenUrl(url)
        }

        /**
         * Called to process deep link.
         *
         * @param url Deep link URL to process
         * @param context Application context
         */
        fun appWillOpenUrl(url: Uri?, context: Context?) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.appWillOpenUrl(url, extractApplicationContext(context))
        }

        /**
         * Called to process referrer information sent with INSTALL_REFERRER intent.
         *
         * @param referrer Referrer content
         * @param context  Application context
         */
        fun setReferrer(referrer: String?, context: Context?) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.sendReferrer(referrer, extractApplicationContext(context))
        }

        /**
         * Called to set SDK to offline or online mode.
         *
         * @param enabled boolean indicating should SDK be in offline mode (true) or not (false)
         */
        fun setOfflineMode(enabled: Boolean) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.setOfflineMode(enabled)
        }

        /**
         * Called if SDK initialisation was delayed and you would like to stop waiting for timer.
         */
        fun sendFirstPackages() {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.sendFirstPackages()
        }

        /**
         * Called to add global callback parameter that will be sent with each session and event.
         *
         * @param key   Global callback parameter key
         * @param value Global callback parameter value
         */
        fun addSessionCallbackParameter(key: String?, value: String?) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.addSessionCallbackParameter(key, value)
        }

        /**
         * Called to add global partner parameter that will be sent with each session and event.
         *
         * @param key   Global partner parameter key
         * @param value Global partner parameter value
         */
        fun addSessionPartnerParameter(key: String?, value: String?) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.addSessionPartnerParameter(key, value)
        }

        /**
         * Called to remove global callback parameter from session and event packages.
         *
         * @param key Global callback parameter key
         */
        fun removeSessionCallbackParameter(key: String?) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.removeSessionCallbackParameter(key)
        }

        /**
         * Called to remove global partner parameter from session and event packages.
         *
         * @param key Global partner parameter key
         */
        fun removeSessionPartnerParameter(key: String?) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.removeSessionPartnerParameter(key)
        }

        /**
         * Called to remove all added global callback parameters.
         */
        fun resetSessionCallbackParameters() {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.resetSessionCallbackParameters()
        }

        /**
         * Called to remove all added global partner parameters.
         */
        fun resetSessionPartnerParameters() {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.resetSessionPartnerParameters()
        }

        /**
         * Called to set user's push notifications token.
         *
         * @param token Push notifications token
         */
        @Deprecated("use {@link #setPushToken(String, Context)} instead.")
        fun setPushToken(token: String?) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.setPushToken(token)
        }

        /**
         * Called to set user's push notifications token.
         *
         * @param token   Push notifications token
         * @param context Application context
         */
        fun setPushToken(token: String, context: Context?) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            extractApplicationContext(context)?.let { motrackInstance.setPushToken(token, it) }
        }

        /**
         * Called to forget the user in accordance with GDPR law.
         *
         * @param context Application context
         */
        fun gdprForgetMe(context: Context?) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            extractApplicationContext(context)?.let { motrackInstance.gdprForgetMe(it) }
        }

        /**
         * Called to disable the third party sharing.
         *
         * @param context Application context
         */
        fun disableThirdPartySharing(context: Context?) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            extractApplicationContext(context)?.let { motrackInstance.disableThirdPartySharing(it) }
        }

        fun trackThirdPartySharing(motrackThirdPartySharing: MotrackThirdPartySharing) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.trackThirdPartySharing(motrackThirdPartySharing)
        }

        fun trackMeasurementConsent(consentMeasurement: Boolean) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.trackMeasurementConsent(consentMeasurement)
        }

        /**
         * Track ad revenue from a source provider
         *
         * @param source Source of ad revenue information, see MotrackConfig.AD_REVENUE_* for some possible sources
         * @param payload JsonObject content of the ad revenue information
         */
        fun trackAdRevenue(source: String?, payload: JSONObject?) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.trackAdRevenue(source, payload)
        }

        /**
         * Track ad revenue from a source provider
         *
         * @param motrackAdRevenue Motrack ad revenue information like source, revenue, currency etc
         */
        fun trackAdRevenue(motrackAdRevenue: MotrackAdRevenue) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.trackAdRevenue(motrackAdRevenue)
        }

        /**
         * Track subscription from Google Play.
         *
         * @param subscription MotrackPlayStoreSubscription object to be tracked
         */
        fun trackPlayStoreSubscription(subscription: MotrackPlayStoreSubscription) {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.trackPlayStoreSubscription(subscription)
        }


        /**
         * Called to get value of Google Play Advertising Identifier.
         *
         * @param context        Application context
         * @param onDeviceIdRead Callback to get triggered once identifier is obtained
         */
        fun getGoogleAdId(context: Context?, onDeviceIdRead: OnDeviceIdsRead) {
            var appContext: Context? = null
            context?.let { appContext = context.applicationContext }
            appContext?.let { AndroidUtil.getGoogleAdId(it, onDeviceIdRead) }
        }

        /**
         * Called to get value of Amazon Advertising Identifier.
         *
         * @param context Application context
         * @return Amazon Advertising Identifier
         */
        fun getAmazonAdId(context: Context?): String? {
            val appContext: Context? = extractApplicationContext(context)
            return if (appContext != null) {
                AndroidUtil.getFireAdvertisingId(appContext.contentResolver)
            } else null
        }

        /**
         * Called to get value of unique Motrack device identifier.
         *
         * @return Unique Motrack device identifier
         */
        fun getAdid(): String? {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            return motrackInstance.getAdid()
        }

        /**
         * Called to get user's current attribution value.
         *
         * @return MotrackAttribution object with current attribution value
         */
        fun getAttribution(): MotrackAttribution? {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            return motrackInstance.getAttribution()
        }

        /**
         * Called to get native SDK version string.
         *
         * @return Native SDK version string.
         */
        fun getSdkVersion(): String {
            val motrackInstance: MotrackInstance = getDefaultInstance()
            return motrackInstance.getSdkVersion()
        }

        /**
         * Used for testing purposes only. Do NOT use this method.
         *
         * @param testOptions Adjust integration tests options
         */
        fun setTestOptions(testOptions: MotrackTestOptions) {
            testOptions.teardown?.let {
                if (it) {
                    defaultInstance?.teardown()
                    defaultInstance = null
                    MotrackFactory.teardown(testOptions.context)
                }
            }

            val motrackInstance: MotrackInstance = getDefaultInstance()
            motrackInstance.setTestOptions(testOptions)
        }

        private fun extractApplicationContext(context: Context?): Context? {
            return context?.applicationContext
        }
    }
}