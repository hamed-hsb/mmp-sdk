package com.motrack.testapp

import Motrack
import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.SparseArray
import com.motrack.sdk.*
import com.motrack.test_options.TestConnectionOptions
import org.json.JSONException
import org.json.JSONObject

/**
 * @author yaya (@yahyalmh)
 * @since 21th November 2021
 */

class AdjustCommandExecutor(private var context: Context?) {
    private val TAG = "AdjustCommandExecutor"
    private var basePath: String? = null
    private var gdprPath: String? = null
    private var subscriptionPath: String? = null
    private var savedEvents: SparseArray<MotrackEvent>? = SparseArray()
    private var savedConfigs: SparseArray<MotrackConfig>? = SparseArray()
    private var command: Command? = null


    fun executeCommand(command: Command) {
        this.command = command
        when (command.methodName) {
            "testOptions" -> testOptions()
            "config" -> config()
            "start" -> start()
            "event" -> event()
            "trackEvent" -> trackEvent()
            "resume" -> resume()
            "pause" -> pause()
            "setEnabled" -> setEnabled()
            "setReferrer" -> setReferrer()
            "setOfflineMode" -> setOfflineMode()
            "sendFirstPackages" -> sendFirstPackages()
            "addSessionCallbackParameter" -> addSessionCallbackParameter()
            "addSessionPartnerParameter" -> addSessionPartnerParameter()
            "removeSessionCallbackParameter" -> removeSessionCallbackParameter()
            "removeSessionPartnerParameter" -> removeSessionPartnerParameter()
            "resetSessionCallbackParameters" -> resetSessionCallbackParameters()
            "resetSessionPartnerParameters" -> resetSessionPartnerParameters()
            "setPushToken" -> setPushToken()
            "openDeeplink" -> openDeeplink()
            "sendReferrer" -> sendReferrer()
            "gdprForgetMe" -> gdprForgetMe()
            "disableThirdPartySharing" -> disableThirdPartySharing()
            "thirdPartySharing" -> thirdPartySharing()
            "measurementConsent" -> measurementConsent()
            "trackAdRevenue" -> trackAdRevenue()
            "trackAdRevenueV2" -> trackAdRevenueV2()
            "trackSubscription" -> trackSubscription()
        }
    }


    /*
    private void factory() {
        if (command.containsParameter("basePath")) {
            this.basePath = command.getFirstParameterValue("basePath");
        }
        if (command.containsParameter("timerInterval")) {
            long timerInterval = Long.parseLong(command.getFirstParameterValue("timerInterval"));
            AdjustFactory.setTimerInterval(timerInterval);
        }
        if (command.containsParameter("timerStart")) {
            long timerStart = Long.parseLong(command.getFirstParameterValue("timerStart"));
            AdjustFactory.setTimerStart(timerStart);
        }
        if (command.containsParameter("sessionInterval")) {
            long sessionInterval = Long.parseLong(command.getFirstParameterValue("sessionInterval"));
            AdjustFactory.setSessionInterval(sessionInterval);
        }
        if (command.containsParameter("subsessionInterval")) {
            long subsessionInterval = Long.parseLong(command.getFirstParameterValue("subsessionInterval"));
            AdjustFactory.setSubsessionInterval(subsessionInterval);
        }
    }
*/
    private fun testOptions() {
        val testOptions = MotrackTestOptions()
        testOptions.baseUrl = MainActivity.baseUrl
        testOptions.gdprUrl = MainActivity.gdprUrl
        testOptions.subscriptionUrl =
            MainActivity.baseUrl // TODO: for now, consider making it separate
        if (command!!.containsParameter("basePath")) {
            basePath = command!!.getFirstParameterValue("basePath")
            gdprPath = command!!.getFirstParameterValue("basePath")
            subscriptionPath = command!!.getFirstParameterValue("basePath")
        }
        if (command!!.containsParameter("timerInterval")) {
            val timerInterval = command!!.getFirstParameterValue("timerInterval")!!.toLong()
            testOptions.timerIntervalInMilliseconds = timerInterval
        }
        if (command!!.containsParameter("timerStart")) {
            val timerStart = command!!.getFirstParameterValue("timerStart")!!.toLong()
            testOptions.timerStartInMilliseconds = timerStart
        }
        if (command!!.containsParameter("sessionInterval")) {
            val sessionInterval = command!!.getFirstParameterValue("sessionInterval")!!.toLong()
            testOptions.sessionIntervalInMilliseconds = sessionInterval
        }
        if (command!!.containsParameter("subsessionInterval")) {
            val subsessionInterval = command!!.getFirstParameterValue("subsessionInterval")!!
                .toLong()
            testOptions.subsessionIntervalInMilliseconds = subsessionInterval
        }
        if (command!!.containsParameter("tryInstallReferrer")) {
            val tryInstallReferrerString = command!!.getFirstParameterValue("tryInstallReferrer")
            val tryInstallReferrerBoolean =
                Util.strictParseStringToBoolean(tryInstallReferrerString)
            if (tryInstallReferrerBoolean != null) {
                testOptions.tryInstallReferrer = tryInstallReferrerBoolean
            }
        }
        if (command!!.containsParameter("noBackoffWait")) {
            val noBackoffWaitString = command!!.getFirstParameterValue("noBackoffWait")
            val noBackoffWaitBoolean = Util.strictParseStringToBoolean(noBackoffWaitString)
            if (noBackoffWaitBoolean != null) {
                testOptions.noBackoffWait = noBackoffWaitBoolean
            }
        }
        var useTestConnectionOptions = false
        if (command!!.containsParameter("teardown")) {
            val teardownOptions = command!!.parameters!!["teardown"]!!
            for (teardownOption in teardownOptions) {
                if (teardownOption == "resetSdk") {
                    testOptions.teardown = true
                    testOptions.basePath = basePath
                    testOptions.gdprPath = gdprPath
                    testOptions.subscriptionPath = subscriptionPath
                    useTestConnectionOptions = true
                    testOptions.tryInstallReferrer = false
                }
                if (teardownOption == "deleteState") {
                    testOptions.context = context
                }
                if (teardownOption == "resetTest") {
                    savedEvents?.clear()
                    savedConfigs?.clear()
                    testOptions.timerIntervalInMilliseconds = (-1).toLong()
                    testOptions.timerStartInMilliseconds = (-1).toLong()
                    testOptions.sessionIntervalInMilliseconds = (-1).toLong()
                    testOptions.subsessionIntervalInMilliseconds = (-1).toLong()
                }
                if (teardownOption == "sdk") {
                    testOptions.teardown = true
                    testOptions.basePath = null
                    testOptions.gdprPath = null
                    testOptions.subscriptionPath = null
                }
                if (teardownOption == "test") {
                    savedEvents = null
                    savedConfigs = null
                    testOptions.timerIntervalInMilliseconds = (-1).toLong()
                    testOptions.timerStartInMilliseconds = (-1).toLong()
                    testOptions.sessionIntervalInMilliseconds = (-1).toLong()
                    testOptions.subsessionIntervalInMilliseconds = (-1).toLong()
                }
            }
        }
        Motrack.setTestOptions(testOptions)
        if (useTestConnectionOptions) {
            TestConnectionOptions.setTestConnectionOptions()
        }
    }

    private fun config() {
        var configNumber = 0
        if (command!!.parameters!!.containsKey("configName")) {
            val configName = command!!.getFirstParameterValue("configName")
            configNumber = configName!!.substring(configName.length - 1).toInt()
        }
        val adjustConfig: MotrackConfig
        if (savedConfigs!!.indexOfKey(configNumber) >= 0) {
            adjustConfig = savedConfigs!!.get(configNumber)
        } else {
            val environment = command!!.getFirstParameterValue("environment")
            val appToken = command!!.getFirstParameterValue("appToken")
            var context = context
            if ("null".equals(command!!.getFirstParameterValue("context"), ignoreCase = true)) {
                context = null
            }
            adjustConfig = MotrackConfig(context, appToken, environment!!)
            //            String logLevel = command.getFirstParameterValue("logLevel");
//            adjustConfig.setLogLevel(LogLevel.valueOf(logLevel));
            adjustConfig.setLogLevel(LogLevel.VERBOSE)
            savedConfigs!!.put(configNumber, adjustConfig)
        }
        if (command!!.containsParameter("logLevel")) {
            val logLevel: LogLevel = when (command!!.getFirstParameterValue("logLevel")) {
                "verbose" -> LogLevel.VERBOSE
                "debug" -> LogLevel.DEBUG
                "info" -> LogLevel.INFO
                "warn" -> LogLevel.WARN
                "error" -> LogLevel.ERROR
                "assert" -> LogLevel.ASSERT
                "suppress" -> LogLevel.SUPPRESS
                else -> LogLevel.SUPPRESS
            }
            Log.d("TestApp", logLevel.toString())
            adjustConfig.setLogLevel(logLevel)
        }
        if (command!!.containsParameter("sdkPrefix")) {
            val sdkPrefix = command!!.getFirstParameterValue("sdkPrefix")
            adjustConfig.sdkPrefix = sdkPrefix
        }
        if (command!!.containsParameter("defaultTracker")) {
            val defaultTracker = command!!.getFirstParameterValue("defaultTracker")
            adjustConfig.defaultTracker = defaultTracker
        }

//        if (command.containsParameter("externalDeviceId")) {
//            String externalDeviceId = command.getFirstParameterValue("externalDeviceId");
//            adjustConfig.setExternalDeviceId(externalDeviceId);
//        }
        if (command!!.parameters!!.containsKey("appSecret")) {
            val appSecretArray = command!!.parameters!!["appSecret"]!!
            try {
                val secretId = appSecretArray[0].toLong()
                val info1 = appSecretArray[1].toLong()
                val info2 = appSecretArray[2].toLong()
                val info3 = appSecretArray[3].toLong()
                val info4 = appSecretArray[4].toLong()
                adjustConfig.setAppSecret(secretId, info1, info2, info3, info4)
            } catch (ignored: Exception) {
            }
        }
        if (command!!.containsParameter("delayStart")) {
            val delayStartS = command!!.getFirstParameterValue("delayStart")
            val delayStart = delayStartS!!.toDouble()
            adjustConfig.delayStart = delayStart
        }
        if (command!!.containsParameter("deviceKnown")) {
            val deviceKnownS = command!!.getFirstParameterValue("deviceKnown")
            val deviceKnown = "true" == deviceKnownS
            adjustConfig.deviceKnown = deviceKnown
        }
        if (command!!.containsParameter("needsCost")) {
            val needsCostS = command!!.getFirstParameterValue("needsCost")
            val needsCost = "true" == needsCostS
            adjustConfig.needsCost = needsCost
        }
        if (command!!.containsParameter("eventBufferingEnabled")) {
            val eventBufferingEnabledS = command!!.getFirstParameterValue("eventBufferingEnabled")
            val eventBufferingEnabled = "true" == eventBufferingEnabledS
            adjustConfig.setEventBufferingEnabled(eventBufferingEnabled)
        }
        if (command!!.containsParameter("sendInBackground")) {
            val sendInBackgroundS = command!!.getFirstParameterValue("sendInBackground")
            val sendInBackground = "true" == sendInBackgroundS
            adjustConfig.sendInBackground = sendInBackground
        }
        if (command!!.containsParameter("userAgent")) {
            val userAgent = command!!.getFirstParameterValue("userAgent")
            adjustConfig.userAgent = userAgent
        }
        if (command!!.containsParameter("externalDeviceId")) {
            val externalDeviceId = command!!.getFirstParameterValue("externalDeviceId")
            adjustConfig.externalDeviceId = externalDeviceId
        }
        if (command!!.containsParameter("deferredDeeplinkCallback")) {
            adjustConfig.onDeeplinkResponseListener = object : OnDeeplinkResponseListener {
                override fun launchReceivedDeeplink(deeplink: Uri?): Boolean {
                    if (deeplink == null) {
                        Log.d("TestApp", "Deeplink Response, uri = null")
                        return false
                    }
                    Log.d("TestApp", "Deeplink Response, uri = $deeplink")
                    return deeplink.toString().startsWith("motracktest")
                }
            }
        }
        if (command!!.containsParameter("attributionCallbackSendAll")) {
            val localBasePath = basePath!!
            adjustConfig.onAttributionChangedListener = object : OnAttributionChangedListener {
                override fun onAttributionChanged(attribution: MotrackAttribution) {
                    Log.d("TestApp", "attribution = $attribution")
                    MainActivity.testLibrary.addInfoToSend(
                        "trackerToken",
                        attribution.trackerToken
                    )
                    MainActivity.testLibrary.addInfoToSend("trackerName", attribution.trackerName)
                    MainActivity.testLibrary.addInfoToSend("network", attribution.network)
                    MainActivity.testLibrary.addInfoToSend("campaign", attribution.campaign)
                    MainActivity.testLibrary.addInfoToSend("adgroup", attribution.adgroup)
                    MainActivity.testLibrary.addInfoToSend("creative", attribution.creative)
                    MainActivity.testLibrary.addInfoToSend("clickLabel", attribution.clickLabel)
                    MainActivity.testLibrary.addInfoToSend("adid", attribution.adid)
                    MainActivity.testLibrary.addInfoToSend("costType", attribution.costType)
                    MainActivity.testLibrary.addInfoToSend(
                        "costAmount",
                        attribution.costAmount.toString()
                    )
                    MainActivity.testLibrary.addInfoToSend(
                        "costCurrency",
                        attribution.costCurrency
                    )
                    MainActivity.testLibrary.sendInfoToServer(localBasePath)
                }
            }
        }
        if (command!!.containsParameter("sessionCallbackSendSuccess")) {
            val localBasePath = basePath!!
            adjustConfig.onSessionTrackingSucceededListener = object :
                OnSessionTrackingSucceededListener {
                override fun onFinishedSessionTrackingSucceeded(sessionSuccessResponseData: MotrackSessionSuccess) {
                    Log.d("TestApp", "session_success = $sessionSuccessResponseData")
                    sessionSuccessResponseData.message?.let {
                        MainActivity.testLibrary.addInfoToSend(
                            "message",
                            it
                        )
                    }
                    sessionSuccessResponseData.timestamp?.let {
                        MainActivity.testLibrary.addInfoToSend(
                            "timestamp",
                            it
                        )
                    }
                    sessionSuccessResponseData.adid?.let {
                        MainActivity.testLibrary.addInfoToSend(
                            "adid",
                            it
                        )
                    }
                    sessionSuccessResponseData.jsonResponse?.let {
                        MainActivity.testLibrary.addInfoToSend(
                            "jsonResponse",
                            it.toString()
                        )
                    }
                    MainActivity.testLibrary.sendInfoToServer(localBasePath)
                }
            }
        }
        if (command!!.containsParameter("sessionCallbackSendFailure")) {
            val localBasePath = basePath
            adjustConfig.onSessionTrackingFailedListener = object :
                OnSessionTrackingFailedListener {
                override fun onFinishedSessionTrackingFailed(failureResponseData: MotrackSessionFailure) {
                    Log.d("TestApp", "session_fail = $failureResponseData")
                    failureResponseData.message?.let {
                        MainActivity.testLibrary.addInfoToSend("message", it)
                    }
                    failureResponseData.timestamp?.let {
                        MainActivity.testLibrary.addInfoToSend("timestamp", it)
                    }
                    failureResponseData.adid?.let {
                        MainActivity.testLibrary.addInfoToSend("adid", it)
                    }
                    MainActivity.testLibrary.addInfoToSend(
                        "willRetry",
                        java.lang.String.valueOf(failureResponseData.willRetry)
                    )
                    if (failureResponseData.jsonResponse != null) {
                        MainActivity.testLibrary.addInfoToSend(
                            "jsonResponse",
                            failureResponseData.jsonResponse.toString()
                        )
                    }
                    MainActivity.testLibrary.sendInfoToServer(localBasePath!!)
                }
            }
        }
        if (command!!.containsParameter("eventCallbackSendSuccess")) {
            val localBasePath = basePath
            adjustConfig.onEventTrackingSucceededListener = object :
                OnEventTrackingSucceededListener {
                override fun onFinishedEventTrackingSucceeded(eventSuccessResponseData: MotrackEventSuccess) {
                    Log.d("TestApp", "event_success = $eventSuccessResponseData")
                    eventSuccessResponseData.message?.let {
                        MainActivity.testLibrary.addInfoToSend(
                            "message",
                            it
                        )
                    }
                    eventSuccessResponseData.timestamp?.let {
                        MainActivity.testLibrary.addInfoToSend(
                            "timestamp",
                            it
                        )
                    }
                    eventSuccessResponseData.adid?.let {
                        MainActivity.testLibrary.addInfoToSend("adid", it)
                    }
                    eventSuccessResponseData.eventToken?.let {
                        MainActivity.testLibrary.addInfoToSend("eventToken", it)
                    }
                    eventSuccessResponseData.callbackId?.let {
                        MainActivity.testLibrary.addInfoToSend("callbackId", it)
                    }
                    if (eventSuccessResponseData.jsonResponse != null) {
                        MainActivity.testLibrary.addInfoToSend(
                            "jsonResponse",
                            eventSuccessResponseData.jsonResponse.toString()
                        )
                    }
                    MainActivity.testLibrary.sendInfoToServer(localBasePath!!)
                }
            }
        }
        if (command!!.containsParameter("eventCallbackSendFailure")) {
            val localBasePath = basePath!!
            adjustConfig.onEventTrackingFailedListener = object : OnEventTrackingFailedListener {
                override fun onFinishedEventTrackingFailed(eventFailureResponseData: MotrackEventFailure) {
                    Log.d("TestApp", "event_fail = $eventFailureResponseData")
                    eventFailureResponseData.message?.let {
                        MainActivity.testLibrary.addInfoToSend("message", it)
                    }
                    eventFailureResponseData.timestamp?.let {
                        MainActivity.testLibrary.addInfoToSend("timestamp", it)
                    }
                    eventFailureResponseData.adid?.let {
                        MainActivity.testLibrary.addInfoToSend("adid", it)
                    }
                    eventFailureResponseData.eventToken?.let {
                        MainActivity.testLibrary.addInfoToSend("eventToken", it)
                    }
                    eventFailureResponseData.callbackId?.let {
                        MainActivity.testLibrary.addInfoToSend("callbackId", it)
                    }
                    MainActivity.testLibrary.addInfoToSend(
                        "willRetry",
                        java.lang.String.valueOf(eventFailureResponseData.willRetry)
                    )
                    if (eventFailureResponseData.jsonResponse != null) {
                        MainActivity.testLibrary.addInfoToSend(
                            "jsonResponse",
                            eventFailureResponseData.jsonResponse.toString()
                        )
                    }
                    MainActivity.testLibrary.sendInfoToServer(localBasePath)
                }
            }
        }
        if (command!!.containsParameter("deferredDeeplinkCallback")) {
            val launchDeferredDeeplinkS =
                command!!.getFirstParameterValue("deferredDeeplinkCallback")
            val launchDeferredDeeplink = "true" == launchDeferredDeeplinkS
            val localBasePath = basePath!!
            adjustConfig.onDeeplinkResponseListener = object : OnDeeplinkResponseListener {
                override fun launchReceivedDeeplink(deeplink: Uri?): Boolean {
                    Log.d("TestApp", "deferred_deep_link = $deeplink")
                    MainActivity.testLibrary.addInfoToSend("deeplink", deeplink.toString())
                    MainActivity.testLibrary.sendInfoToServer(localBasePath)
                    return launchDeferredDeeplink
                }
            }
        }
    }

    private fun start() {
        config()
        var configNumber = 0
        if (command!!.parameters!!.containsKey("configName")) {
            val configName = command!!.getFirstParameterValue("configName")
            configNumber = configName!!.substring(configName.length - 1).toInt()
        }
        val adjustConfig: MotrackConfig = savedConfigs!![configNumber]

        //adjustConfig.setBasePath(basePath);
        Motrack.onCreate(adjustConfig)
        savedConfigs!!.remove(0)
    }

    @Throws(NullPointerException::class)
    private fun event() {
        var eventNumber = 0
        if (command!!.parameters!!.containsKey("eventName")) {
            val eventName = command!!.getFirstParameterValue("eventName")
            eventNumber = eventName!!.substring(eventName.length - 1).toInt()
        }
        val adjustEvent: MotrackEvent
        if (savedEvents!!.indexOfKey(eventNumber) >= 0) {
            adjustEvent = savedEvents!!.get(eventNumber)
        } else {
            val eventToken = command!!.getFirstParameterValue("eventToken")
            adjustEvent = MotrackEvent(eventToken)
            savedEvents!!.put(eventNumber, adjustEvent)
        }
        if (command!!.parameters!!.containsKey("revenue")) {
            val revenueParams = command!!.parameters!!["revenue"]!!
            val currency = revenueParams[0]
            val revenue = revenueParams[1].toDouble()
            adjustEvent.setRevenue(revenue, currency)
        }
        if (command!!.parameters!!.containsKey("callbackParams")) {
            val callbackParams = command!!.parameters!!["callbackParams"]!!
            var i = 0
            while (i < callbackParams.size) {
                val key = callbackParams[i]
                val value = callbackParams[i + 1]
                adjustEvent.addCallbackParameter(key, value)
                i += 2
            }
        }
        if (command!!.parameters!!.containsKey("partnerParams")) {
            val partnerParams = command!!.parameters!!["partnerParams"]!!
            var i = 0
            while (i < partnerParams.size) {
                val key = partnerParams[i]
                val value = partnerParams[i + 1]
                adjustEvent.addPartnerParameter(key, value)
                i += 2
            }
        }
        if (command!!.parameters!!.containsKey("orderId")) {
            val orderId = command!!.getFirstParameterValue("orderId")
            orderId?.let {
                adjustEvent.setOrderId(orderId)
            }
        }
        if (command!!.parameters!!.containsKey("callbackId")) {
            val callbackId = command!!.getFirstParameterValue("callbackId")
            callbackId?.let {
                adjustEvent.setCallbackId(callbackId)
            }
        }

//        Adjust.trackEvent(adjustEvent);
    }

    private fun trackEvent() {
        event()
        var eventNumber = 0
        if (command!!.parameters!!.containsKey("eventName")) {
            val eventName = command!!.getFirstParameterValue("eventName")
            eventNumber = eventName!!.substring(eventName.length - 1).toInt()
        }
        val adjustEvent: MotrackEvent = savedEvents!![eventNumber]
        Motrack.trackEvent(adjustEvent)
        savedEvents!!.remove(0)
    }

    private fun setReferrer() {
        val referrer = command!!.getFirstParameterValue("referrer")
        Motrack.setReferrer(referrer, context)
    }

    private fun pause() {
        Motrack.onPause()
    }

    private fun resume() {
        Motrack.onResume()
    }

    private fun setEnabled() {
        val enabled = java.lang.Boolean.valueOf(command!!.getFirstParameterValue("enabled"))
        Motrack.setEnabled(enabled)
    }

    private fun setOfflineMode() {
        val enabled = java.lang.Boolean.valueOf(command!!.getFirstParameterValue("enabled"))
        Motrack.setOfflineMode(enabled)
    }

    private fun sendFirstPackages() {
        Motrack.sendFirstPackages()
    }

    private fun addSessionCallbackParameter() {
        if (command!!.containsParameter("KeyValue")) {
            val keyValuePairs = command!!.parameters!!["KeyValue"]!!
            var i = 0
            while (i < keyValuePairs.size) {
                val key = keyValuePairs[i]
                val value = keyValuePairs[i + 1]
                Motrack.addSessionCallbackParameter(key, value)
                i += 2
            }
        }
    }

    private fun addSessionPartnerParameter() {
        if (command!!.containsParameter("KeyValue")) {
            val keyValuePairs = command!!.parameters!!["KeyValue"]!!
            var i = 0
            while (i < keyValuePairs.size) {
                val key = keyValuePairs[i]
                val value = keyValuePairs[i + 1]
                Motrack.addSessionPartnerParameter(key, value)
                i += 2
            }
        }
    }

    private fun removeSessionCallbackParameter() {
        if (command!!.containsParameter("key")) {
            val keys = command!!.parameters!!["key"]!!
            var i = 0
            while (i < keys.size) {
                val key = keys[i]
                Motrack.removeSessionCallbackParameter(key)
                i += 1
            }
        }
    }

    private fun removeSessionPartnerParameter() {
        if (command!!.containsParameter("key")) {
            val keys = command!!.parameters!!["key"]!!
            var i = 0
            while (i < keys.size) {
                val key = keys[i]
                Motrack.removeSessionPartnerParameter(key)
                i += 1
            }
        }
    }

    private fun resetSessionCallbackParameters() {
        Motrack.resetSessionCallbackParameters()
    }

    private fun resetSessionPartnerParameters() {
        Motrack.resetSessionPartnerParameters()
    }

    private fun setPushToken() {
        val token = command!!.getFirstParameterValue("pushToken")
        if (token != null) {
            context?.let { Motrack.setPushToken(token, it) }
        }
    }

    /*
        private void teardown() throws NullPointerException {
            String deleteStateString = command.getFirstParameterValue("deleteState");
            boolean deleteState = Boolean.parseBoolean(deleteStateString);

            Log.d("TestApp", "calling teardown with delete state");
            AdjustFactory.teardown(this.context, deleteState);
        }
    */
    private fun openDeeplink() {
        val deeplink = command!!.getFirstParameterValue("deeplink")
        Motrack.appWillOpenUrl(Uri.parse(deeplink), context)
    }

    private fun sendReferrer() {
        val referrer = command!!.getFirstParameterValue("referrer")
        Motrack.setReferrer(referrer, context)
    }

    private fun gdprForgetMe() {
        Motrack.gdprForgetMe(context)
    }

    private fun disableThirdPartySharing() {
        context?.let { Motrack.disableThirdPartySharing(it) }
    }

    private fun thirdPartySharing() {
        val isEnabledString = command!!.getFirstParameterValue("isEnabled")
        val isEnabledBoolean = Util.strictParseStringToBoolean(isEnabledString)
        val adjustThirdPartySharing = MotrackThirdPartySharing(isEnabledBoolean)
        if (command!!.parameters!!.containsKey("granularOptions")) {
            val granularOptions = command!!.parameters!!["granularOptions"]!!
            var i = 0
            while (i < granularOptions.size) {
                val partnerName = granularOptions[i]
                val key = granularOptions[i + 1]
                val value = granularOptions[i + 2]
                adjustThirdPartySharing.addGranularOption(partnerName, key, value)
                i += 3
            }
        }
        Motrack.trackThirdPartySharing(adjustThirdPartySharing)
    }

    private fun measurementConsent() {
        val measurementConsentString = command!!.getFirstParameterValue("isEnabled")
        val measurementConsent = "true" == measurementConsentString
        Motrack.trackMeasurementConsent(measurementConsent)
    }

    private fun trackAdRevenue() {
        val adRevenueSource = command!!.getFirstParameterValue("adRevenueSource")
        val adRevenueJsonString = command!!.getFirstParameterValue("adRevenueJsonString")
        try {
            val adRevenueJson = JSONObject(adRevenueJsonString!!)
            Motrack.trackAdRevenue(adRevenueSource, adRevenueJson)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun trackAdRevenueV2() {
        val adRevenueSource = command!!.getFirstParameterValue("adRevenueSource")
        val adjustAdRevenue = MotrackAdRevenue(adRevenueSource)
        if (command!!.parameters!!.containsKey("revenue")) {
            val revenueParams = command!!.parameters!!["revenue"]!!
            val currency = revenueParams[0]
            val revenue = java.lang.Double.valueOf(revenueParams[1])
            adjustAdRevenue.setRevenue(revenue, currency)
        }
        if (command!!.parameters!!.containsKey("adImpressionsCount")) {
            val adImpressionsCount =
                Integer.valueOf(command!!.getFirstParameterValue("adImpressionsCount")!!)
            adjustAdRevenue.adImpressionsCount = adImpressionsCount
        }
        if (command!!.parameters!!.containsKey("adRevenueNetwork")) {
            val adRevenueNetwork = command!!.getFirstParameterValue("adRevenueNetwork")
            adjustAdRevenue.adRevenueNetwork = adRevenueNetwork
        }
        if (command!!.parameters!!.containsKey("adRevenueUnit")) {
            val adRevenueUnit = command!!.getFirstParameterValue("adRevenueUnit")
            adjustAdRevenue.adRevenueUnit = adRevenueUnit
        }
        if (command!!.parameters!!.containsKey("adRevenuePlacement")) {
            val adRevenuePlacement = command!!.getFirstParameterValue("adRevenuePlacement")
            adjustAdRevenue.adRevenuePlacement = adRevenuePlacement
        }
        if (command!!.parameters!!.containsKey("callbackParams")) {
            val callbackParams = command!!.parameters!!["callbackParams"]!!
            var i = 0
            while (i < callbackParams.size) {
                val key = callbackParams[i]
                val value = callbackParams[i + 1]
                adjustAdRevenue.addCallbackParameter(key, value)
                i += 2
            }
        }
        if (command!!.parameters!!.containsKey("partnerParams")) {
            val partnerParams = command!!.parameters!!["partnerParams"]!!
            var i = 0
            while (i < partnerParams.size) {
                val key = partnerParams[i]
                val value = partnerParams[i + 1]
                adjustAdRevenue.addPartnerParameter(key, value)
                i += 2
            }
        }
        Motrack.trackAdRevenue(adjustAdRevenue)
    }

    private fun trackSubscription() {
        val price = command!!.getFirstParameterValue("revenue")!!.toLong()
        val currency = command!!.getFirstParameterValue("currency")
        val purchaseTime = command!!.getFirstParameterValue("transactionDate")!!.toLong()
        val sku = command!!.getFirstParameterValue("productId")
        val signature = command!!.getFirstParameterValue("receipt")
        val purchaseToken = command!!.getFirstParameterValue("purchaseToken")
        val orderId = command!!.getFirstParameterValue("transactionId")
        val subscription = MotrackPlayStoreSubscription(
            price,
            currency,
            sku,
            orderId,
            signature,
            purchaseToken
        )
        subscription.setPurchaseTime(purchaseTime)
        if (command!!.parameters!!.containsKey("callbackParams")) {
            val callbackParams = command!!.parameters!!["callbackParams"]!!
            var i = 0
            while (i < callbackParams.size) {
                val key = callbackParams[i]
                val value = callbackParams[i + 1]
                subscription.addCallbackParameter(key, value)
                i += 2
            }
        }
        if (command!!.parameters!!.containsKey("partnerParams")) {
            val partnerParams = command!!.parameters!!["partnerParams"]!!
            var i = 0
            while (i < partnerParams.size) {
                val key = partnerParams[i]
                val value = partnerParams[i + 1]
                subscription.addPartnerParameter(key, value)
                i += 2
            }
        }
        Motrack.trackPlayStoreSubscription(subscription)
    }
/*
    private void testBegin() {
        if (command.containsParameter("teardown")) {
            this.basePath = command.getFirstParameterValue("basePath");
        }

        AdjustTestOptions teardownOption = new AdjustTestOptions();
        teardownOption.teardown = true;
        teardownOption.context = this.context;

        AdjustFactory.teardown(this.context);
        AdjustFactory.setTimerInterval(-1);
        AdjustFactory.setTimerStart(-1);
        AdjustFactory.setSessionInterval(-1);
        AdjustFactory.setSubsessionInterval(-1);
        savedEvents = new HashMap<Integer, AdjustEvent>();
        savedConfigs = new HashMap<Integer, AdjustConfig>();
    }

    private void testEnd() {
        AdjustFactory.teardown(this.context, true);
    }
    */

}