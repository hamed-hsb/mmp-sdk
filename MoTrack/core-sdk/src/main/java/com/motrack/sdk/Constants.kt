package com.motrack.sdk

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */


class Constants {
    companion object {
        var ONE_SECOND: Long = 1000
        var ONE_MINUTE: Long = 60 * ONE_SECOND
        var THIRTY_MINUTES: Long = 30 * ONE_MINUTE
        var ONE_HOUR: Long = 2 * THIRTY_MINUTES

        var MAX_INSTALL_REFERRER_RETRIES = 2

        var ACTIVITY_STATE_FILENAME = "MotrackIoActivityState"
        var ATTRIBUTION_FILENAME = "MotrackAttribution"
        var SESSION_CALLBACK_PARAMETERS_FILENAME = "MotrackSessionCallbackParameters"
        var SESSION_PARTNER_PARAMETERS_FILENAME = "MotrackSessionPartnerParameters"

        var ENCODING = "UTF-8"
        var MD5 = "MD5"
        var SHA1 = "SHA-1"
        var SHA256 = "SHA-256"


        const val LOGTAG = "MoTrack"
        var CLIENT_SDK = "android4.28.5"
        var THREAD_PREFIX = "Motrack-"
    }
}