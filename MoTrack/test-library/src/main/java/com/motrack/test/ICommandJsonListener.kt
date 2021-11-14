package com.motrack.test

/**
 * @author yaya (@yahyalmh)
 * @since 14th November 2021
 */

interface ICommandJsonListener {
    fun executeCommand(
        className: String,
        methodName: String,
        jsonParameters: String
    )
}