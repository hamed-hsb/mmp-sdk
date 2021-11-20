package com.motrack.sdk

/**
 * @author yaya (@yahyalmh)
 * @since 20th November 2021
 */

class StateEvent {
    var bufferedSuffix: String? = null
    var backgroundTimerStarts: Int? = null
    var activityStateSuffix: String? = null
    var orderId: String? = null
    var duplicatedOrderId = false
    var disabled = false
}