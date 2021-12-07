package com.motrack.testapp

import android.content.Context
import android.util.Log
import com.motrack.test.ICommandListener
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 21th November 2021
 */

class CommandListener(context: Context) : ICommandListener {
    var motrackCommandExecutor: MotrackCommandExecutor = MotrackCommandExecutor(context)

    override fun executeCommand(
        className: String,
        methodName: String,
        parameters: Map<String, List<String>>
    ) {
        when (className) {
            "Motrack" -> motrackCommandExecutor.executeCommand(
                Command(
                    className,
                    methodName, parameters
                )
            )
            else -> debug("Could not find $className class to execute")
        }
    }

    private fun debug(message: String, vararg parameters: Any?) {
        val tag = "TestLibrary"
        try {
            Log.d(tag, String.format(Locale.US, message, *parameters))
        } catch (e: Exception) {
            Log.e(
                tag,
                "Error formatting log message: $message, with params: ${parameters.contentToString()}"
            )
        }
    }
}