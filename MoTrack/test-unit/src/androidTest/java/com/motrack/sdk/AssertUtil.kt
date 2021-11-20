package com.motrack.sdk

import android.net.Uri
import java.util.*
import com.motrack.sdk.MockLogger.ContainsReturn

import junit.framework.Assert

/**
 * @author yaya (@yahyalmh)
 * @since 20th November 2021
 */

class AssertUtil(private val mockLogger: MockLogger) {
    fun test(message: String, vararg parameters: Any?): String? {
        val containsTestMessage: ContainsReturn = mockLogger.containsTestMessage(
            String.format(
                Locale.US,
                message, *parameters
            )
        )
        Assert.assertTrue(
            mockLogger.toString(),
            containsTestMessage.containsMessage
        )
        return containsTestMessage.matchMessage
    }

    fun verbose(message: String, vararg parameters: Any?): String? {
        val containsVerboseMessage: ContainsReturn = mockLogger.containsMessage(
            LogLevel.VERBOSE, String.format(
                Locale.US,
                message, *parameters
            )
        )
        Assert.assertTrue(
            mockLogger.toString(),
            containsVerboseMessage.containsMessage
        )
        return containsVerboseMessage.matchMessage
    }

    fun debug(message: String, vararg parameters: Any?): String? {
        val containsDebugMessage: ContainsReturn = mockLogger.containsMessage(
            LogLevel.DEBUG, String.format(
                Locale.US,
                message, *parameters
            )
        )
        Assert.assertTrue(
            mockLogger.toString(),
            containsDebugMessage.containsMessage
        )
        return containsDebugMessage.matchMessage
    }

    fun info(message: String, vararg parameters: Any?): String? {
        val containsInfoMessage: ContainsReturn = mockLogger.containsMessage(
            LogLevel.INFO, String.format(
                Locale.US,
                message, *parameters
            )
        )
        Assert.assertTrue(
            mockLogger.toString(),
            containsInfoMessage.containsMessage
        )
        return containsInfoMessage.matchMessage
    }

    fun warn(message: String, vararg parameters: Any?): String? {
        val containsWarnMessage: ContainsReturn = mockLogger.containsMessage(
            LogLevel.WARN, String.format(
                Locale.US,
                message, *parameters
            )
        )
        Assert.assertTrue(
            mockLogger.toString(),
            containsWarnMessage.containsMessage
        )
        return containsWarnMessage.matchMessage
    }

    fun error(message: String, vararg parameters: Any?): String? {
        val containsErrorMessage: ContainsReturn = mockLogger.containsMessage(
            LogLevel.ERROR, String.format(
                Locale.US,
                message, *parameters
            )
        )
        Assert.assertTrue(
            mockLogger.toString(),
            containsErrorMessage.containsMessage
        )
        return containsErrorMessage.matchMessage
    }

    fun Assert(message: String, vararg parameters: Any?): String? {
        val containsAssertMessage: ContainsReturn = mockLogger.containsMessage(
            LogLevel.ASSERT, String.format(
                Locale.US,
                message, *parameters
            )
        )
        Assert.assertTrue(
            mockLogger.toString(),
            containsAssertMessage.containsMessage
        )
        return containsAssertMessage.matchMessage
    }

    fun notInTest(message: String, vararg parameters: Any?) {
        val containsTestMessage: ContainsReturn = mockLogger.containsTestMessage(
            String.format(
                Locale.US,
                message, *parameters
            )
        )
        Assert.assertFalse(
            mockLogger.toString(),
            containsTestMessage.containsMessage
        )
    }

    fun notInVerbose(message: String, vararg parameters: Any?) {
        val containsVerboseMessage: ContainsReturn = mockLogger.containsMessage(
            LogLevel.VERBOSE, String.format(
                Locale.US,
                message, *parameters
            )
        )
        Assert.assertFalse(
            mockLogger.toString(),
            containsVerboseMessage.containsMessage
        )
    }

    fun notInDebug(message: String, vararg parameters: Any?) {
        val containsDebugMessage: ContainsReturn = mockLogger.containsMessage(
            LogLevel.DEBUG, String.format(
                Locale.US,
                message, *parameters
            )
        )
        Assert.assertFalse(
            mockLogger.toString(),
            containsDebugMessage.containsMessage
        )
    }

    fun notInInfo(message: String, vararg parameters: Any?) {
        val containsInfoMessage: ContainsReturn = mockLogger.containsMessage(
            LogLevel.INFO, String.format(
                Locale.US,
                message, *parameters
            )
        )
        Assert.assertFalse(
            mockLogger.toString(),
            containsInfoMessage.containsMessage
        )
    }

    fun notInWarn(message: String?, vararg parameters: Any?) {
        val containsWarnMessage = mockLogger.containsMessage(
            LogLevel.WARN, String.format(
                Locale.US,
                message!!, *parameters
            )
        )
        Assert.assertFalse(
            mockLogger.toString(),
            containsWarnMessage.containsMessage
        )
    }

    fun notInError(message: String?, vararg parameters: Any?) {
        val containsErrorMessage = mockLogger.containsMessage(
            LogLevel.ERROR, String.format(
                Locale.US,
                message!!, *parameters
            )
        )
        Assert.assertFalse(
            mockLogger.toString(),
            containsErrorMessage.containsMessage
        )
    }

    fun notInAssert(message: String?, vararg parameters: Any?) {
        val containsAssertMessage = mockLogger.containsMessage(
            LogLevel.ASSERT, String.format(
                Locale.US,
                message!!, *parameters
            )
        )
        Assert.assertFalse(
            mockLogger.toString(),
            containsAssertMessage.containsMessage
        )
    }

    fun isNull(obj: Any?) {
        Assert.assertNull(mockLogger.toString(), obj)
    }

    fun isNotNull(`object`: Any?) {
        Assert.assertNotNull(mockLogger.toString(), `object`)
    }

    fun isTrue(value: Boolean) {
        Assert.assertTrue(mockLogger.toString(), value)
    }

    fun isFalse(value: Boolean) {
        Assert.assertFalse(mockLogger.toString(), value)
    }

    fun isEqual(expected: String?, actual: String?) {
        Assert.assertEquals(mockLogger.toString(), expected, actual)
    }

    fun isEqual(expected: Boolean, actual: Boolean) {
        Assert.assertEquals(mockLogger.toString(), expected, actual)
    }

    fun isEqual(expected: Int, actual: Int) {
        Assert.assertEquals(mockLogger.toString(), expected, actual)
    }

    fun isEqual(expected: Uri?, actual: Uri?) {
        Assert.assertEquals(mockLogger.toString(), expected, actual)
    }
    fun fail() {
        Assert.fail(mockLogger.toString())
    }

    fun fail(extraMessage: String) {
        Assert.fail(
            """
            $extraMessage
            $mockLogger
            """.trimIndent()
        )
    }

}