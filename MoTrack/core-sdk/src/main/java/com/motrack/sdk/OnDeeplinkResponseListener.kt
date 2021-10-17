package com.motrack.sdk

import android.net.Uri

/**
 * @author yaya (@yahyalmh)
 * @since 17th October 2021
 */

interface OnDeeplinkResponseListener {
    fun launchReceivedDeeplink(deeplink: Uri?): Boolean
}