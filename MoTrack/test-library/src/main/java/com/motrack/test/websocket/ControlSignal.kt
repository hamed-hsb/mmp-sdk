package com.motrack.test.websocket

import com.motrack.test.Constants

/**
 * @author yaya (@yahyalmh)
 * @since 14th November 2021
 */

class ControlSignal {
    private lateinit var type: String
    lateinit var value: String

    constructor(type: SignalType) {
        this.type = getSignalTypeString(type)
        value = "n/a"
    }

    constructor(type: SignalType, value: String) {
        this.type = getSignalTypeString(type)
        this.value = value
    }

    @JvmName("getType1")
    fun getType(): SignalType = getSignalTypeByString(type)

    private fun getSignalTypeString(signalType: SignalType): String {
        return when (signalType) {
            SignalType.INFO -> Constants.SIGNAL_INFO
            SignalType.INIT_TEST_SESSION -> Constants.SIGNAL_INIT_TEST_SESSION
            SignalType.END_WAIT -> Constants.SIGNAL_END_WAIT
            SignalType.CANCEL_CURRENT_TEST -> Constants.SIGNAL_CANCEL_CURRENT_TEST
            else -> "unknown"
        }
    }

    private fun getSignalTypeByString(signalType: String): SignalType {
        return when (signalType) {
            Constants.SIGNAL_INFO -> SignalType.INFO
            Constants.SIGNAL_INIT_TEST_SESSION -> SignalType.INIT_TEST_SESSION
            Constants.SIGNAL_END_WAIT -> SignalType.END_WAIT
            Constants.SIGNAL_CANCEL_CURRENT_TEST -> SignalType.CANCEL_CURRENT_TEST
            else -> SignalType.UNKNOWN
        }
    }
}