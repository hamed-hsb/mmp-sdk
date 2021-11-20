package com.motrack.sdk

/**
 * @author yaya (@yahyalmh)
 * @since 20th November 2021
 */

class StateActivityHandlerInit(private val activityHandler: ActivityHandler) {
    var internalState: ActivityHandler.InternalState? = null
    var startEnabled = true
    var updatePackages = false
    var startsSending = false
    var sdkClickHandlerAlsoStartsPaused = true
    var defaultTracker: String? = null
    var pushToken: String? = null
    var eventBufferingIsEnabled = false
    var sendInBackgroundConfigured = false
    var delayStartConfigured = false
    var activityStateAlreadyCreated = false
    var sendReferrer: String? = null
    var readActivityState: String? = null
    var readAttribution: String? = null
    var readCallbackParameters: String? = null
    var readPartnerParameters: String? = null
    var foregroundTimerStart = 60
    var foregroundTimerCycle = 60

    init {
        internalState = activityHandler.internalState
    }
}