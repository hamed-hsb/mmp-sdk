package com.motrack.sdk

import Motrack
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.motrack.sdk.Constants.Companion.EXTRA_SYSTEM_INSTALLER_REFERRER

/**
 * @author yaya (@yahyalmh)
 * @since 19th October 2021
 *
 * You have to add this broadcast to manifest file like this:
 *
 *  <receiver android:name="com.motrack.sdk.MotrackPreinstallReferrerReceiver">
 *      <intent-filter>
 *       <action android:name="com.attribution.SYSTEM_INSTALLER_REFERRER" />
 *      </intent-filter>
 *  </receiver>
 */

class MotrackPreinstallReferrerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) {
            return
        }

        val referrer = intent.getStringExtra(EXTRA_SYSTEM_INSTALLER_REFERRER) ?: return

        Motrack.getDefaultInstance().sendPreinstallReferrer(referrer, context)
    }
}