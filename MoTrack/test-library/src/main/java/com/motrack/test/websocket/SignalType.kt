package com.motrack.test.websocket

/**
 * @author yaya (@yahyalmh)
 * @since 14th November 2021
 */

enum class SignalType {
    INFO,
    INIT_TEST_SESSION,
    END_WAIT,
    CANCEL_CURRENT_TEST,
    UNKNOWN
}