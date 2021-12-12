package com.motrack.sdk

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

enum class TrackingState(private var value: Int) {
    OPTED_OUT(1);

    @JvmName("getStateValue")
    public fun getValue(): Int {
        return value
    }
}