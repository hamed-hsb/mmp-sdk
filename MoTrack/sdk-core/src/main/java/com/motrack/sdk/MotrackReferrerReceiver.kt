package com.motrack.sdk

import Motrack
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.motrack.sdk.Constants.Companion.REFERRER

/**
 * @author yaya (@yahyalmh)
 * @since 19th October 2021
 *
 *  support multiple BroadcastReceivers for the INSTALL_REFERRER:
 *  https://appington.wordpress.com/2012/08/01/giving-credit-for-android-app-installs/
 *
 *  You should capture the Google Play Store `INSTALL_REFERRER` intent with a broadcast receiver.
 *  If you are **not using your own broadcast receiver** to receive the `INSTALL_REFERRER` intent,
 *  add the following `receiver` tag inside the `application` tag in your `AndroidManifest.xml`.
 *
 * <receiver
 *     android:name="com.motrack.sdk.MotrackReferrerReceiver"
 *     android:permission="android.permission.INSTALL_PACKAGES"
 *     android:exported="true" >
 *           <intent-filter>
 *              <action android:name="com.android.vending.INSTALL_REFERRER" />
 *           </intent-filter>
 * </receiver>
 */

class MotrackReferrerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val rawReferrer = intent!!.getStringExtra(REFERRER) ?: return

        Motrack.getDefaultInstance().sendReferrer(rawReferrer, context)
    }
}