package com.motrack.sdk

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.motrack.sdk.test.UnitTestActivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author yaya (@yahyalmh)
 * @since 21th November 2021
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TestSdkClickHandler {
    lateinit var mockLogger: MockLogger
    lateinit var assertUtil: AssertUtil
    lateinit var mockHttpsURLConnection: MockHttpsURLConnection
    private var activity: UnitTestActivity? = null
    private var context: Context? = null
    private var sdkClickPackage: ActivityPackage? = null
    lateinit var mockActivityHandler: MockActivityHandler

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        mockLogger = MockLogger()
        mockHttpsURLConnection = MockHttpsURLConnection(null, mockLogger)
        mockActivityHandler = MockActivityHandler(mockLogger)
        assertUtil = AssertUtil(mockLogger)
        MotrackFactory.setLogger(mockLogger)
        MotrackFactory.httpsURLConnectionProvider = mockHttpsURLConnection
        sdkClickPackage = getClickPackage()
    }

    @After
    fun tearDown() {
        MotrackFactory.httpsURLConnectionProvider= null
        MotrackFactory.setLogger(null)
    }

    private fun getClickPackage(): ActivityPackage {
        val mockSdkClickHandler = MockSdkClickHandler(mockLogger)
        val mockAttributionHandler = MockAttributionHandler(mockLogger)
        val mockPackageHandler = MockPackageHandler(mockLogger)
        MotrackFactory.setPackageHandler(mockPackageHandler)
        MotrackFactory.setSdkClickHandler(mockSdkClickHandler)
        MotrackFactory.setAttributionHandler(mockAttributionHandler)

        // create the config to start the session
        val config = MotrackConfig(context, "123456789012", MotrackConfig.ENVIRONMENT_SANDBOX)

        // start activity handler with config
        val activityHandler = ActivityHandler.getInstance(config)
        activityHandler!!.onResume()
        activityHandler.readOpenUrl(Uri.parse("AdjustTests://"), System.currentTimeMillis())
        SystemClock.sleep(2000)
        val sdkClickPackage = mockSdkClickHandler.queue[0]
        mockLogger.reset()
        return sdkClickPackage
    }

    @Test
    fun testPaused() {
        sdkClickPackage!!.clientSdk = "Test-First-Click"
        val secondSdkClickPackage = getClickPackage()
        secondSdkClickPackage.clientSdk = "Test-Second-Click"
        MotrackFactory.sdkClickBackoffStrategy = BackoffStrategy.NO_WAIT

        // assert test name to read better in logcat
        mockLogger.assert("TestRequestHandler testPaused")
        val sdkClickHandler = SdkClickHandler(mockActivityHandler, false, null)
        mockHttpsURLConnection.responseType = ResponseType.CLIENT_PROTOCOL_EXCEPTION
        sdkClickHandler.sendSdkClick(sdkClickPackage!!)
        SystemClock.sleep(1000)

        // added first click package to the queue
        assertUtil.debug("Added sdk_click 1")
        assertUtil.verbose(
            """
            Path:      /sdk_click
            ClientSdk: Test-First-Click
            """.trimIndent()
        )

        // but not send because it's paused
        assertUtil.notInTest("MockHttpsURLConnection getInputStream")

        // send second sdk click
        sdkClickHandler.sendSdkClick(secondSdkClickPackage)
        SystemClock.sleep(1000)

        // added second click package to the queue
        assertUtil.debug("Added sdk_click 2")
        assertUtil.verbose(
            """
            Path:      /sdk_click
            ClientSdk: Test-Second-Click
            """.trimIndent()
        )

        // wait two seconds before sending
        mockHttpsURLConnection.waitingTime = 2000L

        // try to send first package
        checkSendFirstPackage(sdkClickHandler, 1)
        // and then the second
        checkSendSecondPackage(sdkClickHandler, 1)

        // try to send first package again
        checkSendFirstPackage(sdkClickHandler, 2)
        // and then the second again
        checkSendSecondPackage(sdkClickHandler, 2)
    }

    private fun checkSendFirstPackage(sdkClickHandler: SdkClickHandler, retries: Int) {
        // try to send the first package
        sdkClickHandler.resumeSending()
        SystemClock.sleep(1000)

        // prevent sending next again
        sdkClickHandler.pauseSending()
        SystemClock.sleep(2000)

        // check that it tried to send the first package
        assertUtil.test("MockHttpsURLConnection setRequestProperty, field Client-SDK, newValue Test-First-Click")

        // and that it will try to send it again
        assertUtil.error("Retrying sdk_click package for the $retries time")

        // first package added again on the end of the queue
        assertUtil.debug("Added sdk_click 2")
        assertUtil.verbose(
            """
            Path:      /sdk_click
            ClientSdk: Test-First-Click
            """.trimIndent()
        )

        // does not continue to send because it was paused
        assertUtil.notInTest("MockHttpsURLConnection setRequestProperty")
    }

    private fun checkSendSecondPackage(sdkClickHandler: SdkClickHandler, retries: Int) {
        // try to send the second package that is at the start of the queue
        sdkClickHandler.resumeSending()
        SystemClock.sleep(1000)

        // prevent sending next again
        sdkClickHandler.pauseSending()
        SystemClock.sleep(2000)

        // check that it tried to send the second package
        assertUtil.test("MockHttpsURLConnection setRequestProperty, field Client-SDK, newValue Test-Second-Click")

        // and that it will try to send it again
        assertUtil.error("Retrying sdk_click package for the $retries time")

        // second package added again on the end of the queue
        assertUtil.debug("Added sdk_click 2")
        assertUtil.verbose(
            """
            Path:      /sdk_click
            ClientSdk: Test-Second-Click
            """.trimIndent()
        )

        // does not continue to send because it was paused
        assertUtil.notInTest("MockHttpsURLConnection setRequestProperty")
    }

    @Test
    fun testNullResponse() {
        // assert test name to read better in logcat
        mockLogger.assert("TestRequestHandler testNullResponse")
        MotrackFactory.sdkClickBackoffStrategy = BackoffStrategy.NO_WAIT

        val sdkClickHandler = SdkClickHandler(mockActivityHandler, true)
        mockHttpsURLConnection.responseType = null
        sdkClickHandler.sendSdkClick(sdkClickPackage!!)
        SystemClock.sleep(1000)
        assertUtil.debug("Added sdk_click 1")
        assertUtil.test("MockHttpsURLConnection getInputStream, responseType: null")
        assertUtil.error("Failed to read response. (null)")
        assertUtil.error("Failed to track click. (Sdk_click runtime exception: java.lang.NullPointerException)")

        // does not to try to retry
        assertUtil.notInError("Retrying sdk_click package for the")
        assertUtil.notInDebug("Added sdk_click")
    }

    @Test
    fun testClientException() {
        // assert test name to read better in logcat
        mockLogger.assert("TestRequestHandler testClientException")
        MotrackFactory.sdkClickBackoffStrategy = BackoffStrategy.NO_WAIT

        val sdkClickHandler = SdkClickHandler(mockActivityHandler, true)
        mockHttpsURLConnection.responseType = ResponseType.CLIENT_PROTOCOL_EXCEPTION
        sdkClickHandler.sendSdkClick(sdkClickPackage!!)
        SystemClock.sleep(1000)
        assertUtil.test("MockHttpsURLConnection getInputStream, responseType: CLIENT_PROTOCOL_EXCEPTION")
        assertUtil.error("Failed to track click. (Sdk_click request failed. Will retry later: java.io.IOException: testResponseError)")

        // tries to retry
        assertUtil.error("Retrying sdk_click package for the 1 time")

        // adds to end of the queue
        assertUtil.debug("Added sdk_click")
    }

    @Test
    fun testServerError() {
        // assert test name to read better in logcat
        mockLogger.assert("TestRequestHandler testServerError")
        MotrackFactory.sdkClickBackoffStrategy = BackoffStrategy.NO_WAIT

        val sdkClickHandler = SdkClickHandler(mockActivityHandler, true)
        mockHttpsURLConnection.responseType = ResponseType.INTERNAL_SERVER_ERROR
        sdkClickHandler.sendSdkClick(sdkClickPackage!!)
        SystemClock.sleep(1000)
        assertUtil.test("MockHttpsURLConnection getErrorStream, responseType: INTERNAL_SERVER_ERROR")
        assertUtil.verbose("Response: { \"message\": \"testResponseError\"}")
        assertUtil.error("testResponseError")
    }

    @Test
    fun testWrongJson() {
        // assert test name to read better in logcat
        mockLogger.assert("TestRequestHandler testWrongJson")
        MotrackFactory.sdkClickBackoffStrategy = BackoffStrategy.NO_WAIT

        val sdkClickHandler = SdkClickHandler(mockActivityHandler, true)
        mockHttpsURLConnection.responseType = ResponseType.WRONG_JSON
        sdkClickHandler.sendSdkClick(sdkClickPackage!!)
        SystemClock.sleep(1000)
        assertUtil.test("MockHttpsURLConnection getInputStream, responseType: WRONG_JSON")
        assertUtil.verbose("Response: not a json response")
        assertUtil.error("Failed to parse json response. (Value not of type java.lang.String cannot be converted to JSONObject)")
    }

    @Test
    fun testEmptyJson() {
        // assert test name to read better in logcat
        mockLogger.assert("TestRequestHandler testWrongJson")
        MotrackFactory.sdkClickBackoffStrategy = BackoffStrategy.NO_WAIT
        val sdkClickHandler = SdkClickHandler(mockActivityHandler, true)
        mockHttpsURLConnection.responseType = ResponseType.EMPTY_JSON
        sdkClickHandler.sendSdkClick(sdkClickPackage!!)
        SystemClock.sleep(1000)
        assertUtil.test("MockHttpsURLConnection getInputStream, responseType: EMPTY_JSON")
        assertUtil.verbose("Response: { }")
        assertUtil.info("No message found")
    }

    @Test
    fun testMessage() {
        // assert test name to read better in logcat
        mockLogger.assert("TestRequestHandler testWrongJson")
        MotrackFactory.sdkClickBackoffStrategy = BackoffStrategy.NO_WAIT
        val sdkClickHandler = SdkClickHandler(mockActivityHandler, true)
        mockHttpsURLConnection.responseType = ResponseType.MESSAGE
        sdkClickHandler.sendSdkClick(sdkClickPackage!!)
        SystemClock.sleep(1000)
        TestActivityPackage.testQueryStringRequest(mockHttpsURLConnection.readRequest(), null)
        assertUtil.test("MockHttpsURLConnection getInputStream, responseType: MESSAGE")
        assertUtil.verbose("Response: { \"message\" : \"response OK\"}")
        assertUtil.info("response OK")

        // sends response to activity handler to be checked
        assertUtil.test("ActivityHandler finishedTrackingActivity, message:response OK timestamp:null json:{\"message\":\"response OK\"}")
    }


}