package com.motrack.sdk

/**
 * @author yaya (@yahyalmh)
 * @since 06th October 2021
 */

data class ReferrerDetails(
    var installReferrer: String,                          // The referrer URL of the installed package.
    var referrerClickTimestampSeconds: Long,              // The client-side timestamp, when the referrer click happened.
    var installBeginTimestampSeconds: Long,               // The client-side timestamp, when app installation began.
    var referrerClickTimestampServerSeconds: Long = -1,   // The server-side timestamp, when the referrer click happened.
    var installBeginTimestampServerSeconds: Long = -1,    // The server-side timestamp, when app installation began.
    var installVersion: String? = null,                   // The app's version at the time when the app was first installed.
    var googlePlayInstant: Boolean? = null                // Indicates whether app's instant experience was launched within the past 7 days.

)
