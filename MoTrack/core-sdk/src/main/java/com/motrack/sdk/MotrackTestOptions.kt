package com.motrack.sdk

import android.content.Context

/**
 * @author yaya (@yahyalmh)
 * @since 16th November 2021
 */

class MotrackTestOptions {
    var context: Context? = null
    var baseUrl: String? = null
    var gdprUrl: String? = null
    var subscriptionUrl: String? = null
    var basePath: String? = null
    var gdprPath: String? = null
    var subscriptionPath: String? = null
    var timerIntervalInMilliseconds: Long? = null
    var timerStartInMilliseconds: Long? = null
    var sessionIntervalInMilliseconds: Long? = null
    var subsessionIntervalInMilliseconds: Long? = null
    var teardown: Boolean? = null
    var tryInstallReferrer = false
    var noBackoffWait: Boolean? = null
    var enableSigning: Boolean? = null
    var disableSigning: Boolean? = null
}