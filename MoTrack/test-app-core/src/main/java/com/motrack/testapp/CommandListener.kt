package com.motrack.testapp

import android.content.Context
import android.util.Log
import com.motrack.test.ICommandListener
import java.lang.Exception
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 21th November 2021
 */

class CommandListener(context: Context?): ICommandListener {
    var adjustCommandExecutor: AdjustCommandExecutor = AdjustCommandExecutor(context)

    override fun executeCommand(
        className: String,
        methodName: String,
        parameters: Map<String, List<String>>?
    ) {
        when (className) {
            "Adjust" -> adjustCommandExecutor.executeCommand(
                Command(
                    className,
                    methodName, parameters
                )
            )
            else -> debug("Could not find %s class to execute", className)
        }
    }

    fun debug(message: String?, vararg parameters: Any?) {
        try {
            Log.d("TestApp", String.format(Locale.US, message!!, *parameters))
        } catch (e: Exception) {
            Log.e(
                "TestApp", String.format(
                    Locale.US,
                    "Error formatting log message: %s, with params: %s",
                    message,
                    parameters.contentToString()
                )
            )
        }
    }
}