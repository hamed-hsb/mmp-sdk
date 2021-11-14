package com.motrack.test

/**
 * @author yaya (@yahyalmh)
 * @since 14th November 2021
 */

interface Constants {
    companion object {
        var ONE_SECOND = 1000
        var ONE_MINUTE = 60 * ONE_SECOND
        var ENCODING = "UTF-8"

        var LOGTAG = "TestLibrary"
        var TEST_LIBRARY_CLASSNAME = "TestLibrary"
        var WAIT_FOR_CONTROL = "control"
        var WAIT_FOR_SLEEP = "sleep"
        var TEST_SESSION_ID_HEADER = "Test-Session-Id"

        // web socket values
        var SIGNAL_INFO = "info"
        var SIGNAL_INIT_TEST_SESSION = "init-test-session"
        var SIGNAL_END_WAIT = "end-wait"
        var SIGNAL_CANCEL_CURRENT_TEST = "cancel-current-test"
    }
}