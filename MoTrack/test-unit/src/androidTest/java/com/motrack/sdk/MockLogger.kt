package com.motrack.sdk

import android.util.Log
import android.util.SparseArray
import java.util.*

class MockLogger : ILogger {
    private lateinit var logBuffer: StringBuffer
    private var logMap: SparseArray<ArrayList<String>>? = null
    private var startTime: Long = 0
    private var lastTime: Long = 0

    companion object {
        const val TEST_LEVEL = 6
        const val CHECK_LEVEL = 7
    }

    init {
        reset()
    }

    class ContainsReturn internal constructor(
        var containsMessage: Boolean,
        var matchMessage: String?
    )

    fun reset() {
        startTime = System.currentTimeMillis()
        lastTime = startTime
        logBuffer = StringBuffer()
        logMap = SparseArray(8)
        logMap?.let {
            it.put(LogLevel.VERBOSE.getAndroidLogLevel(), ArrayList())
            it.put(LogLevel.DEBUG.getAndroidLogLevel(), ArrayList())
            it.put(LogLevel.INFO.getAndroidLogLevel(), ArrayList())
            it.put(LogLevel.WARN.getAndroidLogLevel(), ArrayList())
            it.put(LogLevel.ERROR.getAndroidLogLevel(), ArrayList())
            it.put(LogLevel.ASSERT.getAndroidLogLevel(), ArrayList())
            it.put(TEST_LEVEL, ArrayList())
            it.put(CHECK_LEVEL, ArrayList())
        }
        test("Logger reset")
    }

    fun test(message: String) {
        logMessage(message, TEST_LEVEL, "t", Log.VERBOSE)
    }

    private fun logMessage(message: String, iLoglevel: Int, messagePrefix: String, priority: Int) {
        val now = System.currentTimeMillis()
        val milliSecondsPassed = now - startTime
        val lastTimePassed = now - lastTime
        lastTime = now
        val prefixedList: MutableList<String> = logMap!![iLoglevel]
        prefixedList.add(message)
        val longMessage =
            String.format("$milliSecondsPassed $lastTimePassed $messagePrefix $message")
        Log.println(priority, Constants.LOGTAG, longMessage)
        val logBufferMessage =
            String.format("$longMessage${System.getProperty("line.separator")}")
        logBuffer.append(logBufferMessage)
        //String sList = Arrays.toString(prefixedList.toArray());
        //String logBufferList = String.format("In %s", sList);
        //logBuffer.append(logBufferList);
    }

    override fun setLogLevel(logLevel: LogLevel, isProductionEnvironment: Boolean) {
        test("MockLogger setLogLevel: $logLevel, isProductionEnvironment: $isProductionEnvironment")
    }

    override fun setLogLevelString(logLevelString: String?, isProductionEnvironment: Boolean) {
        test("MockLogger setLogLevelString: $logLevelString, isProductionEnvironment: $isProductionEnvironment")
    }


    override fun verbose(message: String, vararg parameters: Any) {
        logMessage(
            String.format(Locale.US, message, *parameters),
            LogLevel.VERBOSE.getAndroidLogLevel(),
            "v",
            Log.VERBOSE
        )
    }

    override fun debug(message: String, vararg parameters: Any) {
        logMessage(
            String.format(Locale.US, message, *parameters),
            LogLevel.DEBUG.getAndroidLogLevel(),
            "d",
            Log.DEBUG
        )
    }

    override fun info(message: String, vararg parameters: Any) {
        logMessage(
            String.format(Locale.US, message, *parameters),
            LogLevel.INFO.getAndroidLogLevel(),
            "i",
            Log.INFO
        )
    }

    override fun warn(message: String, vararg parameters: Any) {
        logMessage(
            String.format(Locale.US, message, *parameters),
            LogLevel.WARN.getAndroidLogLevel(),
            "w",
            Log.WARN
        )
    }

    override fun warnInProduction(message: String, vararg parameters: Any) {
        logMessage(
            String.format(Locale.US, message, *parameters),
            LogLevel.WARN.getAndroidLogLevel(),
            "w",
            Log.WARN
        )
    }

    override fun error(message: String, vararg parameters: Any) {
        logMessage(
            String.format(Locale.US, message, *parameters),
            LogLevel.ERROR.getAndroidLogLevel(),
            "e",
            Log.ERROR
        )
    }

    override fun assert(message: String, vararg parameters: Any) {
        logMessage(
            String.format(Locale.US, message, *parameters),
            LogLevel.ASSERT.getAndroidLogLevel(),
            "a",
            Log.ASSERT
        )
    }

    override fun lockLogLevel() {
        test("MockLogger lockLogLevel")
    }

    fun containsMessage(level: LogLevel, beginsWith: String): ContainsReturn {
        return mapContainsMessage(level.getAndroidLogLevel(), beginsWith)
    }

    fun containsTestMessage(beginsWith: String): ContainsReturn {
        return mapContainsMessage(TEST_LEVEL, beginsWith)
    }

    private fun mapContainsMessage(level: Int, beginsWith: String): ContainsReturn {
        val list = logMap!![level]
        val listCopy = ArrayList(list)
        val sList = list.joinToString()
        for (log in list) {
            listCopy.removeAt(0)
            if (log.startsWith(beginsWith)) {
                val foundMessage = "$log found"
                check(foundMessage)
                logMap!!.put(level, listCopy)
                return ContainsReturn(true, log)
            }
        }
        val notFoundMessage = "$beginsWith is not in $sList"
        check(notFoundMessage)
        return ContainsReturn(false, null)
    }

    private fun check(message: String) {
        logMessage(message, CHECK_LEVEL, "c", Log.VERBOSE)
    }

    fun printLogMap(level: Int) {
        val list = logMap!![level]
        val sList = list.joinToString()
        val message = "list level $level: $sList"
        check(message)
    }

    override fun toString(): String {
        return logBuffer.toString()
    }

}