package com.motrack.testapp

import androidx.test.platform.app.InstrumentationRegistry
import com.motrack.test.Util.Companion.debug
import java.io.IOException
import java.io.InputStreamReader

object FileReader {
    fun readStringFromFile(fileName: String): String {
        try {
            debug("$fileName :", "Read json response from assets")
            val inputStream = (InstrumentationRegistry.getInstrumentation().targetContext
                .applicationContext as TestApplicationLoader).assets.open(fileName)
            val builder = StringBuilder()
            val reader = InputStreamReader(inputStream, "UTF-8")
            reader.readLines().forEach {
                builder.append(it)
            }
            return builder.toString()
        } catch (e: IOException) {
            throw e
        }
    }
}