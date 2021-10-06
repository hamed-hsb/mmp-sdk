package com.motrack.sdk

/**
 * @author yaya (@yahyalmh)
 * @since 06th October 2021
 */

class SdkClickResponseData : RespondData() {
    var isInstallReferrer = false
    var clickTime: Long? = null
    var installBegin: Long? = null
    var installReferrer: String? = null
    var clickTimeServer: Long? = null
    var installBeginServer: Long? = null
    var installVersion: String? = null
    var googlePlayInstant = false
    var referrerApi: String? = null
}