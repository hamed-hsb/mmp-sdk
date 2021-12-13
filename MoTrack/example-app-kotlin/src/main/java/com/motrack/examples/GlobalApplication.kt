package com.motrack.examples

import Motrack
import android.app.Activity
import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.motrack.sdk.*
import com.motrack.sdk.imei.MotrackImei
import com.motrack.sdk.oaid.MotrackOaid

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Configure adjust SDK.
        val appToken = "2fm9gkqubvpc"
        val environment = MotrackConfig.ENVIRONMENT_SANDBOX

        val config = MotrackConfig(this, appToken, environment)

        // Change the log level.
        config.setLogLevel(LogLevel.VERBOSE)

        // Set attribution delegate.
        config.onAttributionChangedListener = object : OnAttributionChangedListener {
            override fun onAttributionChanged(attribution: MotrackAttribution) {
                Log.d("example", "Attribution callback called!")
                Log.d("example", "Attribution: $attribution")
            }
        }

        // Set event success tracking delegate.
        config.onEventTrackingSucceededListener = object : OnEventTrackingFailedListener,
            OnEventTrackingSucceededListener {
            override fun onFinishedEventTrackingFailed(eventFailureResponseData: MotrackEventFailure) {
                Log.d("example", "Event Failure callback called!")
                Log.d("example", "Event Failure data: $eventFailureResponseData")
            }

            override fun onFinishedEventTrackingSucceeded(eventSuccessResponseData: MotrackEventSuccess) {
                Log.d("example", "Event success callback called!")
                Log.d("example", "Event success data: $eventSuccessResponseData")
            }
        }

        // Set event failure tracking delegate.
        config.onEventTrackingFailedListener = object : OnEventTrackingFailedListener {
            override fun onFinishedEventTrackingFailed(eventFailureResponseData: MotrackEventFailure) {
                Log.d("example", "Event failure callback called!")
                Log.d("example", "Event failure data: $eventFailureResponseData")
            }
        }

        // Set session success tracking delegate.
        config.onSessionTrackingSucceededListener = object : OnSessionTrackingSucceededListener {
            override fun onFinishedSessionTrackingSucceeded(sessionSuccessResponseData: MotrackSessionSuccess) {
                Log.d("example", "Session success callback called!")
                Log.d("example", "Session success data: $sessionSuccessResponseData")
            }
        }

        // Set session failure tracking delegate.
        config.onSessionTrackingFailedListener = object : OnSessionTrackingFailedListener {
            override fun onFinishedSessionTrackingFailed(failureResponseData: MotrackSessionFailure) {
                Log.d("example", "Session failure callback called!")
                Log.d("example", "Session failure data: $failureResponseData")
            }
        }

        // Evaluate deferred deep link to be launched.
        config.onDeeplinkResponseListener = object : OnDeeplinkResponseListener {
            override fun launchReceivedDeeplink(deeplink: Uri?): Boolean {
                Log.d("example", "Deferred deep link callback called!")
                Log.d("example", "Deep link URL: $deeplink")

                return true
            }
        }

        // Set default tracker.
        config.defaultTracker = "{YourDefaultTracker}"

        // Set process name.
        config.processName = "com.adjust.examples"

        // Allow to send in the background.
        config.sendInBackground = true

        // Enable event buffering.
        config.setEventBufferingEnabled(true)

        // Delay first session.
        config.delayStart = 7.toDouble()

        // Allow tracking preinstall
        config.preinstallTrackingEnabled = true

        // Add session callback parameters.
        Motrack.addSessionCallbackParameter("sc_foo", "sc_bar")
        Motrack.addSessionCallbackParameter("sc_key", "sc_value")

        // Add session partner parameters.
        Motrack.addSessionPartnerParameter("sp_foo", "sp_bar")
        Motrack.addSessionPartnerParameter("sp_key", "sp_value")

        // Remove session callback parameters.
        Motrack.removeSessionCallbackParameter("sc_foo")

        // Remove session partner parameters.
        Motrack.removeSessionPartnerParameter("sp_key")

        // Remove all session callback parameters.
        Motrack.resetSessionCallbackParameters()

        // Remove all session partner parameters.
        Motrack.resetSessionPartnerParameters()

        // Enable IMEI reading ONLY IF:
        // - IMEI plugin is added to your app.
        // - Your app is NOT distributed in Google Play Store.
        MotrackImei.readImei()

        // Enable OAID reading ONLY IF:
        // - OAID plugin is added to your app.
        // - Your app is NOT distributed in Google Play Store & supports OAID.
        MotrackOaid.readOaid()

        // Initialise the adjust SDK.
        Motrack.onCreate(config)

        // Abort delay for the first session introduced with setDelayStart method.
        // Motrack.sendFirstPackages();

        // Register onResume and onPause events of all activities
        // for applications with minSdkVersion >= 14.
        registerActivityLifecycleCallbacks(MotrackLifecycleCallbacks())

        // Put the SDK in offline mode.
        // Motrack.setOfflineMode(true);

        // Disable the SDK
        Motrack.setEnabled(false)

        // Send push notification token.
        // Motrack.setPushToken("token");

    }

    // You can use this class if your app is for Android 4.0 or higher
    private class MotrackLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            Motrack.onResume()
        }

        override fun onActivityPaused(activity: Activity) {
            Motrack.onPause()
        }

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {}
    }
}
