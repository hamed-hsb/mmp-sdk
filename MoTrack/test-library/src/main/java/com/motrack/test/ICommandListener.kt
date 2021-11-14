package com.motrack.test

/**
 * @author yaya (@yahyalmh)
 * @since 14th November 2021
 */

interface ICommandListener {
    fun executeCommand(
        className: String,
        methodName: String,
        parameters: Map<String, List<String>>?
    )
}