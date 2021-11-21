package com.motrack.testapp

/**
 * @author yaya (@yahyalmh)
 * @since 21th November 2021
 */

class Command(
    var className: String,
    var methodName: String,
    var parameters: Map<String, List<String>>?
) {

    fun getFirstParameterValue(parameterKey: String): String? {
        val parameterValues = parameters!![parameterKey]
        return if (parameterValues == null || parameterValues.isEmpty()) {
            null
        } else parameterValues[0]
    }

    fun containsParameter(parameterKey: String): Boolean {
        return parameters!![parameterKey] != null
    }
}