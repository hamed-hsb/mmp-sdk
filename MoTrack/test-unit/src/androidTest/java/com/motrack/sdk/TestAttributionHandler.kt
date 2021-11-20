package com.motrack.sdk

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.motrack.sdk.test.UnitTestActivity
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author yaya (@yahyalmh)
 * @since 20th November 2021
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TestAttributionHandler {

    lateinit var mockLogger: MockLogger
    lateinit var mockActivityHandler: MockActivityHandler
    lateinit var mockHttpsURLConnection: MockHttpsURLConnection
    lateinit var assertUtil: AssertUtil
    private var activity: UnitTestActivity? = null
    private var context: Context? = null
    private var attributionPackage: ActivityPackage? = null
    private var firstSessionPackage: ActivityPackage? = null
    private var sdkClickPackage: ActivityPackage? = null

    @Rule
    public val mActivityRule: ActivityScenarioRule<UnitTestActivity> =
        ActivityScenarioRule(UnitTestActivity::class.java)

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        mockLogger = MockLogger()
        mockActivityHandler = MockActivityHandler(mockLogger)
        mockHttpsURLConnection = MockHttpsURLConnection(null, mockLogger)
        assertUtil = AssertUtil(mockLogger)
        MotrackFactory.setLogger(mockLogger)
        MotrackFactory.setActivityHandler(mockActivityHandler)
        MotrackFactory.httpsURLConnectionProvider = mockHttpsURLConnection
        savePackages()
        mockLogger.reset()
    }

    private fun savePackages() {
        val mockAttributionHandler = MockAttributionHandler(mockLogger)
        val mockPackageHandler = MockPackageHandler(mockLogger)
        val mockSdkClickHandler = MockSdkClickHandler(mockLogger)
        MotrackFactory.setAttributionHandler(mockAttributionHandler)
        MotrackFactory.setPackageHandler(mockPackageHandler)
        MotrackFactory.setSdkClickHandler(mockSdkClickHandler)

        // deleting the activity state file to simulate a first session
        val activityStateDeleted = ActivityHandler.deleteActivityState(context!!)
        val attributionDeleted = ActivityHandler.deleteAttribution(context!!)

        // create the config to start the session
        val config = MotrackConfig(context, "123456789012", MotrackConfig.ENVIRONMENT_SANDBOX)

        // start activity handler with config
        val activityHandler = ActivityHandler.getInstance(config)!!
        activityHandler.onResume()
        val attributions =
            Uri.parse("Tests://example.com/path/inApp?adjust_tracker=trackerValue&other=stuff&adjust_campaign=campaignValue&adjust_adgroup=adgroupValue&adjust_creative=creativeValue")
        val now = System.currentTimeMillis()
        activityHandler.readOpenUrl(attributions, now)
        SystemClock.sleep(3000)
        val attributionPackage: ActivityPackage = activityHandler.getAttribution()
        val attributionPackageTest = TestActivityPackage(attributionPackage)
        attributionPackageTest.testAttributionPackage()
        firstSessionPackage = mockPackageHandler.queue[0]
        sdkClickPackage = mockSdkClickHandler.queue[0]
        this.attributionPackage = attributionPackage
        MotrackFactory.setAttributionHandler(null)
        MotrackFactory.setPackageHandler(null)
        MotrackFactory.setSdkClickHandler(null)
    }

    @After
    fun tearDown() {
        MotrackFactory.httpsURLConnectionProvider = null
        MotrackFactory.setActivityHandler(null)
        MotrackFactory.setLogger(null)
        activity = null
        context = null
    }

    @Test
    fun testGetAttribution() {
        // assert test name to read better in logcat
        mockLogger.assert("TestAttributionHandler testGetAttribution")
        val attributionHandler = AttributionHandler(
            mockActivityHandler,
             true, attributionPackage
        )

        // test null client
        nullClientTest(attributionHandler)

        // test client exception
        clientExceptionTest(attributionHandler)

        // test wrong json response
        wrongJsonTest(attributionHandler)

        // test empty response
        emptyJsonResponseTest(attributionHandler)

        // test server error
        serverErrorTest(attributionHandler)

        // test ok response with message
        okMessageTest(attributionHandler)
    }

    @Test
    fun testCheckSessionResponse() {
        // assert test name to read better in logcat
        mockLogger.assert("TestAttributionHandler testCheckSessionResponse")
        val attributionHandler = AttributionHandler(
            mockActivityHandler,true,
            attributionPackage
        )

        // new attribution
        var attributionJson: JSONObject? = null
        try {
            attributionJson = JSONObject(
                "{ " +
                        "\"tracker_token\" : \"ttValue\" , " +
                        "\"tracker_name\"  : \"tnValue\" , " +
                        "\"network\"       : \"nValue\" , " +
                        "\"campaign\"      : \"cpValue\" , " +
                        "\"adgroup\"       : \"aValue\" , " +
                        "\"creative\"      : \"ctValue\" , " +
                        "\"click_label\"   : \"clValue\" }"
            )
        } catch (e: JSONException) {
            assertUtil.fail(e.message!!)
        }
        val sessionResponseData =
            ResponseData.buildResponseData(firstSessionPackage!!, null) as SessionResponseData
        sessionResponseData.jsonResponse = attributionJson
        attributionHandler.checkSessionResponse(sessionResponseData)
        SystemClock.sleep(1000)

        // updated set askingAttribution to false
        assertUtil.test("ActivityHandler setAskingAttribution, false")

        // it did not update to true
        assertUtil.notInTest("ActivityHandler setAskingAttribution, true")

        // and waiting for query
        assertUtil.notInDebug("Waiting to query attribution")

        // check attribution was called without ask_in
        assertUtil.test("ActivityHandler launchSessionResponseTasks, message:null timestamp:null json:{\"tracker_token\":\"ttValue\",\"tracker_name\":\"tnValue\",\"network\":\"nValue\",\"campaign\":\"cpValue\",\"adgroup\":\"aValue\",\"creative\":\"ctValue\",\"click_label\":\"clValue\"}")
    }

    @Test
    fun testCheckSdkClickResponse() {
        // assert test name to read better in logcat
        mockLogger.assert("TestAttributionHandler testCheckSdkClickResponse")
        val attributionHandler = AttributionHandler(
            mockActivityHandler,
            true,
            attributionPackage,
        )

        // new attribution
        var attributionJson: JSONObject? = null
        try {
            attributionJson = JSONObject(
                "{ " +
                        "\"tracker_token\" : \"ttValue\" , " +
                        "\"tracker_name\"  : \"tnValue\" , " +
                        "\"network\"       : \"nValue\" , " +
                        "\"campaign\"      : \"cpValue\" , " +
                        "\"adgroup\"       : \"aValue\" , " +
                        "\"creative\"      : \"ctValue\" , " +
                        "\"click_label\"   : \"clValue\" }"
            )
        } catch (e: JSONException) {
            assertUtil.fail(e.message!!)
        }
        val sdkClickResponseData =
            ResponseData.buildResponseData(sdkClickPackage!!, null) as SdkClickResponseData
        sdkClickResponseData.jsonResponse = attributionJson
        attributionHandler.checkSdkClickResponse(sdkClickResponseData)
        SystemClock.sleep(1000)

        // updated set askingAttribution to false
        assertUtil.test("ActivityHandler setAskingAttribution, false")

        // it did not update to true
        assertUtil.notInTest("ActivityHandler setAskingAttribution, true")

        // and waiting for query
        assertUtil.notInDebug("Waiting to query attribution")

        // check attribution was called without ask_in
        assertUtil.test("ActivityHandler launchSdkClickResponseTasks, message:null timestamp:null json:{\"tracker_token\":\"ttValue\",\"tracker_name\":\"tnValue\",\"network\":\"nValue\",\"campaign\":\"cpValue\",\"adgroup\":\"aValue\",\"creative\":\"ctValue\",\"click_label\":\"clValue\"}")
    }

    @Test
    fun testAskIn() {
        // assert test name to read better in logcat
        mockLogger.assert("TestAttributionHandler testAskIn")
        val attributionHandler = AttributionHandler(
            mockActivityHandler,
            true,
            attributionPackage
        )
        val response = "Response: { \"ask_in\" : 4000 }"
        var askIn4sJson: JSONObject? = null
        try {
            askIn4sJson = JSONObject("{ \"ask_in\" : 4000 }")
        } catch (e: JSONException) {
            assertUtil.fail(e.message!!)
        }
        mockHttpsURLConnection.responseType = ResponseType.MESSAGE
        val sessionResponseData =
            ResponseData.buildResponseData(firstSessionPackage!!, null) as SessionResponseData
        sessionResponseData.jsonResponse = askIn4sJson
        attributionHandler.checkSessionResponse(sessionResponseData)

        // sleep enough not to trigger the timer
        SystemClock.sleep(1000)

        // change the response to avoid a cycle;
        mockHttpsURLConnection.responseType = ResponseType.MESSAGE

        // check attribution was called with ask_in
        assertUtil.notInTest("ActivityHandler updateAttribution")

        // it did update to true
        assertUtil.test("ActivityHandler setAskingAttribution, true")

        // and waited to for query
        assertUtil.debug("Waiting to query attribution in 4.0 seconds")
        SystemClock.sleep(2000)
        var askIn5sJson: JSONObject? = null
        try {
            askIn5sJson = JSONObject("{ \"ask_in\" : 5000 }")
        } catch (e: JSONException) {
            assertUtil.fail(e.message!!)
        }
        sessionResponseData.jsonResponse = askIn5sJson
        attributionHandler.checkSessionResponse(sessionResponseData)

        // sleep enough not to trigger the old timer
        SystemClock.sleep(3000)

        // it did update to true
        assertUtil.test("ActivityHandler setAskingAttribution, true")

        // and waited to for query
        assertUtil.debug("Waiting to query attribution in 5.0 seconds")

        // it was been waiting for 1000 + 2000 + 3000 = 6 seconds
        // check that the mock http client was not called because the original clock was reseted
        assertUtil.notInTest("HttpClient execute")

        // check that it was finally called after 6 seconds after the second ask_in
        SystemClock.sleep(4000)
        okMessageTestLogs(attributionHandler)

        //requestTest(mockHttpClient.lastRequest);
    }

    @Test
    fun testPause() {
        // assert test name to read better in logcat
        mockLogger.assert("TestAttributionHandler testPause")
        val attributionHandler = AttributionHandler(
            mockActivityHandler,
            false,
            attributionPackage
        )
        mockHttpsURLConnection.responseType = ResponseType.MESSAGE
        attributionHandler.getAttribution()
        SystemClock.sleep(1000)

        // check that the activity handler is paused
        assertUtil.debug("Attribution handler is paused")

        // and it did not call the http client
        //assertUtil.isNull(mockHttpClient.lastRequest);
        assertUtil.notInTest("MockHttpsURLConnection getInputStream")
    }

    @Test
    fun testDeeplink() {
        // assert test name to read better in logcat
        mockLogger.assert("TestAttributionHandler testDeeplink")
        val attributionHandler = AttributionHandler(
            mockActivityHandler,
            true,
            attributionPackage,
        )
        val responseJson = JSONObject()
        val sessionResponseDeeplink =
            ResponseData.buildResponseData(firstSessionPackage!!, null) as SessionResponseData
        try {
            val internalAttributionJson = JSONObject()
            internalAttributionJson.put("deeplink", "testDeeplinkAttribution://")
            responseJson.put("deeplink", "testDeeplinkRoot://")
            responseJson.put("attribution", internalAttributionJson)

            //sessionResponseDeeplink.jsonResponse = new JSONObject("{ " +
            //        "\"deeplink\" :  \"testDeeplinkRoot://\" }");
        } catch (e: JSONException) {
            assertUtil.fail(e.message!!)
        }
        sessionResponseDeeplink.jsonResponse = responseJson
        attributionHandler.checkSessionResponse(sessionResponseDeeplink)
        SystemClock.sleep(2000)
        assertUtil.test("ActivityHandler setAskingAttribution, false")
        assertUtil.test(
            "ActivityHandler launchSessionResponseTasks, message:null timestamp:null " +
                    "json:{\"deeplink\":\"testDeeplinkRoot:\\/\\/\",\"attribution\":{\"deeplink\":\"testDeeplinkAttribution:\\/\\/\"}}"
        )
        val attributionResponseDeeplink =
            ResponseData.buildResponseData(attributionPackage!!, null) as AttributionResponseData
        attributionResponseDeeplink.jsonResponse = responseJson
        attributionHandler.checkAttributionResponse(attributionResponseDeeplink)
        SystemClock.sleep(2000)
        assertUtil.test("ActivityHandler setAskingAttribution, false")
        assertUtil.test(
            "ActivityHandler launchAttributionResponseTasks, message:null timestamp:null " +
                    "json:{\"deeplink\":\"testDeeplinkRoot:\\/\\/\",\"attribution\":{\"deeplink\":\"testDeeplinkAttribution:\\/\\/\"}}"
        )
        assertUtil.isEqual(
            attributionResponseDeeplink.deeplink,
            Uri.parse("testDeeplinkAttribution://")
        )
    }

    private fun nullClientTest(attributionHandler: AttributionHandler) {
        startGetAttributionTest(attributionHandler, null)

        // check response was not logged
        assertUtil.notInVerbose("Response")
    }

    private fun clientExceptionTest(attributionHandler: AttributionHandler) {
        startGetAttributionTest(attributionHandler, ResponseType.CLIENT_PROTOCOL_EXCEPTION)

        // check the client error
        assertUtil.error("Failed to get attribution (testResponseError)")
    }

    private fun wrongJsonTest(attributionHandler: AttributionHandler) {
        startGetAttributionTest(attributionHandler, ResponseType.WRONG_JSON)

        // check that the mock http client was called
        assertUtil.test("MockHttpsURLConnection getInputStream")
        assertUtil.verbose("Response: not a json response")
        assertUtil.error("Failed to parse json response. (Value not of type java.lang.String cannot be converted to JSONObject)")
    }

    private fun emptyJsonResponseTest(attributionHandler: AttributionHandler) {
        startGetAttributionTest(attributionHandler, ResponseType.EMPTY_JSON)

        // check that the mock http client was called
        assertUtil.test("MockHttpsURLConnection getInputStream")
        assertUtil.verbose("Response: { }")
        assertUtil.info("No message found")

        // check attribution was called without ask_in
        assertUtil.test("ActivityHandler setAskingAttribution, false")
        assertUtil.test("ActivityHandler launchAttributionResponseTasks, message:null timestamp:null json:{}")
    }

    private fun serverErrorTest(attributionHandler: AttributionHandler) {
        startGetAttributionTest(attributionHandler, ResponseType.INTERNAL_SERVER_ERROR)

        // check that the mock http client was called
        assertUtil.test("MockHttpsURLConnection getErrorStream")

        // the response logged
        assertUtil.verbose("Response: { \"message\": \"testResponseError\"}")

        // the message in the response
        assertUtil.error("testResponseError")

        // check attribution was called without ask_in
        assertUtil.test("ActivityHandler setAskingAttribution, false")
        assertUtil.test("ActivityHandler launchAttributionResponseTasks, message:testResponseError timestamp:null json:{\"message\":\"testResponseError\"}")
    }

    private fun okMessageTest(attributionHandler: AttributionHandler) {
        startGetAttributionTest(attributionHandler, ResponseType.MESSAGE)
        okMessageTestLogs(attributionHandler)
    }

    private fun okMessageTestLogs(attributionHandler: AttributionHandler) {
        /*
        TestActivityPackage.testQueryStringRequest(attributionHandler.lastUrlUsed.getQuery(), null);

        // check that the mock http client was called
        assertUtil.test("MockHttpsURLConnection getInputStream");

        // the response logged
        assertUtil.verbose("Response: { \"message\" : \"response OK\"}");

        // the message in the response
        assertUtil.info("response OK");

        assertUtil.test("ActivityHandler setAskingAttribution, false");

        // check attribution was called without ask_in
        assertUtil.test("ActivityHandler launchAttributionResponseTasks, message:response OK timestamp:null json:{\"message\":\"response OK\"}");
        */
    }

    private fun startGetAttributionTest(
        attributionHandler: AttributionHandler,
        responseType: ResponseType?
    ) {
        mockHttpsURLConnection.responseType = responseType
        attributionHandler.getAttribution()
        SystemClock.sleep(1000)
    }
}