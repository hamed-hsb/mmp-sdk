package com.motrack.sdk

/**
 * @author yaya (@yahyalmh)
 * @since 20th November 2021
 */

class StateSession(val sessionType: SessionType) {
    var toSend = true
    var sessionCount = 1
    var subsessionCount = 1
    var eventCount = 0
    var getAttributionIsCalled: Boolean? = null
    var eventBufferingIsEnabled = false
    var foregroundTimerStarts = true
    var foregroundTimerAlreadyStarted = false
    var sendInBackgroundConfigured = false
    var sdkClickHandlerAlsoStartsPaused = true
    var startSubsession = true
    var disabled = false
    var delayStart: String? = null
    var activityStateAlreadyCreated = false


    enum class SessionType {
        NEW_SESSION, NEW_SUBSESSION, TIME_TRAVEL, NONSESSION, DISABLED
    }
}