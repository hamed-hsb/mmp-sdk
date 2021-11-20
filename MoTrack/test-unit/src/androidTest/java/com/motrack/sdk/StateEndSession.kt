package com.motrack.sdk

/**
 * @author yaya (@yahyalmh)
 * @since 20th November 2021
 */

class StateEndSession {
    var pausing = true
    var updateActivityState = true
    var eventBufferingEnabled = false
    var checkOnPause = false
    var foregroundAlreadySuspended = false
    var backgroundTimerStarts = false
}