package com.motrack.sdk

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.test.mock.MockContext
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
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 20th November 2021
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TestActivityHandler {
    lateinit var mockLogger: MockLogger
    lateinit var mockPackageHandler: MockPackageHandler
    lateinit var mockAttributionHandler: MockAttributionHandler
    lateinit var mockSdkClickHandler: MockSdkClickHandler
    var activity: UnitTestActivity? = null
    var context: Context? = null
    lateinit var assertUtil: AssertUtil

    @Rule
    public val mActivityRule: ActivityScenarioRule<UnitTestActivity> =
        ActivityScenarioRule(UnitTestActivity::class.java)

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        mockLogger = MockLogger()
        mockPackageHandler = MockPackageHandler(mockLogger)
        mockAttributionHandler = MockAttributionHandler(mockLogger)
        mockSdkClickHandler = MockSdkClickHandler(mockLogger)
        assertUtil = AssertUtil(mockLogger)
        MotrackFactory.setLogger(mockLogger)
        MotrackFactory.setPackageHandler(mockPackageHandler)
        MotrackFactory.setAttributionHandler(mockAttributionHandler)
        MotrackFactory.setSdkClickHandler(mockSdkClickHandler)

        // deleting state to simulate fresh install
        mockLogger.test(
            "Was AdjustActivityState deleted? " + ActivityHandler.deleteActivityState(
                context!!
            )
        )
        mockLogger.test("Was Attribution deleted? ${ActivityHandler.deleteAttribution(context!!)}")
        mockLogger.test(
            "Was Session Callback Parameters deleted? ${
                ActivityHandler.deleteSessionCallbackParameters(
                    context!!
                )
            }"
        )
        mockLogger.test(
            "Was Session Partner Parameters deleted? ${
                ActivityHandler.deleteSessionPartnerParameters(
                    context!!
                )
            }"
        )

        // check the server url
        assertUtil.isEqual(
            Constants.BASE_URL, "https://app.motrack.com"
        )
    }

    @After
    fun tearDown() {
        MotrackFactory.setPackageHandler(null)
        MotrackFactory.setAttributionHandler(null)
        MotrackFactory.setSdkClickHandler(null)
        MotrackFactory.setLogger(null)
        MotrackFactory.timerInterval = -1
        MotrackFactory.timerStart = -1
        MotrackFactory.sessionInterval = -1
        MotrackFactory.subsessionInterval = -1
        context?.let {
            val settings =
                it.getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE)
            val editor = settings.edit()
            editor.clear()
            editor.apply()
        }

        activity = null
        context = null
    }


    @Test
    fun testFirstSession() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testFirstSession")

        // create the config to start the session
        val config: MotrackConfig = getConfig()

        // start activity handler with config
        val activityHandler: ActivityHandler = startAndCheckFirstSession(config)

        // checking the default values of the first session package
        // should only have one package
        assertUtil.isEqual(1, mockPackageHandler.queue.size)
        val activityPackage = mockPackageHandler.queue[0]

        // create activity package test
        val testActivityPackage = TestActivityPackage(activityPackage)

        // set first session
        testActivityPackage.testSessionPackage(1)
    }

    @Test
    fun testEventsBuffered() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testEventsBuffered")

        // create the config to start the session
        val config: MotrackConfig = getConfig()

        // buffer events
        config.setEventBufferingEnabled(true)

        // set default tracker
        config.defaultTracker = "default1234tracker"

        //  create handler and start the first session
        val activityHandler = getActivityHandler(config)
        SystemClock.sleep(1500)

        // test init values
        val initState = StateActivityHandlerInit(activityHandler)
        initState.eventBufferingIsEnabled = true
        initState.defaultTracker = "default1234tracker"
        val stateSession = StateSession(StateSession.SessionType.NEW_SESSION)
        stateSession.eventBufferingIsEnabled = true
        checkInitAndStart(activityHandler, initState, stateSession)

        // create the first Event
        val firstEvent = MotrackEvent("event1")

        // add callback parameters
        firstEvent.addCallbackParameter("keyCall", "valueCall")
        firstEvent.addCallbackParameter("keyCall", "valueCall2")
        firstEvent.addCallbackParameter("fooCall", "barCall")

        // add partner paramters
        firstEvent.addPartnerParameter("keyPartner", "valuePartner")
        firstEvent.addPartnerParameter("keyPartner", "valuePartner2")
        firstEvent.addPartnerParameter("fooPartner", "barPartner")

        // check that callback parameter was overwritten
        assertUtil.warn("Key keyCall was overwritten")

        // check that partner parameter was overwritten
        assertUtil.warn("Key keyPartner was overwritten")

        // add revenue
        firstEvent.setRevenue(0.001, "EUR")

        // set order id
        firstEvent.setOrderId("orderIdTest")

        // track event
        activityHandler.trackEvent(firstEvent)
        SystemClock.sleep(1500)
        val stateEvent1 = StateEvent()
        stateEvent1.orderId = "orderIdTest"
        stateEvent1.bufferedSuffix = "(0.00100 EUR, 'event1')"
        stateEvent1.activityStateSuffix = "ec:1"
        checkEvent(stateEvent1)

        // create the second Event
        val secondEvent = MotrackEvent("event2")

        // set order id
        secondEvent.setOrderId("orderIdTest")

        // track second event
        activityHandler.trackEvent(secondEvent)
        SystemClock.sleep(1500)
        val stateEvent2 = StateEvent()
        stateEvent2.duplicatedOrderId = true
        stateEvent2.orderId = "orderIdTest"
        checkEvent(stateEvent2)

        // create third Event
        val thirdEvent = MotrackEvent("event3")

        // set order id
        thirdEvent.setOrderId("otherOrderId")

        // add empty revenue
        thirdEvent.setRevenue(0.0, "USD")

        // track third event
        activityHandler.trackEvent(thirdEvent)
        SystemClock.sleep(1500)
        val stateEvent3 = StateEvent()
        stateEvent3.orderId = "otherOrderId"
        stateEvent3.bufferedSuffix = "(0.00000 USD, 'event3')"
        stateEvent3.activityStateSuffix = "ec:2"
        checkEvent(stateEvent3)

        // create a forth Event object without revenue
        val forthEvent = MotrackEvent("event4")

        // track third event
        activityHandler.trackEvent(forthEvent)
        SystemClock.sleep(1500)
        val stateEvent4 = StateEvent()
        stateEvent4.bufferedSuffix = "'event4'"
        stateEvent4.activityStateSuffix = "ec:3"
        checkEvent(stateEvent4)

        // check the number of activity packages
        // 1 session + 3 events
        assertUtil.isEqual(4, mockPackageHandler.queue.size)
        val firstSessionPackage = mockPackageHandler.queue[0]

        // create activity package test
        val testFirstSessionPackage = TestActivityPackage(firstSessionPackage)
        testFirstSessionPackage.eventBufferingEnabled = true
        testFirstSessionPackage.defaultTracker = "default1234tracker"

        // set first session
        testFirstSessionPackage.testSessionPackage(1)

        // first event
        val firstEventPackage = mockPackageHandler.queue[1]

        // create event package test
        val testFirstEventPackage = TestActivityPackage(firstEventPackage)

        // set event test parameters
        testFirstEventPackage.eventCount = "1"
        testFirstEventPackage.suffix = "(0.00100 EUR, 'event1')"
        testFirstEventPackage.revenueString = "0.00100"
        testFirstEventPackage.currency = "EUR"
        testFirstEventPackage.callbackParams =
            "{\"keyCall\":\"valueCall2\",\"fooCall\":\"barCall\"}"
        testFirstEventPackage.partnerParams =
            "{\"keyPartner\":\"valuePartner2\",\"fooPartner\":\"barPartner\"}"
        testFirstEventPackage.eventBufferingEnabled = true

        // test first event
        testFirstEventPackage.testEventPackage("event1")

        // second event
        val secondEventPackage = mockPackageHandler.queue[2]

        // create event package test
        val testSecondEventPackage = TestActivityPackage(secondEventPackage)

        // set event test parameters
        testSecondEventPackage.eventCount = "2"
        testSecondEventPackage.suffix = "(0.00000 USD, 'event3')"
        testSecondEventPackage.revenueString = "0.00000"
        testSecondEventPackage.currency = "USD"
        testSecondEventPackage.eventBufferingEnabled = true

        // test second event
        testSecondEventPackage.testEventPackage("event3")

        // third event
        val thirdEventPackage = mockPackageHandler.queue[3]

        // create event package test
        val testThirdEventPackage = TestActivityPackage(thirdEventPackage)

        // set event test parameters
        testThirdEventPackage.eventCount = "3"
        testThirdEventPackage.suffix = "'event4'"
        testThirdEventPackage.eventBufferingEnabled = true

        // test third event
        testThirdEventPackage.testEventPackage("event4")
    }

    @Test
    fun testForegroundTimer() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testForegroundTimer")
        MotrackFactory.timerInterval = 4000
        MotrackFactory.timerStart = 4000

        // create the config to start the session
        val config: MotrackConfig = getConfig()

        // start activity handler with config
        val activityHandler = getActivityHandler(config)
        SystemClock.sleep(1500)

        // test init values
        val stateActivityHandlerInit = StateActivityHandlerInit(activityHandler)
        stateActivityHandlerInit.foregroundTimerStart = 4
        stateActivityHandlerInit.foregroundTimerCycle = 4
        checkInitTests(stateActivityHandlerInit)
        resumeActivity(activityHandler)
        SystemClock.sleep(1500)

        // test session
        checkFirstSession()

        // wait enough to fire the first cycle
        SystemClock.sleep(3000)
        checkForegroundTimerFired(true)

        // end subsession to stop timer
        activityHandler.onPause()

        // wait enough for a new cycle
        SystemClock.sleep(6000)

        // start a new session
        activityHandler.onResume()
        SystemClock.sleep(1000)

        // check that not enough time passed to fire again
        checkForegroundTimerFired(false)
    }

    @Test
    fun testEventsNotBuffered() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testEventsNotBuffered")

        // create the config to start the session
        val config: MotrackConfig = getConfig()

        // start activity handler with config
        val activityHandler: ActivityHandler = startAndCheckFirstSession(config)

        // create the first Event
        val firstEvent = MotrackEvent("event1")

        // track event
        activityHandler.trackEvent(firstEvent)
        SystemClock.sleep(1500)
        val stateEvent = StateEvent()
        checkEvent(stateEvent)
    }

    @Test
    fun testEventBeforeStart() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testEventBeforeStart")

        // create the config to start the session
        val config: MotrackConfig = getConfig()

        // create the first Event
        val firstEvent = MotrackEvent("event1")

        //  create handler and start the first session
        val activityHandler = getActivityHandler(config)

        // track event
        activityHandler.trackEvent(firstEvent)
        SystemClock.sleep(1500)

        // test init values
        checkInitTests(activityHandler)

        // does not start the activity because it was started by the track event

        // test session
        val stateSession = StateSession(StateSession.SessionType.NEW_SESSION)
        // does not start session
        stateSession.startSubsession = false
        stateSession.toSend = false
        checkStartInternal(stateSession)
        val stateEvent = StateEvent()
        checkEvent(stateEvent)
    }

    @Test
    fun testChecks() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testChecks")

        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testChecks")

        // config with null app token
        val nullAppTokenConfig = MotrackConfig(context, null, MotrackConfig.ENVIRONMENT_SANDBOX)
        assertUtil.error("Missing App Token")
        assertUtil.isFalse(nullAppTokenConfig.isValid())

        // config with wrong size app token
        val oversizeAppTokenConfig =
            MotrackConfig(context, "1234567890123", MotrackConfig.ENVIRONMENT_SANDBOX)
        assertUtil.error("Malformed App Token '1234567890123'")
        assertUtil.isFalse(oversizeAppTokenConfig.isValid())

        // config with null environment
        val nullEnvironmentConfig = MotrackConfig(context, "123456789012", null)
        assertUtil.error("Missing environment")
        assertUtil.isFalse(nullEnvironmentConfig.isValid())

        // config with wrong environment
        val wrongEnvironmentConfig = MotrackConfig(context, "123456789012", "Other")
        assertUtil.error("Unknown environment 'Other'")
        assertUtil.isFalse(wrongEnvironmentConfig.isValid())

        // config with null context
        val nullContextConfig =
            MotrackConfig(null, "123456789012", MotrackConfig.ENVIRONMENT_SANDBOX)
        assertUtil.error("Missing context")
        assertUtil.isFalse(nullContextConfig.isValid())

        // config without internet permission
        val mockContext: Context = object : MockContext() {
            override fun checkCallingOrSelfPermission(permission: String): Int {
                return PackageManager.PERMISSION_DENIED
            }
        }
        val mockContextConfig =
            MotrackConfig(mockContext, "123456789012", MotrackConfig.ENVIRONMENT_SANDBOX)
        assertUtil.error("Missing permission: INTERNET")
        assertUtil.isFalse(mockContextConfig.isValid())

        // config without access wifi state permission
        // TODO

        // start with null config
        val nullConductivityHandler = ActivityHandler.getInstance(null)
        assertUtil.error("MotrackConfig missing")
        assertUtil.isNull(nullConductivityHandler)
        val invalidConnectivityHandler = ActivityHandler.getInstance(nullAppTokenConfig)
        assertUtil.error("MotrackConfig not initialized correctly")
        assertUtil.isNull(invalidConnectivityHandler)

        // event with null event token
        val nullEventToken = MotrackEvent(null)
        assertUtil.error("Missing Event Token")
        assertUtil.isFalse(nullEventToken.isValid())

        // event with wrong size
        val wrongEventTokenSize = MotrackEvent("eventXX")
        assertUtil.error("Malformed Event Token 'eventXX'")
        assertUtil.isFalse(wrongEventTokenSize.isValid())

        // event
        val event = MotrackEvent("event1")

        // event with negative revenue
        event.setRevenue(-0.001, "EUR")
        assertUtil.error("Invalid amount -0.001")

        // event with null currency
        event.setRevenue(0.0, null)
        assertUtil.error("Currency must be set with revenue")

        // event with empty currency
        event.setRevenue(0.0, "")
        assertUtil.error("Currency is empty")

        // callback parameter null key
        event.addCallbackParameter(null, "callValue")
        assertUtil.error("Callback parameter key is missing")

        // callback parameter empty key
        event.addCallbackParameter("", "callValue")
        assertUtil.error("Callback parameter key is empty")

        // callback parameter null value
        event.addCallbackParameter("keyCall", null)
        assertUtil.error("Callback parameter value is missing")

        // callback parameter empty value
        event.addCallbackParameter("keyCall", "")
        assertUtil.error("Callback parameter value is empty")

        // partner parameter null key
        event.addPartnerParameter(null, "callValue")
        assertUtil.error("Partner parameter key is missing")

        // partner parameter empty key
        event.addPartnerParameter("", "callValue")
        assertUtil.error("Partner parameter key is empty")

        // partner parameter null value
        event.addPartnerParameter("keyCall", null)
        assertUtil.error("Partner parameter value is missing")

        // partner parameter empty value
        event.addPartnerParameter("keyCall", "")
        assertUtil.error("Partner parameter value is empty")

        // create the config with wrong process name
        val configWrongProcess =
            MotrackConfig(context, "123456789012", MotrackConfig.ENVIRONMENT_SANDBOX)
        configWrongProcess.processName = "com.wrong.process"

        // create handler and start the first session
        ActivityHandler.getInstance(configWrongProcess)
        assertUtil.info("Skipping initialization in background process (com.adjust.sdk.test.test)")

        // create the config with correct process name
        val configCorrectProcess =
            MotrackConfig(context, "123456789012", MotrackConfig.ENVIRONMENT_SANDBOX)
        configCorrectProcess.processName = "com.adjust.sdk.test.test"

        // create handler and start the first session
        ActivityHandler.getInstance(configCorrectProcess)
        assertUtil.notInInfo("Skipping initialization in background process")

        // create the config to start the session
        val config: MotrackConfig = getConfig()

        // create handler and start the first session
        val activityHandler: ActivityHandler = startAndCheckFirstSession(config)

        // track null event
        activityHandler.trackEvent(null)
        SystemClock.sleep(1000)
        assertUtil.error("Event missing")
        activityHandler.trackEvent(nullEventToken)
        SystemClock.sleep(1000)
        assertUtil.error("Event not initialized correctly")
        activityHandler.resetSessionCallbackParameters()
        activityHandler.resetSessionPartnerParameters()
        activityHandler.removeSessionCallbackParameter(null)
        activityHandler.removeSessionCallbackParameter("")
        activityHandler.removeSessionCallbackParameter("nonExistent")
        activityHandler.removeSessionPartnerParameter(null)
        activityHandler.removeSessionPartnerParameter("")
        activityHandler.removeSessionPartnerParameter("nonExistent")
        activityHandler.addSessionCallbackParameter(null, "value")
        activityHandler.addSessionCallbackParameter("", "value")
        activityHandler.addSessionCallbackParameter("key", null)
        activityHandler.addSessionCallbackParameter("key", "")
        activityHandler.addSessionPartnerParameter(null, "value")
        activityHandler.addSessionPartnerParameter("", "value")
        activityHandler.addSessionPartnerParameter("key", null)
        activityHandler.addSessionPartnerParameter("key", "")
        activityHandler.removeSessionCallbackParameter("nonExistent")
        activityHandler.removeSessionPartnerParameter("nonExistent")
        SystemClock.sleep(1500)
        assertUtil.warn("Session Callback parameters are not set")
        assertUtil.warn("Session Partner parameters are not set")
        assertUtil.error("Session Callback parameter key is missing")
        assertUtil.error("Session Callback parameter key is empty")
        assertUtil.warn("Session Callback parameters are not set")
        assertUtil.error("Session Partner parameter key is missing")
        assertUtil.error("Session Partner parameter key is empty")
        assertUtil.warn("Session Partner parameters are not set")
        assertUtil.error("Session Callback parameter key is missing")
        assertUtil.error("Session Callback parameter key is empty")
        assertUtil.error("Session Callback parameter value is missing")
        assertUtil.error("Session Callback parameter value is empty")
        assertUtil.error("Session Partner parameter key is missing")
        assertUtil.error("Session Partner parameter key is empty")
        assertUtil.error("Session Partner parameter value is missing")
        assertUtil.error("Session Partner parameter value is empty")
        assertUtil.warn("Session Callback parameters are not set")
        assertUtil.warn("Session Partner parameters are not set")
    }

    @Test
    fun testTeardown() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testTeardown")

        //  change the timer defaults
        MotrackFactory.timerInterval = 4000

        // create the config to start the session
        val config: MotrackConfig = getConfig()
        config.delayStart = 4.0

        // enable send in the background
        config.sendInBackground = true

        // start activity handler with config
        val activityHandler = getActivityHandler(config)
        SystemClock.sleep(1500)

        // handlers start sending
        val stateActivityHandlerInit = StateActivityHandlerInit(activityHandler)
        stateActivityHandlerInit.startsSending = false
        stateActivityHandlerInit.sendInBackgroundConfigured = true
        stateActivityHandlerInit.foregroundTimerCycle = 4
        stateActivityHandlerInit.delayStartConfigured = true
        stateActivityHandlerInit.sdkClickHandlerAlsoStartsPaused = false
        checkInitTests(stateActivityHandlerInit)
        resumeActivity(activityHandler)
        SystemClock.sleep(1500)

        // test session
        val newStateSession = StateSession(StateSession.SessionType.NEW_SESSION)
        newStateSession.sendInBackgroundConfigured = true
        newStateSession.toSend = false
        newStateSession.sdkClickHandlerAlsoStartsPaused = false
        newStateSession.delayStart = "4.0"
        checkStartInternal(newStateSession)
        activityHandler.teardown()
        assertUtil.test("PackageHandler teardown deleteState, false")
        assertUtil.test("AttributionHandler teardown")
        assertUtil.test("SdkClickHandler teardown")
        activityHandler.teardown()
        assertUtil.notInTest("PackageHandler teardown deleteState, false")
        assertUtil.notInTest("AttributionHandler teardown")
        assertUtil.notInTest("SdkClickHandler teardown")
    }

    @Test
    fun testUpdateStart() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testUpdateStart")

        // create the config to start the session
        val config: MotrackConfig = getConfig()
        config.delayStart = 10.1
        var activityHandler: ActivityHandler? = getActivityHandler(config)
        SystemClock.sleep(1500)

        // test init values
        val stateActivityHandlerInit = StateActivityHandlerInit(activityHandler!!)
        stateActivityHandlerInit.delayStartConfigured = true
        checkInitTests(stateActivityHandlerInit)
        resumeActivity(activityHandler)
        SystemClock.sleep(1000)
        val newStateSession = StateSession(StateSession.SessionType.NEW_SESSION)
        // delay start means it starts paused
        newStateSession.toSend = false
        // sdk click handler does not start paused
        newStateSession.sdkClickHandlerAlsoStartsPaused = false
        // delay configured
        newStateSession.delayStart = "10.0"
        stopActivity(activityHandler)
        SystemClock.sleep(1000)
        val stateEndSession = StateEndSession()
        checkEndSession(stateEndSession)
        activityHandler.teardown()
        activityHandler = null
        SystemClock.sleep(1000)
        val restartActivityHandler = getActivityHandler(config)
        SystemClock.sleep(1500)

        // start new one
        // delay start not configured because activity state is already created
        val restartActivityHandlerInit = StateActivityHandlerInit(restartActivityHandler)
        restartActivityHandlerInit.activityStateAlreadyCreated = true
        restartActivityHandlerInit.readActivityState = "ec:0 sc:1"
        restartActivityHandlerInit.updatePackages = true

        // test init values
        checkInitTests(restartActivityHandlerInit)
        resumeActivity(restartActivityHandler)
        SystemClock.sleep(1500)
        val stateRestartSession = StateSession(StateSession.SessionType.NEW_SUBSESSION)
        stateRestartSession.activityStateAlreadyCreated = true
        stateRestartSession.subsessionCount = 2
        checkStartInternal(stateRestartSession)
    }

    @Test
    fun testSessions() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testSessions")

        // adjust the session intervals for testing
        MotrackFactory.sessionInterval = 4000

        // create the config to start the session
        val config: MotrackConfig = getConfig()

        // start activity handler with config
        val activityHandler: ActivityHandler = startAndCheckFirstSession(config)

        // end subsession
        stopActivity(activityHandler)
        SystemClock.sleep(2000)

        // test the end of the subsession
        checkEndSession()

        // start a subsession
        resumeActivity(activityHandler)
        SystemClock.sleep(2000)

        // test the new sub session
        val secondSubsession = StateSession(StateSession.SessionType.NEW_SUBSESSION)
        secondSubsession.subsessionCount = 2
        checkStartInternal(secondSubsession)
        stopActivity(activityHandler)
        SystemClock.sleep(5000)

        // test the end of the subsession
        checkEndSession()

        // trigger a new session
        activityHandler.onResume()
        SystemClock.sleep(1500)

        // new session
        val secondSession = StateSession(StateSession.SessionType.NEW_SESSION)
        secondSession.sessionCount = 2
        checkStartInternal(secondSession)

        // stop and start the activity with little interval
        // so it won't trigger a sub session
        stopActivity(activityHandler)
        resumeActivity(activityHandler)
        SystemClock.sleep(1500)

        // test the end of the subsession
        val stateEndSession = StateEndSession()
        stateEndSession.pausing = false
        checkEndSession(stateEndSession)

        // test non sub session
        val nonSessionState = StateSession(StateSession.SessionType.NONSESSION)
        checkStartInternal(nonSessionState)

        // 2 session packages
        assertUtil.isEqual(2, mockPackageHandler.queue.size)
        val firstSessionActivityPackage = mockPackageHandler.queue[0]

        // create activity package test
        val testFirstSessionActivityPackage = TestActivityPackage(firstSessionActivityPackage)

        // test first session
        testFirstSessionActivityPackage.testSessionPackage(1)

        // get second session package
        val secondSessionActivityPackage = mockPackageHandler.queue[1]

        // create second session test package
        val testSecondSessionActivityPackage = TestActivityPackage(secondSessionActivityPackage)

        // check if it saved the second subsession in the new package
        testSecondSessionActivityPackage.subsessionCount = 2

        // test second session
        testSecondSessionActivityPackage.testSessionPackage(2)
    }

    @Test
    fun testDisable() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testDisable")

        // adjust the session intervals for testing
        MotrackFactory.sessionInterval = 4000

        // create the config to start the session
        val config: MotrackConfig = getConfig()

        //  create handler and start the first session
        val activityHandler = getActivityHandler(config)

        // check that it is enabled
        assertUtil.isTrue(activityHandler.isEnabled())

        // disable sdk
        activityHandler.setEnabled(false)
        SystemClock.sleep(1000)

        // check that it is disabled
        assertUtil.isFalse(activityHandler.isEnabled())

        // not writing activity state because it set enable does not start the sdk
        assertUtil.notInDebug("Wrote Activity state")
        val stateActivityHandlerInit = StateActivityHandlerInit(activityHandler)
        stateActivityHandlerInit.startsSending = false
        stateActivityHandlerInit.startEnabled = false
        checkInitTests(stateActivityHandlerInit)

        // check if message the disable of the SDK
        assertUtil.info("Handlers will start as paused due to the SDK being disabled")
        checkHandlerStatus(true)

        // start the sdk
        // foreground timer does not start because it's paused
        resumeActivity(activityHandler)
        val firstEvent = MotrackEvent("event1")
        activityHandler.trackEvent(firstEvent)
        SystemClock.sleep(1500)

        // check initial created session
        val initialSessionDisabled = StateSession(StateSession.SessionType.DISABLED)
        initialSessionDisabled.toSend = false
        initialSessionDisabled.foregroundTimerStarts = false
        checkStartInternal(initialSessionDisabled)

        // and failed event
        val stateFailedEvent = StateEvent()
        stateFailedEvent.disabled = true
        checkEvent(stateFailedEvent)

        // try to pause session
        stopActivity(activityHandler)
        SystemClock.sleep(1500)
        val stateEndSession = StateEndSession()
        stateEndSession.checkOnPause = true
        stateEndSession.foregroundAlreadySuspended = true
        checkEndSession(stateEndSession)
        SystemClock.sleep(4000)

        // try to generate a new session
        resumeActivity(activityHandler)
        SystemClock.sleep(1500)
        val sessionDisabled = StateSession(StateSession.SessionType.DISABLED)
        sessionDisabled.toSend = false
        sessionDisabled.foregroundTimerStarts = false
        sessionDisabled.disabled = true
        checkStartInternal(sessionDisabled)

        // only the first session package should be sent
        assertUtil.isEqual(0, mockPackageHandler.queue.size)

        // put in offline mode
        activityHandler.setOfflineMode(true)

        // wait to update status
        SystemClock.sleep(1500)

        // pausing due to offline mode
        assertUtil.info("Pausing handlers to put SDK offline mode")

        // after pausing, even when it's already paused
        // tries to update the status
        checkHandlerStatus(true)

        // re-enable the SDK
        activityHandler.setEnabled(true)

        // wait to update status
        SystemClock.sleep(1500)

        // check that it is enabled
        assertUtil.isTrue(activityHandler.isEnabled())

        // check message of SDK still paused
        assertUtil.info("Handlers remain paused")

        // wait to generate a new session
        SystemClock.sleep(5000)

        // even though it will remained paused,
        // it will update the status to paused
        checkHandlerStatus(true)

        // generate a new session
        resumeActivity(activityHandler)
        val secondEvent = MotrackEvent("event2")
        activityHandler.trackEvent(secondEvent)
        SystemClock.sleep(1500)

        // difference from the first session is that now the foreground timer starts
        val sessionOffline = StateSession(StateSession.SessionType.NEW_SESSION)
        sessionOffline.toSend = false
        sessionOffline.foregroundTimerStarts = true
        sessionOffline.foregroundTimerAlreadyStarted = false
        checkStartInternal(sessionOffline)

        // and the event does not fail
        val stateEvent = StateEvent()
        checkEvent(stateEvent)

        // it should have the second session and the event
        assertUtil.isEqual(2, mockPackageHandler.queue.size)
        val secondSessionPackage = mockPackageHandler.queue[0]

        // create activity package test
        val testSecondSessionPackage = TestActivityPackage(secondSessionPackage)

        // set the sub sessions
        testSecondSessionPackage.subsessionCount = 1

        // test second session
        testSecondSessionPackage.testSessionPackage(2)
        val eventPackage = mockPackageHandler.queue[1]

        // create activity package test
        val testEventPackage = TestActivityPackage(eventPackage)
        testEventPackage.suffix = "'event2'"

        // test event
        testEventPackage.testEventPackage("event2")

        // end the session
        stopActivity(activityHandler)
        SystemClock.sleep(1500)
        checkEndSession()

        // put in online mode
        activityHandler.setOfflineMode(false)
        SystemClock.sleep(1500)

        // message that is finally resuming
        assertUtil.info("Resuming handlers to put SDK in online mode")
        SystemClock.sleep(5000)

        // after un-pausing the sdk, tries to update the handlers
        // it is still paused because it's on the background
        checkHandlerStatus(true)
        resumeActivity(activityHandler)
        SystemClock.sleep(1500)
        val thirdSessionStarting = StateSession(StateSession.SessionType.NEW_SESSION)
        thirdSessionStarting.sessionCount = 3
        thirdSessionStarting.eventCount = 1
        checkStartInternal(thirdSessionStarting)
    }

    @Test
    fun testOpenUrl() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testOpenUrl")

        // create the config to start the session
        val config: MotrackConfig = getConfig()

        // start activity handler with config
        val activityHandler: ActivityHandler = startAndCheckFirstSession(config)
        val attributions =
            Uri.parse("AdjustTests://example.com/path/inApp?adjust_tracker=trackerValue&other=stuff&adjust_campaign=campaignValue&adjust_adgroup=adgroupValue&adjust_creative=creativeValue")
        val extraParams =
            Uri.parse("AdjustTests://example.com/path/inApp?adjust_foo=bar&other=stuff&adjust_key=value")
        val mixed =
            Uri.parse("AdjustTests://example.com/path/inApp?adjust_foo=bar&other=stuff&adjust_campaign=campaignValue&adjust_adgroup=adgroupValue&adjust_creative=creativeValue")
        val encodedSeparators =
            Uri.parse("AdjustTests://example.com/path/inApp?adjust_foo=b%26a%3B%3Dr&adjust_campaign=campaign%3DValue%26&other=stuff")
        val emptyQueryString = Uri.parse("AdjustTests://")
        val emptyString = Uri.parse("")
        val nullUri: Uri? = null
        val single = Uri.parse("AdjustTests://example.com/path/inApp?adjust_foo")
        val prefix = Uri.parse("AdjustTests://example.com/path/inApp?adjust_=bar")
        val incomplete = Uri.parse("AdjustTests://example.com/path/inApp?adjust_foo=")
        val now = System.currentTimeMillis()
        activityHandler.readOpenUrl(attributions, now)
        activityHandler.readOpenUrl(extraParams, now)
        activityHandler.readOpenUrl(mixed, now)
        activityHandler.readOpenUrl(encodedSeparators, now)
        activityHandler.readOpenUrl(emptyQueryString, now)
        activityHandler.readOpenUrl(emptyString, now)
        activityHandler.readOpenUrl(nullUri!!, now)
        activityHandler.readOpenUrl(single, now)
        activityHandler.readOpenUrl(prefix, now)
        activityHandler.readOpenUrl(incomplete, now)
        SystemClock.sleep(1000)
        assertUtil.verbose("Url to parse (%s)", attributions)
        assertUtil.test("SdkClickHandler sendSdkClick")
        assertUtil.verbose("Url to parse (%s)", extraParams)
        assertUtil.test("SdkClickHandler sendSdkClick")
        assertUtil.verbose("Url to parse (%s)", mixed)
        assertUtil.test("SdkClickHandler sendSdkClick")
        assertUtil.verbose("Url to parse (%s)", encodedSeparators)
        assertUtil.test("SdkClickHandler sendSdkClick")
        assertUtil.verbose("Url to parse (%s)", emptyQueryString)
        assertUtil.test("SdkClickHandler sendSdkClick")
        assertUtil.verbose("Url to parse (%s)", single)
        assertUtil.test("SdkClickHandler sendSdkClick")
        assertUtil.verbose("Url to parse (%s)", prefix)
        assertUtil.test("SdkClickHandler sendSdkClick")
        assertUtil.verbose("Url to parse (%s)", incomplete)
        assertUtil.test("SdkClickHandler sendSdkClick")

        // check that it did not send any other click package
        assertUtil.notInTest("SdkClickHandler sendSdkClick")

        // 8 clicks
        assertUtil.isEqual(8, mockSdkClickHandler.queue.size)

        // get the click package
        val attributionClickPackage = mockSdkClickHandler.queue[0]

        // create activity package test
        val testAttributionClickPackage = TestActivityPackage(attributionClickPackage)

        // create the attribution
        val firstAttribution = MotrackAttribution()
        firstAttribution.trackerName = "trackerValue"
        firstAttribution.campaign = "campaignValue"
        firstAttribution.adgroup = "adgroupValue"
        firstAttribution.creative = "creativeValue"

        // and set it
        testAttributionClickPackage.attribution = firstAttribution
        testAttributionClickPackage.deeplink = attributions.toString()

        // test the first deeplink
        testAttributionClickPackage.testClickPackage("deeplink")

        // get the click package
        val extraParamsClickPackage = mockSdkClickHandler.queue[1]

        // create activity package test
        val testExtraParamsClickPackage = TestActivityPackage(extraParamsClickPackage)

        // other deep link parameters
        testExtraParamsClickPackage.otherParameters = "{\"key\":\"value\",\"foo\":\"bar\"}"
        testExtraParamsClickPackage.deeplink = extraParams.toString()

        // test the second deeplink
        testExtraParamsClickPackage.testClickPackage("deeplink")

        // get the click package
        val mixedClickPackage = mockSdkClickHandler.queue[2]

        // create activity package test
        val testMixedClickPackage = TestActivityPackage(mixedClickPackage)

        // create the attribution
        val secondAttribution = MotrackAttribution()
        secondAttribution.campaign = "campaignValue"
        secondAttribution.adgroup = "adgroupValue"
        secondAttribution.creative = "creativeValue"

        // and set it
        testMixedClickPackage.attribution = secondAttribution

        // other deep link parameters
        testMixedClickPackage.otherParameters = "{\"foo\":\"bar\"}"
        testMixedClickPackage.deeplink = mixed.toString()

        // test the third deeplink
        testMixedClickPackage.testClickPackage("deeplink")

        // get the click package
        val encodedClickPackage = mockSdkClickHandler.queue[3]

        // create activity package test
        val testEncodedClickPackage = TestActivityPackage(encodedClickPackage)

        // create the attribution
        val thirdAttribution = MotrackAttribution()
        thirdAttribution.campaign = "campaign=Value&"

        // and set it
        testEncodedClickPackage.attribution = thirdAttribution

        // other deep link parameters
        testEncodedClickPackage.otherParameters = "{\"foo\":\"b&a;=r\"}"
        testEncodedClickPackage.deeplink = encodedSeparators.toString()

        // test the third deeplink
        testEncodedClickPackage.testClickPackage("deeplink")

        // get the click package
        val emptyQueryStringClickPackage = mockSdkClickHandler.queue[4]

        // create activity package test
        val testEmptyQueryStringClickPackage = TestActivityPackage(emptyQueryStringClickPackage)
        testEmptyQueryStringClickPackage.deeplink = emptyQueryString.toString()
        testEmptyQueryStringClickPackage.testClickPackage("deeplink")

        // get the click package
        val singleClickPackage = mockSdkClickHandler.queue[5]

        // create activity package test
        val testSingleClickPackage = TestActivityPackage(singleClickPackage)
        testSingleClickPackage.deeplink = single.toString()
        testSingleClickPackage.testClickPackage("deeplink")

        // get the click package
        val prefixClickPackage = mockSdkClickHandler.queue[6]

        // create activity package test
        val testPrefixClickPackage = TestActivityPackage(prefixClickPackage)
        testPrefixClickPackage.deeplink = prefix.toString()
        testPrefixClickPackage.testClickPackage("deeplink")

        // get the click package
        val incompleteClickPackage = mockSdkClickHandler.queue[7]

        // create activity package test
        val testIncompleteClickPackage = TestActivityPackage(incompleteClickPackage)
        testIncompleteClickPackage.deeplink = incomplete.toString()
        testIncompleteClickPackage.testClickPackage("deeplink")
    }

    @Test
    fun testAttributionDelegate() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testAttributionDelegate")

        // create the config to start the session
        val config: MotrackConfig = getConfig()
        config.onAttributionChangedListener = object : OnAttributionChangedListener {
            override fun onAttributionChanged(attribution: MotrackAttribution?) {
                mockLogger.test("onAttributionChanged: $attribution")
            }
        }
        val attributionDelegatePresent = StateDelegates()
        attributionDelegatePresent.attributionDelegatePresent = true
        checkFinishTasks(config, attributionDelegatePresent)
    }

    @Test
    fun testSendBackground() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testSendBackground")
        MotrackFactory.timerInterval = 4000

        // create the config to start the session
        val config: MotrackConfig = getConfig()

        // enable send in the background
        config.sendInBackground = true

        // create activity handler without starting
        val activityHandler = getActivityHandler(config)
        SystemClock.sleep(1500)

        // handlers start sending
        val stateActivityHandlerInit = StateActivityHandlerInit(activityHandler)
        stateActivityHandlerInit.startsSending = true
        stateActivityHandlerInit.sendInBackgroundConfigured = true
        stateActivityHandlerInit.foregroundTimerCycle = 4
        checkInitTests(stateActivityHandlerInit)
        resumeActivity(activityHandler)
        SystemClock.sleep(1500)

        // test session
        val newStateSession = StateSession(StateSession.SessionType.NEW_SESSION)
        newStateSession.sendInBackgroundConfigured = true
        newStateSession.toSend = true
        checkStartInternal(newStateSession)

        // end subsession
        // background timer starts
        stopActivity(activityHandler)
        SystemClock.sleep(1500)

        // session end does not pause the handlers
        val stateEndSession1 = StateEndSession()
        stateEndSession1.pausing = false
        stateEndSession1.checkOnPause = true
        stateEndSession1.backgroundTimerStarts = true
        checkEndSession(stateEndSession1)

        // end subsession again
        // to test if background timer starts again
        stopActivity(activityHandler)
        SystemClock.sleep(1500)

        // session end does not pause the handlers
        val stateEndSession2 = StateEndSession()
        stateEndSession2.pausing = false
        stateEndSession2.checkOnPause = true
        stateEndSession2.foregroundAlreadySuspended = true
        checkEndSession(stateEndSession2)

        // wait for background timer launch
        SystemClock.sleep(3000)

        // background timer fired
        assertUtil.test("PackageHandler sendFirstPackage")

        // wait enough time
        SystemClock.sleep(3000)

        // check that background timer does not fire again
        assertUtil.notInTest("PackageHandler sendFirstPackage")
        activityHandler.trackEvent(MotrackEvent("abc123"))
        SystemClock.sleep(1000)
        val stateEvent = StateEvent()
        stateEvent.backgroundTimerStarts = 4
        checkEvent(stateEvent)

        // disable and enable the sdk while in the background
        activityHandler.setEnabled(false)
        SystemClock.sleep(1000)

        // check that it is disabled
        assertUtil.isFalse(activityHandler.isEnabled())

        // check if message the disable of the SDK
        assertUtil.info("Pausing handlers due to SDK being disabled")
        SystemClock.sleep(1000)

        // handlers being paused because of the disable
        checkHandlerStatus(true)
        activityHandler.setEnabled(true)
        SystemClock.sleep(1000)

        // check that it is enabled
        assertUtil.isTrue(activityHandler.isEnabled())

        // check if message the enable of the SDK
        assertUtil.info("Resuming handlers due to SDK being enabled")
        SystemClock.sleep(1000)

        // handlers being resumed because of the enable
        // even in the background because of the sendInBackground option
        checkHandlerStatus(false)

        // set offline and online the sdk while in the background
        activityHandler.setOfflineMode(true)
        SystemClock.sleep(1000)
        val internalState: ActivityHandler.InternalState? = activityHandler.internalState

        // check that it is offline
        if (internalState != null) {
            assertUtil.isTrue(internalState.isOffline)
        }

        // check if message the offline of the SDK
        assertUtil.info("Pausing handlers to put SDK offline mode")
        SystemClock.sleep(1000)

        // handlers being paused because of the offline
        checkHandlerStatus(true)
        activityHandler.setOfflineMode(false)
        SystemClock.sleep(1000)

        // check that it is online
        if (internalState != null) {
            assertUtil.isTrue(internalState.isOnline())
        }

        // check if message the online of the SDK
        assertUtil.info("Resuming handlers to put SDK in online mode")
        SystemClock.sleep(1000)

        // handlers being resumed because of the online
        // even in the background because of the sendInBackground option
        checkHandlerStatus(false)
    }


    @Test
    fun testPushToken() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testPushToken")
        val config: MotrackConfig = getConfig()
        // set the push token before the sdk starts
        config.pushToken = "preStartPushToken"

        // start activity handler with config
        val activityHandler = getActivityHandler(config)
        SystemClock.sleep(1500)

        // test init values
        val stateActivityHandlerInit = StateActivityHandlerInit(activityHandler)
        stateActivityHandlerInit.pushToken = "preStartPushToken"
        checkInitTests(stateActivityHandlerInit)
        resumeActivity(activityHandler)
        SystemClock.sleep(1500)

        // test session
        checkFirstSession()

        // create the first Event
        val firstEvent = MotrackEvent("event1")

        // track event
        activityHandler.trackEvent(firstEvent)
        SystemClock.sleep(1500)

        // checking the default values of the first session package
        assertUtil.isEqual(2, mockPackageHandler.queue.size)
        val activityPackage = mockPackageHandler.queue[0]

        // create activity package test
        val testActivityPackage = TestActivityPackage(activityPackage)
        testActivityPackage.pushToken = "preStartPushToken"

        // set first session
        testActivityPackage.testSessionPackage(1)

        // first event
        val firstEventPackage = mockPackageHandler.queue[1]

        // create event package test
        val testFirstEventPackage = TestActivityPackage(firstEventPackage)

        // set event test parameters
        testFirstEventPackage.eventCount = "1"
        testFirstEventPackage.suffix = "'event1'"
        testFirstEventPackage.pushToken = "preStartPushToken"

        // test first event
        testFirstEventPackage.testEventPackage("event1")

        // try to update with the same push token
        activityHandler.setPushToken("preStartPushToken")
        SystemClock.sleep(1500)

        // should not have added a new package either in the package handler
        assertUtil.isEqual(2, mockPackageHandler.queue.size)

        // nor the click handler
        assertUtil.notInTest("SdkClickHandler sendSdkClick")
        assertUtil.isEqual(0, mockSdkClickHandler.queue.size)

        // update with new push token
        activityHandler.setPushToken("newPushToken")
        SystemClock.sleep(1500)

        // check it was added to sdk click handler
        assertUtil.notInTest("SdkClickHandler sendSdkClick")
        assertUtil.isEqual(0, mockSdkClickHandler.queue.size)

        // check that info package was added
        assertUtil.test("PackageHandler addPackage")

        // check that event was sent to package handler
        assertUtil.test("PackageHandler sendFirstPackage")

        // checking that the info package was added
        assertUtil.isEqual(3, mockPackageHandler.queue.size)

        // get the click package
        val sdkInfoPackage = mockPackageHandler.queue[2]

        // create activity package test
        val testInfoPackage = TestActivityPackage(sdkInfoPackage)
        testInfoPackage.pushToken = "newPushToken"

        // test the first deeplink
        testInfoPackage.testInfoPackage("push")
    }

    @Test
    fun testCheckAttributionState() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testCheckAttributionState")

        // if it's the first launch
        //if (internalState.isFirstLaunch()) {
        //    if (internalState.hasSessionResponseNotBeenProcessed()) {
        //        return;
        //    }
        //}
        //if (attribution != null && !activityState.askingAttribution) {
        //    return;
        //}
        //attributionHandler.getAttribution();
        MotrackFactory.sessionInterval = 4000

        // create the config to start the session
        val config: MotrackConfig = getConfig()
        config.onAttributionChangedListener = object : OnAttributionChangedListener {
            override fun onAttributionChanged(attribution: MotrackAttribution?) {
                mockLogger.test("onAttributionChanged $attribution")
            }
        }

        // start activity handler with config
        var activityHandler = getActivityHandler(config)
        SystemClock.sleep(1500)

        // test init values
        checkInitTests(activityHandler)
        resumeActivity(activityHandler)
        SystemClock.sleep(1500)

        // it's first launch
        // session response has not been processed
        // attribution is null
        // -> not called
        var newSessionState = StateSession(StateSession.SessionType.NEW_SESSION)
        newSessionState.getAttributionIsCalled = false
        checkStartInternal(newSessionState)
        val firstSessionPackage = mockPackageHandler.queue[0]

        // trigger a new sub session
        activityHandler.onResume()
        SystemClock.sleep(2000)

        // does not call because the session has not been processed
        checkSubSession(1, 2, false)

        // it's first launch
        // session response has been processed
        // attribution is null
        // -> called

        // simulate a successful session
        val successSessionResponseData =
            ResponseData.buildResponseData(firstSessionPackage, null) as SessionResponseData
        successSessionResponseData.success = true
        successSessionResponseData.message = "Session successfully tracked"
        successSessionResponseData.adid = "adidValue"
        activityHandler.launchSessionResponseTasks(successSessionResponseData)

        // trigger a new sub session
        activityHandler.onResume()
        SystemClock.sleep(2000)

        // does call because the session has been processed
        checkSubSession(1, 3, true)

        // it's first launch
        // session response has been processed
        // attribution is not null
        // askingAttribution is false
        // -> not called

        // save the new attribution
        successSessionResponseData.attribution = MotrackAttribution()
        successSessionResponseData.attribution!!.trackerName = "trackerName"
        activityHandler.launchSessionResponseTasks(successSessionResponseData)
        activityHandler.setAskingAttribution(false)

        // trigger a new sub session
        activityHandler.onResume()
        SystemClock.sleep(2000)

        // does call because the session has been processed
        checkSubSession(1, 4, false)

        // it's first launch
        // session response has been processed
        // attribution is not null
        // askingAttribution is true
        // -> called
        activityHandler.setAskingAttribution(true)

        // trigger a new sub session
        activityHandler.onResume()
        SystemClock.sleep(2000)

        // does call because the session has been processed
        checkSubSession(1, 5, true)

        // it's not first launch
        // attribution is null
        // -> called

        // finish activity handler
        activityHandler.teardown()
        // delete attribution
        ActivityHandler.deleteAttribution(context!!)

        // start new activity handler
        SystemClock.sleep(5000)
        activityHandler = getActivityHandler(config)
        SystemClock.sleep(1500)

        // test init values
        val stateActivityHandlerInit = StateActivityHandlerInit(activityHandler)
        stateActivityHandlerInit.readActivityState = "ec:0 sc:1 ssc:5"
        checkInitTests(stateActivityHandlerInit)
        resumeActivity(activityHandler)
        SystemClock.sleep(1500)
        newSessionState = StateSession(StateSession.SessionType.NEW_SESSION)
        newSessionState.getAttributionIsCalled = true
        newSessionState.sessionCount = 2
        checkStartInternal(newSessionState)

        // it's not first launch
        // attribution is not null
        // askingAttribution is true
        // -> called

        // save the new attribution
        successSessionResponseData.attribution = MotrackAttribution()
        successSessionResponseData.attribution!!.trackerName = "trackerName"
        activityHandler.launchSessionResponseTasks(successSessionResponseData)
        activityHandler.setAskingAttribution(true)

        // trigger a new sub session
        activityHandler.onResume()
        SystemClock.sleep(2000)

        // does call because the session has been processed
        checkSubSession(2, 2, true)

        // it's not first launch
        // attribution is not null
        // askingAttribution is false
        // -> not called
        activityHandler.setAskingAttribution(false)

        // trigger a new sub session
        activityHandler.onResume()
        SystemClock.sleep(2000)

        // does call because the session has been processed
        checkSubSession(2, 3, false)
    }

    @Test
    fun testSuccessDelegates() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testSuccessDelegates")

        // create the config to start the session
        val config: MotrackConfig = getConfig()
        config.onEventTrackingSucceededListener = object : OnEventTrackingSucceededListener {
            override fun onFinishedEventTrackingSucceeded(eventSuccessResponseData: MotrackEventSuccess) {
                mockLogger.test("onFinishedEventTrackingSucceeded: $eventSuccessResponseData")
            }
        }
        config.onSessionTrackingSucceededListener = object : OnSessionTrackingSucceededListener {
            override fun onFinishedSessionTrackingSucceeded(sessionSuccessResponseData: MotrackSessionSuccess?) {
                mockLogger.test("onFinishedSessionTrackingSucceeded: $sessionSuccessResponseData")
            }
        }
        val successDelegatesPresent = StateDelegates()
        successDelegatesPresent.eventSuccessDelegatePresent = true
        successDelegatesPresent.sessionSuccessDelegatePresent = true
        checkFinishTasks(config, successDelegatesPresent)
    }

    @Test
    fun testFailureDelegates() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testFailureDelegates")

        // create the config to start the session
        val config: MotrackConfig = getConfig()
        config.onEventTrackingFailedListener = object : OnEventTrackingFailedListener {
            override fun onFinishedEventTrackingFailed(eventFailureResponseData: MotrackEventFailure) {
                mockLogger.test("onFinishedEventTrackingFailed: $eventFailureResponseData")
            }
        }
        config.onSessionTrackingFailedListener = object : OnSessionTrackingFailedListener {
            override fun onFinishedSessionTrackingFailed(failureResponseData: MotrackSessionFailure?) {
                mockLogger.test("onFinishedSessionTrackingFailed: $failureResponseData")
            }
        }
        val failureDelegatesPresent = StateDelegates()
        failureDelegatesPresent.sessionFailureDelegatePresent = true
        failureDelegatesPresent.eventFailureDelegatePresent = true
        checkFinishTasks(config, failureDelegatesPresent)
    }

    private fun checkFinishTasks(
        config: MotrackConfig,
        stateDelegates: StateDelegates
    ) {
        val activityHandler: ActivityHandler = startAndCheckFirstSession(config)

        // test first session package
        val firstSessionPackage = mockPackageHandler.queue[0]

        // create activity package test
        val testActivityPackage = TestActivityPackage(firstSessionPackage)

        // set first session
        testActivityPackage.testSessionPackage(1)
        val now = System.currentTimeMillis()
        val dateString = Util.dateFormatter.format(now)

        // simulate a successful session
        val successSessionResponseData =
            ResponseData.buildResponseData(firstSessionPackage, null) as SessionResponseData
        successSessionResponseData.success = true
        successSessionResponseData.message = "Session successfully tracked"
        successSessionResponseData.timestamp = dateString
        successSessionResponseData.adid = "adidValue"
        activityHandler.finishedTrackingActivity(successSessionResponseData)
        SystemClock.sleep(1000)

        // attribution handler should always receive the session response
        assertUtil.test("AttributionHandler checkSessionResponse")

        // attribution handler does not receive sdk click
        assertUtil.notInTest("AttributionHandler checkSdkClickResponse")

        // the first session does not trigger the event response delegate
        assertUtil.notInDebug("Launching success event tracking listener")
        assertUtil.notInDebug("Launching failed event tracking listener")
        activityHandler.launchSessionResponseTasks(successSessionResponseData)
        SystemClock.sleep(1000)

        // if present, the first session triggers the success session delegate
        if (stateDelegates.sessionSuccessDelegatePresent) {
            assertUtil.debug("Launching success session tracking listener")
            assertUtil.test("onFinishedSessionTrackingSucceeded: Session Success msg:Session successfully tracked time:$dateString adid:adidValue json:null")
        } else {
            assertUtil.notInDebug("Launching success session tracking delegate")
            assertUtil.notInTest("onFinishedSessionTrackingSucceeded: Session Success ")
        }
        // it doesn't trigger the failure session delegate
        assertUtil.notInDebug("Launching failed session tracking listener")

        // simulate a failure session
        val failureSessionResponseData =
            ResponseData.buildResponseData(firstSessionPackage, null) as SessionResponseData
        failureSessionResponseData.success = false
        failureSessionResponseData.message = "Session failure tracked"
        failureSessionResponseData.timestamp = dateString
        failureSessionResponseData.adid = "adidValue"
        activityHandler.launchSessionResponseTasks(failureSessionResponseData)
        SystemClock.sleep(1000)

        // it doesn't trigger the success session delegate
        assertUtil.notInDebug("Launching success session tracking listener")

        // if present, the first session triggers the failure session delegate
        if (stateDelegates.sessionFailureDelegatePresent) {
            assertUtil.debug("Launching failed session tracking listener")
            assertUtil.test("onFinishedSessionTrackingFailed: Session Failure msg:Session failure tracked time:$dateString adid:adidValue retry:false json:null")
        } else {
            assertUtil.notInDebug("Launching failed session tracking listener")
            assertUtil.notInTest("onFinishedSessionTrackingFailed: Session Failure ")
        }

        // test success event response data
        activityHandler.trackEvent(MotrackEvent("abc123"))
        SystemClock.sleep(1000)
        val eventPackage = mockPackageHandler.queue[1]
        val eventSuccessResponseData =
            ResponseData.buildResponseData(eventPackage, null) as EventResponseData
        eventSuccessResponseData.success = true
        eventSuccessResponseData.message = "Event successfully tracked"
        eventSuccessResponseData.timestamp = dateString
        eventSuccessResponseData.adid = "adidValue"
        activityHandler.finishedTrackingActivity(eventSuccessResponseData)
        SystemClock.sleep(1000)

        // attribution handler should never receive the event response
        assertUtil.notInTest("AttributionHandler checkSessionResponse")

        // if present, the success event triggers the success event delegate
        if (stateDelegates.eventSuccessDelegatePresent) {
            assertUtil.debug("Launching success event tracking listener")
            assertUtil.test("onFinishedEventTrackingSucceeded: Event Success msg:Event successfully tracked time:$dateString adid:adidValue event:abc123 json:null")
        } else {
            assertUtil.notInDebug("Launching success event tracking listener")
            assertUtil.notInTest("onFinishedEventTrackingSucceeded: Event Success ")
        }
        // it doesn't trigger the failure event delegate
        assertUtil.notInDebug("Launching failed event tracking listener")

        // test failure event response data
        val eventFailureResponseData =
            ResponseData.buildResponseData(eventPackage, null) as EventResponseData
        eventFailureResponseData.success = false
        eventFailureResponseData.message = "Event failure tracked"
        eventFailureResponseData.timestamp = dateString
        eventFailureResponseData.adid = "adidValue"
        activityHandler.finishedTrackingActivity(eventFailureResponseData)
        SystemClock.sleep(1000)

        // attribution handler should never receive the event response
        assertUtil.notInTest("AttributionHandler checkSessionResponse")

        // if present, the failure event triggers the failure event delegate
        if (stateDelegates.eventFailureDelegatePresent) {
            assertUtil.debug("Launching failed event tracking listener")
            assertUtil.test("onFinishedEventTrackingFailed: Event Failure msg:Event failure tracked time:$dateString adid:adidValue event:abc123 retry:false json:null")
        } else {
            assertUtil.notInDebug("Launching failed event tracking listener")
            assertUtil.notInTest("onFinishedEventTrackingFailed: Event Failure ")
        }
        // it doesn't trigger the success event delegate
        assertUtil.notInDebug("Launching success event tracking listener")

        // test click
        val attributions =
            Uri.parse("AdjustTests://example.com/path/inApp?adjust_tracker=trackerValue&other=stuff&adjust_campaign=campaignValue&adjust_adgroup=adgroupValue&adjust_creative=creativeValue")
        activityHandler.readOpenUrl(attributions, now)
        SystemClock.sleep(1000)
        assertUtil.test("SdkClickHandler sendSdkClick")

        // test sdk_click response data
        val sdkClickPackage = mockSdkClickHandler.queue[0]
        val sdkClickResponseData =
            ResponseData.buildResponseData(sdkClickPackage, null) as SdkClickResponseData
        activityHandler.finishedTrackingActivity(sdkClickResponseData)
        SystemClock.sleep(1000)

        // attribution handler receives the click response
        assertUtil.test("AttributionHandler checkSdkClickResponse")

        // attribution handler does not receive session response
        assertUtil.notInTest("AttributionHandler checkSessionResponse")

        // it doesn't trigger the any event delegate
        assertUtil.notInDebug("Launching success event tracking listener")
        assertUtil.notInDebug("Launching failed event tracking listener")
    }

    @Test
    fun testLaunchDeepLink() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testLaunchDeepLink")

        // create the config to start the session
        val config: MotrackConfig = getConfig()

        // start activity handler with config
        val activityHandler: ActivityHandler = startAndCheckFirstSession(config)
        val responseDataNull: ResponseData? = null
        activityHandler.finishedTrackingActivity(responseDataNull!!)
        SystemClock.sleep(1500)

        // if the response is null
        assertUtil.notInTest("AttributionHandler checkAttribution")
        assertUtil.notInError("Unable to open deferred deep link")
        assertUtil.notInInfo("Open deferred deep link")

        // test success session response data
        val sessionResponseDeeplink =
            ResponseData.buildResponseData(mockPackageHandler.queue[0], null) as SessionResponseData
        try {
            sessionResponseDeeplink.jsonResponse = JSONObject(
                "{ " +
                        "\"deeplink\" :  \"adjustTestSchema://\" }"
            )
        } catch (e: JSONException) {
            assertUtil.fail(e.message!!)
        }
        activityHandler.launchSessionResponseTasks(sessionResponseDeeplink)
        SystemClock.sleep(1500)

        // check that it was unable to open the url
        assertUtil.notInError("Unable to open deferred deep link")
        assertUtil.notInInfo("Open deferred deep link")

        // test attribution response
        val attributionResponseDeeplink =
            ResponseData.buildResponseData(
                mockAttributionHandler.attributionPackage,
                null
            ) as AttributionResponseData
        attributionResponseDeeplink.deeplink = Uri.parse("adjustTestSchema://")
        activityHandler.launchAttributionResponseTasks(attributionResponseDeeplink)
        SystemClock.sleep(1500)
        assertUtil.info("Deferred deeplink received (adjustTestSchema://)")
        assertUtil.notInError("Unable to open deferred deep link (adjustTestSchema://)")
        assertUtil.info("Open deferred deep link (adjustTestSchema://)")

        // checking the default values of the first session package
        // should only have one package
        assertUtil.isEqual(1, mockPackageHandler.queue.size)
        val attributionResponseWrongDeeplink =
            ResponseData.buildResponseData(
                mockAttributionHandler.attributionPackage,
                null
            ) as AttributionResponseData
        attributionResponseWrongDeeplink.deeplink = Uri.parse("wrongDeeplink://")
        activityHandler.launchAttributionResponseTasks(attributionResponseWrongDeeplink)
        SystemClock.sleep(1500)
        assertUtil.info("Deferred deeplink received (wrongDeeplink://)")
        assertUtil.error("Unable to open deferred deep link (wrongDeeplink://)")
        assertUtil.notInInfo("Open deferred deep link (wrongDeeplink://)")
    }

    @Test
    fun testLogLevel() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testLogLevel")
        var config: MotrackConfig = getConfig()
        config.setLogLevel(LogLevel.VERBOSE)
        config.setLogLevel(LogLevel.DEBUG)
        config.setLogLevel(LogLevel.INFO)
        config.setLogLevel(LogLevel.WARN)
        config.setLogLevel(LogLevel.ERROR)
        config.setLogLevel(LogLevel.ASSERT)
        assertUtil.test("MockLogger setLogLevel: " + LogLevel.VERBOSE.toString() + ", isProductionEnvironment: false")
        assertUtil.test("MockLogger setLogLevel: " + LogLevel.DEBUG.toString() + ", isProductionEnvironment: false")
        assertUtil.test("MockLogger setLogLevel: " + LogLevel.INFO.toString() + ", isProductionEnvironment: false")
        assertUtil.test("MockLogger setLogLevel: " + LogLevel.WARN.toString() + ", isProductionEnvironment: false")
        assertUtil.test("MockLogger setLogLevel: " + LogLevel.ERROR.toString() + ", isProductionEnvironment: false")
        assertUtil.test("MockLogger setLogLevel: " + LogLevel.ASSERT.toString() + ", isProductionEnvironment: false")
        config.setLogLevel(LogLevel.SUPPRESS)

        // chooses Assert because config object was not configured to allow suppress
        //assertUtil.test("MockLogger setLogLevel: " + LogLevel.ASSERT);
        // changed when log in production was introduced
        assertUtil.test("MockLogger setLogLevel: " + LogLevel.SUPPRESS.toString() + ", isProductionEnvironment: false")

        // init log level with assert because it was not configured to allow suppress
        config = getConfig("production", "123456789012", false, context!!)
        config.setLogLevel(LogLevel.SUPPRESS)

        // chooses Assert because config object was not configured to allow suppress
        //assertUtil.test("MockLogger setLogLevel: " + LogLevel.ASSERT);
        // changed when log in production was introduced
        assertUtil.test("MockLogger setLogLevel: " + LogLevel.SUPPRESS.toString() + ", isProductionEnvironment: true")

        // init with info because it's sandbox
        config = getConfig("sandbox", "123456789012", true, context!!)
        config.setLogLevel(LogLevel.SUPPRESS)
        // chooses SUPPRESS because config object was configured to allow suppress
        assertUtil.test("MockLogger setLogLevel: " + LogLevel.SUPPRESS.toString() + ", isProductionEnvironment: false")

        // init with info because it's sandbox
        config = getConfig("production", "123456789012", true, context!!)
        config.setLogLevel(LogLevel.ASSERT)

        // chooses SUPPRESS because config object was configured to allow suppress
        //assertUtil.test("MockLogger setLogLevel: " + LogLevel.SUPPRESS);
        // changed when log in production was introduced
        assertUtil.test("MockLogger setLogLevel: " + LogLevel.ASSERT.toString() + ", isProductionEnvironment: true")
    }

    @Test
    fun testNotLaunchDeeplinkCallback() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testNotLaunchDeeplinkCallback")

        // create the config to start the session
        val config: MotrackConfig = getConfig()
        config.onDeeplinkResponseListener = object : OnDeeplinkResponseListener {
            override fun launchReceivedDeeplink(deeplink: Uri?): Boolean {
                mockLogger.test("launchReceivedDeeplink, $deeplink")
                return false
            }
        }

        // start activity handler with config
        val activityHandler: ActivityHandler = startAndCheckFirstSession(config)

        // test attribution response
        val attributionResponseDeeplink =
            ResponseData.buildResponseData(
                mockAttributionHandler.attributionPackage,
                null
            ) as AttributionResponseData
        attributionResponseDeeplink.deeplink = Uri.parse("adjustTestSchema://")
        activityHandler.launchAttributionResponseTasks(attributionResponseDeeplink)
        SystemClock.sleep(1500)

        // received the deferred deeplink
        assertUtil.info("Deferred deeplink received (adjustTestSchema://)")
        // but it did not try to launch it
        assertUtil.test("launchReceivedDeeplink, adjustTestSchema://")
        assertUtil.notInError("Unable to open deferred deep link (adjustTestSchema://)")
        assertUtil.notInInfo("Open deferred deep link (adjustTestSchema://)")
    }

    @Test
    fun testDeeplinkCallback() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testDeeplinkCallback")

        // create the config to start the session
        val config: MotrackConfig = getConfig()
        config.onDeeplinkResponseListener = object : OnDeeplinkResponseListener {
            override fun launchReceivedDeeplink(deeplink: Uri?): Boolean {
                mockLogger.test("launchReceivedDeeplink, $deeplink")
                return true
            }
        }

        // start activity handler with config
        val activityHandler: ActivityHandler = startAndCheckFirstSession(config)

        // set package handler to respond with a valid attribution
        val attributionResponseDeeplink =
            ResponseData.buildResponseData(
                mockAttributionHandler.attributionPackage,
                null
            ) as AttributionResponseData
        attributionResponseDeeplink.deeplink = Uri.parse("adjustTestSchema://")
        activityHandler.launchAttributionResponseTasks(attributionResponseDeeplink)
        SystemClock.sleep(2000)

        // received the deferred deeplink
        assertUtil.info("Deferred deeplink received (adjustTestSchema://)")
        // and it did launch it
        assertUtil.test("launchReceivedDeeplink, adjustTestSchema://")
        assertUtil.notInError("Unable to open deferred deep link (adjustTestSchema://)")
        assertUtil.info("Open deferred deep link (adjustTestSchema://)")
    }

    @Test
    fun testDelayStartSendFirst() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testDelayStartSendFirst")

        // create the config to start the session
        val config: MotrackConfig = getConfig()
        config.delayStart = 5.0

        // start activity handler with config
        val activityHandler = getActivityHandler(config)
        SystemClock.sleep(1500)

        // test init values
        val stateActivityHandlerInit = StateActivityHandlerInit(activityHandler)
        stateActivityHandlerInit.delayStartConfigured = true
        checkInitTests(stateActivityHandlerInit)
        resumeActivity(activityHandler)
        resumeActivity(activityHandler)
        SystemClock.sleep(1000)
        val newStateSession = StateSession(StateSession.SessionType.NEW_SESSION)
        // delay start means it starts paused
        newStateSession.toSend = false
        // sdk click handler does not start paused
        newStateSession.sdkClickHandlerAlsoStartsPaused = false
        // delay configured
        newStateSession.delayStart = "5.0"
        checkStartInternal(newStateSession)

        // change state session for non session
        val nonSession = StateSession(StateSession.SessionType.NONSESSION)
        // delay already processed
        nonSession.delayStart = null
        nonSession.toSend = false
        nonSession.sdkClickHandlerAlsoStartsPaused = false
        nonSession.foregroundTimerAlreadyStarted = true
        checkStartInternal(nonSession)
        activityHandler.sendFirstPackages()
        SystemClock.sleep(3000)
        assertUtil.notInVerbose("Delay Start timer fired")
        activityHandler.internalState?.let { checkSendFirstPackages(true, it, true, false) }
        activityHandler.sendFirstPackages()
        SystemClock.sleep(1000)
        activityHandler.internalState?.let { checkSendFirstPackages(false, it, true, false) }
    }

    @Test
    fun testUpdateAttribution() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testUpdateAttribution")

        // create the config to start the session
        var config: MotrackConfig = getConfig()

        // start activity handler with config
        val firstActivityHandler: ActivityHandler = startAndCheckFirstSession(config)
        val nullJsonObject: JSONObject? = null
        val nullAttribution: MotrackAttribution? =
            MotrackAttribution.fromJson(nullJsonObject, null, null) //XXX

        // check if Attribution wasn't built
        assertUtil.isNull(nullAttribution)

        // check that it does not update a null attribution
        assertUtil.isFalse(firstActivityHandler.updateAttributionI(nullAttribution))

        // create an empty attribution
        var emptyJsonResponse: JSONObject? = null
        try {
            emptyJsonResponse = JSONObject("{ }")
        } catch (e: JSONException) {
            assertUtil.fail(e.message!!)
        }
        var emptyAttribution: MotrackAttribution? =
            MotrackAttribution.fromJson(emptyJsonResponse, null, null) // XXX

        // check that updates attribution
        assertUtil.isTrue(firstActivityHandler.updateAttributionI(emptyAttribution))
        assertUtil.debug("Wrote Attribution: tt:null tn:null net:null cam:null adg:null cre:null cl:null")
        emptyAttribution = MotrackAttribution.fromJson(emptyJsonResponse, null, null) // XXX

        // test first session package
        val firstSessionPackage = mockPackageHandler.queue[0]

        // simulate a session response with attribution data
        val sessionResponseDataWithAttribution =
            ResponseData.buildResponseData(firstSessionPackage, null) as SessionResponseData
        sessionResponseDataWithAttribution.attribution = emptyAttribution

        // check that it does not update the attribution
        firstActivityHandler.launchSessionResponseTasks(sessionResponseDataWithAttribution)
        SystemClock.sleep(1000)
        assertUtil.notInDebug("Wrote Attribution")

        // end session
        firstActivityHandler.onPause()
        SystemClock.sleep(1000)
        checkEndSession()

        // create the new config
        config = getConfig()
        config.onAttributionChangedListener = object : OnAttributionChangedListener {
            override fun onAttributionChanged(attribution: MotrackAttribution?) {
                mockLogger.test("restartActivityHandler onAttributionChanged: $attribution")
            }
        }
        val restartActivityHandler = getActivityHandler(config)
        SystemClock.sleep(1500)
        val restartActivityHandlerInit = StateActivityHandlerInit(restartActivityHandler)
        restartActivityHandlerInit.activityStateAlreadyCreated = true
        restartActivityHandlerInit.readActivityState = "ec:0 sc:1 ssc:1"
        restartActivityHandlerInit.readAttribution =
            "tt:null tn:null net:null cam:null adg:null cre:null cl:null"

        // test init values
        checkInitTests(restartActivityHandlerInit)
        resumeActivity(restartActivityHandler)
        SystemClock.sleep(2000)
        val firstRestart = StateSession(StateSession.SessionType.NEW_SUBSESSION)
        firstRestart.subsessionCount = 2
        firstRestart.foregroundTimerAlreadyStarted = false
        checkStartInternal(firstRestart)

        // check that it does not update the attribution after the restart
        assertUtil.isFalse(restartActivityHandler.updateAttributionI(emptyAttribution))
        assertUtil.notInDebug("Wrote Attribution")

        // new attribution
        var firstAttributionJson: JSONObject? = null
        try {
            firstAttributionJson = JSONObject(
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
            assertUtil.fail((e.message)!!)
        }
        val firstAttribution: MotrackAttribution? =
            MotrackAttribution.fromJson(firstAttributionJson, null, null) // XXX

        //check that it updates
        sessionResponseDataWithAttribution.attribution = firstAttribution
        restartActivityHandler.launchSessionResponseTasks(sessionResponseDataWithAttribution)
        SystemClock.sleep(1000)
        assertUtil.debug("Wrote Attribution: tt:ttValue tn:tnValue net:nValue cam:cpValue adg:aValue cre:ctValue cl:clValue")
        assertUtil.test("restartActivityHandler onAttributionChanged: tt:ttValue tn:tnValue net:nValue cam:cpValue adg:aValue cre:ctValue cl:clValue")

        // test first session package
        val attributionPackage: ActivityPackage = mockAttributionHandler.attributionPackage
        // simulate a session response with attribution data
        val attributionResponseDataWithAttribution =
            ResponseData.buildResponseData(attributionPackage, null) as AttributionResponseData
        attributionResponseDataWithAttribution.attribution = firstAttribution
        // check that it does not update the attribution
        restartActivityHandler.launchAttributionResponseTasks(attributionResponseDataWithAttribution)
        SystemClock.sleep(1000)
        assertUtil.notInDebug("Wrote Attribution")

        // end session
        restartActivityHandler.onPause()
        SystemClock.sleep(1000)
        checkEndSession()
        config = getConfig()
        config.onAttributionChangedListener = object : OnAttributionChangedListener {
            override fun onAttributionChanged(attribution: MotrackAttribution?) {
                mockLogger.test("secondRestartActivityHandler onAttributionChanged: $attribution")
            }
        }
        val secondRestartActivityHandler = getActivityHandler(config)
        SystemClock.sleep(1500)
        val secondrestartActivityHandlerInit =
            StateActivityHandlerInit(secondRestartActivityHandler)
        secondrestartActivityHandlerInit.activityStateAlreadyCreated = true
        secondrestartActivityHandlerInit.readActivityState = "ec:0 sc:1 ssc:2"
        secondrestartActivityHandlerInit.readAttribution =
            "tt:ttValue tn:tnValue net:nValue cam:cpValue adg:aValue cre:ctValue cl:clValue"
        SystemClock.sleep(2000)

        // test init values
        checkInitTests(secondrestartActivityHandlerInit)
        resumeActivity(secondRestartActivityHandler)
        SystemClock.sleep(2000)
        val secondRestart = StateSession(StateSession.SessionType.NEW_SUBSESSION)
        secondRestart.subsessionCount = 3
        secondRestart.foregroundTimerAlreadyStarted = false
        checkStartInternal(secondRestart)

        // check that it does not update the attribution after the restart
        assertUtil.isFalse(secondRestartActivityHandler.updateAttributionI(firstAttribution))
        assertUtil.notInDebug("Wrote Attribution")

        // new attribution
        var secondAttributionJson: JSONObject? = null
        try {
            secondAttributionJson = JSONObject(
                ("{ " +
                        "\"tracker_token\" : \"ttValue2\" , " +
                        "\"tracker_name\"  : \"tnValue2\" , " +
                        "\"network\"       : \"nValue2\" , " +
                        "\"campaign\"      : \"cpValue2\" , " +
                        "\"adgroup\"       : \"aValue2\" , " +
                        "\"creative\"      : \"ctValue2\" , " +
                        "\"click_label\"   : \"clValue2\" }")
            )
        } catch (e: JSONException) {
            assertUtil.fail((e.message)!!)
        }
        val secondAttribution: MotrackAttribution? =
            MotrackAttribution.fromJson(secondAttributionJson, null, null) // XXX

        //check that it updates
        attributionResponseDataWithAttribution.attribution = secondAttribution
        secondRestartActivityHandler.launchAttributionResponseTasks(
            attributionResponseDataWithAttribution
        )
        SystemClock.sleep(2000)
        assertUtil.debug("Wrote Attribution: tt:ttValue2 tn:tnValue2 net:nValue2 cam:cpValue2 adg:aValue2 cre:ctValue2 cl:clValue2")

        // check that it launch the saved attribute
        assertUtil.test("secondRestartActivityHandler onAttributionChanged: tt:ttValue2 tn:tnValue2 net:nValue2 cam:cpValue2 adg:aValue2 cre:ctValue2 cl:clValue2")

        // check that it does not update the attribution
        assertUtil.isFalse(secondRestartActivityHandler.updateAttributionI(secondAttribution))
        assertUtil.notInDebug("Wrote Attribution")
    }

    @Test
    fun testOfflineMode() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testOfflineMode")

        // adjust the session intervals for testing
        MotrackFactory.sessionInterval = 2000
        MotrackFactory.subsessionInterval = 500

        // create the config to start the session
        val config: MotrackConfig = getConfig()

        // start activity handler with config
        val activityHandler = getActivityHandler(config)

        // put SDK offline
        activityHandler.setOfflineMode(true)
        SystemClock.sleep(1000)
        val internalState: ActivityHandler.InternalState? = activityHandler.internalState

        // check if it's offline before the sdk starts
        assertUtil.isTrue(internalState!!.isOffline)
        SystemClock.sleep(1500)

        // not writing activity state because it set enable does not start the sdk
        assertUtil.notInDebug("Wrote Activity state")

        // test init values
        checkInitTests(activityHandler)

        // check if message the disable of the SDK
        assertUtil.info("Handlers will start paused due to SDK being offline")

        // check change from set offline mode
        checkHandlerStatus(true)

        // start the sdk
        // foreground timer starts because it's offline, not disabled
        resumeActivity(activityHandler)
        SystemClock.sleep(2500)

        // test first session start
        val firstSessionStartPaused = StateSession(StateSession.SessionType.NEW_SESSION)
        firstSessionStartPaused.toSend = false
        firstSessionStartPaused.foregroundTimerStarts = true

        // check session that is paused
        checkStartInternal(firstSessionStartPaused)
        stopActivity(activityHandler)
        SystemClock.sleep(1500)

        // test end session of offline
        val stateOfflineEndSession = StateEndSession()
        stateOfflineEndSession.checkOnPause = true
        stateOfflineEndSession.updateActivityState = false // update too late on the session
        checkEndSession(stateOfflineEndSession)

        // disable the SDK in the background
        activityHandler.setEnabled(false)
        SystemClock.sleep(1000)

        // check that it is disabled
        assertUtil.isFalse(activityHandler.isEnabled())

        // writing activity state after disabling
        assertUtil.debug("Wrote Activity state: ec:0 sc:1 ssc:1")

        // check if message the disable of the SDK
        assertUtil.info("Pausing handlers due to SDK being disabled")

        // start a session while offline and disabled
        SystemClock.sleep(2500)

        // check handler status update of disable
        checkHandlerStatus(true)

        // try to start new session disabled
        resumeActivity(activityHandler)
        SystemClock.sleep(1500)

        // session not created for being disabled
        // foreground timer does not start because it's offline, not disabled
        val sessionDisabled = StateSession(StateSession.SessionType.DISABLED)
        sessionDisabled.toSend = false
        sessionDisabled.foregroundTimerStarts = false
        sessionDisabled.disabled = true
        checkStartInternal(sessionDisabled)

        // put SDK back online
        activityHandler.setOfflineMode(false)
        SystemClock.sleep(1500)
        assertUtil.info("Handlers remain paused")

        // test the update status, still paused
        checkHandlerStatus(true)

        // try to do activities while SDK disabled
        resumeActivity(activityHandler)
        activityHandler.trackEvent(MotrackEvent("event1"))
        SystemClock.sleep(2500)

        // check that timer was not executed
        checkForegroundTimerFired(false)

        // session not created for being disabled
        // foreground timer does not start because it's offline, not disabled
        checkStartInternal(sessionDisabled)

        // end the session
        stopActivity(activityHandler)
        SystemClock.sleep(1000)
        val stateDisableEndSession = StateEndSession()
        stateDisableEndSession.checkOnPause = true
        stateDisableEndSession.foregroundAlreadySuspended = true // did not start timer disabled
        stateDisableEndSession.updateActivityState = false // update too late on the session
        checkEndSession(stateDisableEndSession)

        // enable the SDK again
        activityHandler.setEnabled(true)
        SystemClock.sleep(1000)

        // check that is enabled
        assertUtil.isTrue(activityHandler.isEnabled())
        assertUtil.debug("Wrote Activity state")
        assertUtil.info("Resuming handlers due to SDK being enabled")
        SystemClock.sleep(2500)

        // it is still paused because it's on the background
        checkHandlerStatus(true)
        resumeActivity(activityHandler)
        SystemClock.sleep(1500)
        val secondSessionState = StateSession(StateSession.SessionType.NEW_SESSION)
        secondSessionState.sessionCount = 2
        // test that is not paused anymore
        checkStartInternal(secondSessionState)
    }

    private fun checkForegroundTimerFired(timerFired: Boolean) {
        // timer fired
        if (timerFired) {
            assertUtil.verbose("Foreground timer fired")
        } else {
            assertUtil.notInVerbose("Foreground timer fired")
        }
    }

    private fun checkSendFirstPackages(
        delayStart: Boolean,
        internalState: ActivityHandler.InternalState,
        activityStateCreated: Boolean,
        pausing: Boolean
    ) {
        if (!delayStart) {
            assertUtil.info("Start delay expired or never configured")
            // did not update package
            assertUtil.notInTest("PackageHandler updatePackages")
            return
        }
        assertUtil.notInInfo("Start delay expired or never configured")

        // update packages
        checkUpdatePackages(internalState, activityStateCreated)

        // no longer is in delay start
        assertUtil.isFalse(internalState.isInDelayedStart)

        // cancel timer
        assertUtil.verbose("Delay Start timer canceled")
        checkHandlerStatus(pausing, false, false)
    }

    @Test
    fun testSendReferrer() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testSendReferrer")

        // create the config to start the session
        val config: MotrackConfig = getConfig()
        val now = System.currentTimeMillis()
        val referrerBeforeLaunch = "referrerBeforeLaunch"
        val settings =
            context!!.getSharedPreferences(Constants.PREFERENCES_NAME, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString(Constants.REFERRER_PREFKEY, referrerBeforeLaunch)
        editor.putLong(Constants.REFERRER_CLICKTIME_PREFKEY, now)
        editor.apply()

        // start activity handler with config
        val activityHandler = getActivityHandler(config)
        SystemClock.sleep(1500)

        // test init values
        val stateActivityHandlerInit = StateActivityHandlerInit(activityHandler)
        //stateActivityHandlerInit.sendReferrer = referrerBeforeLaunch;
        checkInitTests(stateActivityHandlerInit)
        resumeActivity(activityHandler)
        SystemClock.sleep(1500)

        // test session
        checkFirstSession()
        val reftag = "motrack_reftag=referrerValue"
        val extraParams = "motrack_foo=bar&other=stuff&motrack_key=value"
        val mixed = "motrack_foo=bar&other=stuff&motrack_reftag=referrerValue"
        val encodedSeparators =
            "motrack_foo=b%26a%3B%3Dr&motrack_reftag=referrer%3DValue%26&other=stuff"
        val empty = ""
        val nullString: String? = null
        val single = "motrack_foo"
        val prefix = "motrack_=bar"
        val incomplete = "motrack_foo="
        activityHandler.sendReferrer(reftag, now)
        activityHandler.sendReferrer(extraParams, now)
        activityHandler.sendReferrer(mixed, now)
        activityHandler.sendReferrer(encodedSeparators, now)
        activityHandler.sendReferrer(empty, now)
        activityHandler.sendReferrer(nullString, now)
        activityHandler.sendReferrer(single, now)
        activityHandler.sendReferrer(prefix, now)
        activityHandler.sendReferrer(incomplete, now)
        SystemClock.sleep(2000)
        assertUtil.verbose("Referrer to parse (%s)", reftag)
        assertUtil.test("SdkClickHandler sendSdkClick")
        assertUtil.verbose("Referrer to parse (%s)", extraParams)
        assertUtil.test("SdkClickHandler sendSdkClick")
        assertUtil.verbose("Referrer to parse (%s)", mixed)
        assertUtil.test("SdkClickHandler sendSdkClick")
        assertUtil.verbose("Referrer to parse (%s)", encodedSeparators)
        assertUtil.test("SdkClickHandler sendSdkClick")
        assertUtil.verbose("Referrer to parse (%s)", single)
        assertUtil.test("SdkClickHandler sendSdkClick")
        assertUtil.verbose("Referrer to parse (%s)", prefix)
        assertUtil.test("SdkClickHandler sendSdkClick")
        assertUtil.verbose("Referrer to parse (%s)", incomplete)
        assertUtil.test("SdkClickHandler sendSdkClick")

        // 7 click
        assertUtil.isEqual(8, mockSdkClickHandler.queue.size())
        val referrerBeforeLaunchPacakge = mockSdkClickHandler.queue[0]
        val referrerBeforeLaunchTest = TestActivityPackage(referrerBeforeLaunchPacakge)
        referrerBeforeLaunchTest.referrer = referrerBeforeLaunch
        referrerBeforeLaunchTest.testClickPackage("reftag", true)
        val reftagClickPackage = mockSdkClickHandler.queue[1]
        val reftagClickPackageTest = TestActivityPackage(reftagClickPackage)
        reftagClickPackageTest.reftag = "referrerValue"
        reftagClickPackageTest.referrer = reftag
        reftagClickPackageTest.testClickPackage("reftag")

        // get the click package
        val extraParamsClickPackage = mockSdkClickHandler.queue[2]

        // create activity package test
        val testExtraParamsClickPackage = TestActivityPackage(extraParamsClickPackage)

        // other deep link parameters
        testExtraParamsClickPackage.otherParameters = "{\"key\":\"value\",\"foo\":\"bar\"}"
        testExtraParamsClickPackage.referrer = extraParams

        // test the second deeplink
        testExtraParamsClickPackage.testClickPackage("reftag")

        // get the click package
        val mixedClickPackage = mockSdkClickHandler.queue[3]

        // create activity package test
        val testMixedClickPackage = TestActivityPackage(mixedClickPackage)
        testMixedClickPackage.reftag = "referrerValue"
        testMixedClickPackage.referrer = mixed

        // other deep link parameters
        testMixedClickPackage.otherParameters = "{\"foo\":\"bar\"}"

        // test the third deeplink
        testMixedClickPackage.testClickPackage("reftag")

        // get the click package
        val encodedClickPackage = mockSdkClickHandler.queue[4]

        // create activity package test
        val testEncodedClickPackage = TestActivityPackage(encodedClickPackage)
        testEncodedClickPackage.reftag = "referrer=Value&"
        testEncodedClickPackage.referrer = encodedSeparators

        // other deep link parameters
        testEncodedClickPackage.otherParameters = "{\"foo\":\"b&a;=r\"}"

        // test the third deeplink
        testMixedClickPackage.testClickPackage("reftag")

        // get the click package
        val singleClickPackage = mockSdkClickHandler.queue[5]

        // create activity package test
        val testSingleClickPackage = TestActivityPackage(singleClickPackage)
        testSingleClickPackage.referrer = single
        testSingleClickPackage.testClickPackage("reftag")

        // get the click package
        val prefixClickPackage = mockSdkClickHandler.queue[6]

        // create activity package test
        val testPrefixClickPackage = TestActivityPackage(prefixClickPackage)
        testPrefixClickPackage.referrer = prefix
        testPrefixClickPackage.testClickPackage("reftag")

        // get the click package
        val incompleteClickPackage = mockSdkClickHandler.queue[7]

        // create activity package test
        val testIncompleteClickPackage = TestActivityPackage(incompleteClickPackage)
        testIncompleteClickPackage.referrer = incomplete
        testIncompleteClickPackage.testClickPackage("reftag")
    }

    @Test
    fun testSessionParameters() {
        // assert test name to read better in logcat
        mockLogger.assert("TestActivityHandler testSessionParameters")

        // create the config to start the session
        val config: MotrackConfig = getConfig()

        //  create handler and start the first session
        config.preLaunchActions = ArrayList<IRunActivityHandler>()
        config.preLaunchActions.add(object : IRunActivityHandler {
            override fun run(activityHandler: ActivityHandler?) {
                //
                activityHandler?.let {
                    it.addSessionCallbackParameterI("cKey", "cValue")
                    it.addSessionCallbackParameterI("cFoo", "cBar")
                    it.addSessionCallbackParameterI("cKey", "cValue2")
                    it.resetSessionCallbackParametersI()
                    it.addSessionCallbackParameterI("cKey", "cValue")
                    it.addSessionCallbackParameterI("cFoo", "cBar")
                    it.removeSessionCallbackParameterI("cKey")
                    //
                    it.addSessionPartnerParameterI("pKey", "pValue")
                    it.addSessionPartnerParameterI("pFoo", "pBar")
                    it.addSessionPartnerParameterI("pKey", "pValue2")
                    it.resetSessionPartnerParametersI()
                    it.addSessionPartnerParameterI("pKey", "pValue")
                    it.addSessionPartnerParameterI("pFoo", "pBar")
                    it.removeSessionPartnerParameterI("pKey")
                }

            }
        })
        var activityHandler: ActivityHandler? = getActivityHandler(config)
        SystemClock.sleep(1500)

        // test init values
        checkInitTests(activityHandler!!)
        checkSessionParameters()
        resumeActivity(activityHandler)
        val firstEvent = MotrackEvent("event1")
        activityHandler.trackEvent(firstEvent)
        SystemClock.sleep(1500)

        // test session
        checkFirstSession()
        val stateEvent1 = StateEvent()
        checkEvent(stateEvent1)

        // 1 session + 1 event
        assertUtil.isEqual(2, mockPackageHandler.queue.size)

        // get the session package
        val firstSessionPackage = mockPackageHandler.queue[0]

        // create event package test
        val testFirstSessionPackage = TestActivityPackage(firstSessionPackage)

        // set event test parameters
        testFirstSessionPackage.callbackParams = "{\"cFoo\":\"cBar\"}"
        testFirstSessionPackage.partnerParams = "{\"pFoo\":\"pBar\"}"
        testFirstSessionPackage.testSessionPackage(1)

        // get the event
        val firstEventPackage = mockPackageHandler.queue[1]

        // create event package test
        val testFirstEventPackage = TestActivityPackage(firstEventPackage)

        // set event test parameters
        testFirstEventPackage.eventCount = "1"
        testFirstEventPackage.suffix = "'event1'"
        testFirstEventPackage.callbackParams = "{\"cFoo\":\"cBar\"}"
        testFirstEventPackage.partnerParams = "{\"pFoo\":\"pBar\"}"
        testFirstEventPackage.testEventPackage("event1")

        // end current session
        stopActivity(activityHandler)
        SystemClock.sleep(1000)
        checkEndSession()
        activityHandler.teardown()
        activityHandler = null
        val newConfig: MotrackConfig = getConfig()
        val restartActivityHandler = getActivityHandler(newConfig)
        SystemClock.sleep(1500)

        // start new one
        // delay start not configured because activity state is already created
        val restartActivityHandlerInit = StateActivityHandlerInit(restartActivityHandler)
        restartActivityHandlerInit.activityStateAlreadyCreated = true
        restartActivityHandlerInit.readActivityState = "ec:1 sc:1"
        restartActivityHandlerInit.readCallbackParameters = "{cFoo=cBar}"
        restartActivityHandlerInit.readPartnerParameters = "{pFoo=pBar}"

        // test init values
        checkInitTests(restartActivityHandlerInit)
        resumeActivity(restartActivityHandler)
        SystemClock.sleep(1500)
        val stateRestartSession = StateSession(StateSession.SessionType.NEW_SUBSESSION)
        stateRestartSession.subsessionCount = 2
        stateRestartSession.eventCount = 1
        checkStartInternal(stateRestartSession)

        // create the second Event
        val secondEvent = MotrackEvent("event2")
        secondEvent.addCallbackParameter("ceFoo", "ceBar")
        secondEvent.addPartnerParameter("peFoo", "peBar")
        restartActivityHandler.trackEvent(secondEvent)
        val thirdEvent = MotrackEvent("event3")
        thirdEvent.addCallbackParameter("cFoo", "ceBar")
        thirdEvent.addPartnerParameter("pFoo", "peBar")
        restartActivityHandler.trackEvent(thirdEvent)
        SystemClock.sleep(1500)

        // 2 events
        assertUtil.isEqual(2, mockPackageHandler.queue.size)

        // get the event
        val secondEventPackage = mockPackageHandler.queue[0]

        // create event package test
        val testSecondEventPackage = TestActivityPackage(secondEventPackage)

        // set event test parameters
        testSecondEventPackage.eventCount = "2"
        testSecondEventPackage.suffix = "'event2'"
        testSecondEventPackage.callbackParams = "{\"ceFoo\":\"ceBar\",\"cFoo\":\"cBar\"}"
        testSecondEventPackage.partnerParams = "{\"peFoo\":\"peBar\",\"pFoo\":\"pBar\"}"
        testSecondEventPackage.testEventPackage("event2")

        // get the event
        val thirdEventPackage = mockPackageHandler.queue[1]

        // create event package test
        val testThirdEventPackage = TestActivityPackage(thirdEventPackage)

        // set event test parameters
        testThirdEventPackage.eventCount = "3"
        testThirdEventPackage.suffix = "'event3'"
        testThirdEventPackage.callbackParams = "{\"cFoo\":\"ceBar\"}"
        testThirdEventPackage.partnerParams = "{\"pFoo\":\"peBar\"}"
        testThirdEventPackage.testEventPackage("event3")
    }


    private fun checkInitAndStart(
        activityHandler: ActivityHandler,
        initState: StateActivityHandlerInit,
        stateSession: StateSession
    ) {
        checkInitTests(initState)
        resumeActivity(activityHandler)
        SystemClock.sleep(1500)
        checkStartInternal(stateSession)
    }

    private fun checkEvent(stateEvent: StateEvent) {
        if (stateEvent.disabled) {
            assertUtil.notInInfo("Skipping duplicated order ID ")
            assertUtil.notInVerbose("Added order ID ")
            assertUtil.notInTest("PackageHandler addPackage")
            assertUtil.notInInfo("Buffered event ")
            assertUtil.notInTest("PackageHandler sendFirstPackage")
            return
        }
        if (stateEvent.duplicatedOrderId) {
            // dropping duplicate transaction id
            assertUtil.info("Skipping duplicated order ID '" + stateEvent.orderId.toString() + "'")
            // check that event package was not added
            assertUtil.notInTest("PackageHandler addPackage")
            return
        }
        if (stateEvent.orderId != null) {
            // check order id was added
            assertUtil.verbose("Added order ID '" + stateEvent.orderId.toString() + "'")
        } else {
            // check order id was not added
            assertUtil.notInVerbose("Added order ID")
        }

        // check that event package was added
        assertUtil.test("PackageHandler addPackage")
        if (stateEvent.bufferedSuffix != null) {
            // check that event was buffered
            assertUtil.info("Buffered event " + stateEvent.bufferedSuffix)

            // and not sent to package handler
            assertUtil.notInTest("PackageHandler sendFirstPackage")
        } else {
            // check that event was sent to package handler
            assertUtil.test("PackageHandler sendFirstPackage")
            // and not buffered
            assertUtil.notInInfo("Buffered event")
        }
        if (stateEvent.backgroundTimerStarts != null) {
            // does not fire background timer
            assertUtil.verbose("Background timer starting. Launching in " + stateEvent.backgroundTimerStarts.toString() + ".0 seconds")
        } else {
            // does not fire background timer
            assertUtil.notInVerbose("Background timer starting")
        }

        // after tracking the event it should write the activity state
        if (stateEvent.activityStateSuffix != null) {
            assertUtil.debug("Wrote Activity state: " + stateEvent.activityStateSuffix)
        } else {
            assertUtil.debug("Wrote Activity state")
        }
    }


    private fun startAndCheckFirstSession(config: MotrackConfig): ActivityHandler {
        // start activity handler with config
        val activityHandler: ActivityHandler = getActivityHandler(config)
        SystemClock.sleep(1500)
        startAndCheckFirstSession(activityHandler)
        return activityHandler
    }

    private fun startAndCheckFirstSession(activityHandler: ActivityHandler) {
        // test init values
        checkInitTests(activityHandler)
        resumeActivity(activityHandler)
        SystemClock.sleep(1500)

        // test session
        checkFirstSession()
    }

    private fun checkFirstSession() {
        val newStateSession = StateSession(StateSession.SessionType.NEW_SESSION)
        checkStartInternal(newStateSession)
    }

    private fun checkSubSession(
        sessionCount: Int,
        subsessionCount: Int,
        getAttributionIsCalled: Boolean
    ) {
        val subSessionState = StateSession(StateSession.SessionType.NEW_SUBSESSION)
        subSessionState.sessionCount = sessionCount
        subSessionState.subsessionCount = subsessionCount
        subSessionState.getAttributionIsCalled = getAttributionIsCalled
        subSessionState.foregroundTimerAlreadyStarted = true
        checkStartInternal(subSessionState)
    }

    private fun checkFurtherSessions(sessionCount: Int, getAttributionIsCalled: Boolean) {
        val subSessionState = StateSession(StateSession.SessionType.NEW_SESSION)
        subSessionState.sessionCount = sessionCount
        subSessionState.getAttributionIsCalled = getAttributionIsCalled
        subSessionState.foregroundTimerAlreadyStarted = true
        checkStartInternal(subSessionState)
    }

    private fun checkSessionParameters() {
        //
        assertUtil.debug("Wrote Session Callback parameters: {cKey=cValue}")
        assertUtil.debug("Wrote Session Callback parameters: {cKey=cValue, cFoo=cBar}")
        assertUtil.warn("Key cKey will be overwritten")
        assertUtil.debug("Wrote Session Callback parameters: {cKey=cValue2, cFoo=cBar}")
        assertUtil.debug("Wrote Session Callback parameters: null")
        assertUtil.debug("Wrote Session Callback parameters: {cKey=cValue}")
        assertUtil.debug("Wrote Session Callback parameters: {cKey=cValue, cFoo=cBar}")
        assertUtil.debug("Key cKey will be removed")
        assertUtil.debug("Wrote Session Callback parameters: {cFoo=cBar}")

        //
        assertUtil.debug("Wrote Session Partner parameters: {pKey=pValue}")
        assertUtil.debug("Wrote Session Partner parameters: {pKey=pValue, pFoo=pBar}")
        assertUtil.warn("Key pKey will be overwritten")
        assertUtil.debug("Wrote Session Partner parameters: {pKey=pValue2, pFoo=pBar}")
        assertUtil.debug("Wrote Session Partner parameters: null")
        assertUtil.debug("Wrote Session Partner parameters: {pKey=pValue}")
        assertUtil.debug("Wrote Session Partner parameters: {pKey=pValue, pFoo=pBar}")
        assertUtil.debug("Key pKey will be removed")
        assertUtil.debug("Wrote Session Partner parameters: {pFoo=pBar}")
    }

    private fun checkStartInternal(stateSession: StateSession) {
        // check delay start
        checkDelayStart(stateSession)

        // check onResume
        checkOnResume(stateSession)

        // update Handlers Status
        checkHandlerStatus(
            if (stateSession.disabled) null else !stateSession.toSend,
            stateSession.eventBufferingIsEnabled,
            stateSession.sdkClickHandlerAlsoStartsPaused
        )
        when (stateSession.sessionType) {
            StateSession.SessionType.NEW_SESSION -> {
                // if the package was build, it was sent to the Package Handler
                assertUtil.test("PackageHandler addPackage")

                // after adding, the activity handler ping the Package handler to send the package
                assertUtil.test("PackageHandler sendFirstPackage")

                // writes activity state
                assertUtil.debug(
                    "Wrote Activity state: " +
                            "ec:" + stateSession.eventCount + " sc:" + stateSession.sessionCount + " ssc:" + stateSession.subsessionCount
                )
            }
            StateSession.SessionType.NEW_SUBSESSION -> {
                // test the subsession message
                assertUtil.verbose("Started subsession " + stateSession.subsessionCount.toString() + " of session " + stateSession.sessionCount)
                // writes activity state
                assertUtil.debug(
                    ("Wrote Activity state: " +
                            "ec:" + stateSession.eventCount + " sc:" + stateSession.sessionCount + " ssc:" + stateSession.subsessionCount)
                )
            }
            StateSession.SessionType.NONSESSION -> {
                // stopped for a short time, not enough for a new sub subsession
                assertUtil.verbose("Time span since last activity too short for a new subsession")
                // does not writes activity state
                assertUtil.notInDebug("Wrote Activity state: ")
            }
            StateSession.SessionType.TIME_TRAVEL -> {
                assertUtil.error("Time travel!")
                // writes activity state
                assertUtil.debug(
                    ("Wrote Activity state: " +
                            "ec:" + stateSession.eventCount + " sc:" + stateSession.sessionCount + " ssc:" + stateSession.subsessionCount)
                )
            }
            StateSession.SessionType.DISABLED -> {
                assertUtil.notInTest("PackageHandler addPackage")
                assertUtil.notInVerbose("Started subsession ")
                assertUtil.notInVerbose("Time span since last activity too short for a new subsession")
                assertUtil.notInError("Time travel!")
            }
        }

        /*
        // after processing the session, writes the activity state
        if (stateSession.sessionType != stateSession.sessionType.NONSESSION &&
                stateSession.sessionType != stateSession.sessionType.DISABLED)
        {
            assertUtil.debug("Wrote Activity state: " +
                    "ec:" + stateSession.eventCount + " sc:" + stateSession.sessionCount + " ssc:" + stateSession.subsessionCount);
        } else {

        }
        */
        // check Attribution State
        if (stateSession.getAttributionIsCalled != null) {
            if (stateSession.getAttributionIsCalled!!) {
                assertUtil.test("AttributionHandler getAttribution")
            } else {
                assertUtil.notInTest("AttributionHandler getAttribution")
            }
        }
    }

    private fun resumeActivity(activityHandler: ActivityHandler) {
        // start activity
        activityHandler.onResume()
        val internalState: ActivityHandler.InternalState = activityHandler.internalState!!

        // comes to the foreground
        assertUtil.isTrue(internalState.isInForeground())
    }

    private fun checkDelayStart(stateSession: StateSession) {
        if (stateSession.delayStart == null) {
            assertUtil.notInWarn("Waiting")
            return
        }
        if (stateSession.delayStart.equals("10.1")) {
            assertUtil.warn("Delay start of 10.1 seconds bigger than max allowed value of 10.0 seconds")
            stateSession.delayStart = "10.0"
        }
        assertUtil.info("Waiting " + stateSession.delayStart.toString() + " seconds before starting first session")
        assertUtil.verbose("Delay Start timer starting. Launching in " + stateSession.delayStart.toString() + " seconds")
        if (stateSession.activityStateAlreadyCreated) {
            assertUtil.verbose("Wrote Activity state")
        }
    }

    private fun checkOnResume(stateSession: StateSession) {
        if (!stateSession.startSubsession) {
            assertUtil.notInVerbose("Background timer canceled")
            assertUtil.notInVerbose("Foreground timer is already started")
            assertUtil.notInVerbose("Foreground timer starting")
            assertUtil.notInVerbose("Subsession start")
            return
        }
        // TODO check delay start

        // stops background timer
        if (stateSession.sendInBackgroundConfigured) {
            assertUtil.verbose("Background timer canceled")
        } else {
            assertUtil.notInVerbose("Background timer canceled")
        }

        // start foreground timer
        if (stateSession.foregroundTimerStarts) {
            if (stateSession.foregroundTimerAlreadyStarted) {
                assertUtil.verbose("Foreground timer is already started")
            } else {
                assertUtil.verbose("Foreground timer starting")
            }
        } else {
            assertUtil.notInVerbose("Foreground timer is already started")
            assertUtil.notInVerbose("Foreground timer starting")
        }

        // starts the subsession
        assertUtil.verbose("Subsession start")
    }

    private fun checkHandlerStatus(pausing: Boolean) {
        checkHandlerStatus(pausing, false, true)
    }

    private fun checkHandlerStatus(
        pausing: Boolean?,
        eventBufferingEnabled: Boolean,
        sdkClickHandlerAlsoPauses: Boolean
    ) {
        if (pausing == null) {
            assertUtil.notInTest("AttributionHandler pauseSending")
            assertUtil.notInTest("PackageHandler pauseSending")
            assertUtil.notInTest("SdkClickHandler pauseSending")
            assertUtil.notInTest("AttributionHandler resumeSending")
            assertUtil.notInTest("PackageHandler resumeSending")
            assertUtil.notInTest("SdkClickHandler resumeSending")
            return
        }
        if (pausing) {
            assertUtil.test("AttributionHandler pauseSending")
            assertUtil.test("PackageHandler pauseSending")
            if (sdkClickHandlerAlsoPauses) {
                assertUtil.test("SdkClickHandler pauseSending")
            } else {
                assertUtil.test("SdkClickHandler resumeSending")
            }
        } else {
            assertUtil.test("AttributionHandler resumeSending")
            assertUtil.test("PackageHandler resumeSending")
            assertUtil.test("SdkClickHandler resumeSending")
            if (!eventBufferingEnabled) {
                assertUtil.test("PackageHandler sendFirstPackage")
            }
        }
    }


    private fun getActivityHandler(config: MotrackConfig): ActivityHandler {
        val activityHandler = ActivityHandler.getInstance(config)
        activityHandler.let {
            assertUtil.test("MockLogger lockLogLevel")
            val internalState: ActivityHandler.InternalState? = activityHandler.internalState
            // test default values
            internalState?.let {
                assertUtil.isTrue(it.isEnabled)
                assertUtil.isTrue(it.isOnline())
                assertUtil.isTrue(it.isInBackground)
                assertUtil.isTrue(it.isNotInDelayedStart())
                assertUtil.isFalse(it.itHasToUpdatePackages())
            }

        }
        return activityHandler
    }

    private fun checkInitTests(activityHandler: ActivityHandler) {
        val stateActivityHandlerInit = StateActivityHandlerInit(activityHandler)
        checkInitTests(stateActivityHandlerInit)
    }

    private fun checkInitTests(sInit: StateActivityHandlerInit) {
        checkReadFile(sInit.readAttribution, "Attribution")
        checkReadFile(sInit.readActivityState, "Activity state")
        checkReadFile(sInit.readCallbackParameters, "Session Callback parameters")
        checkReadFile(sInit.readPartnerParameters, "Session Partner parameters")

        // check values read from activity state
        assertUtil.isEqual(sInit.internalState!!.isEnabled, sInit.startEnabled)
        //assertUtil.isEqual(sInit.internalState.itHasToUpdatePackages(), sInit.updatePackages);

        // check event buffering
        if (sInit.eventBufferingIsEnabled) {
            assertUtil.info("Event buffering is enabled")
        } else {
            assertUtil.notInInfo("Event buffering is enabled")
        }

        // check Google play is set
        assertUtil.info("Google Play Services Advertising ID read correctly at start time")

        // check default tracker
        if (sInit.defaultTracker != null) {
            assertUtil.info("Default tracker: '%s'", sInit.defaultTracker)
        } else {
            assertUtil.notInInfo("Default tracker: ")
        }

        // check push token
        if (sInit.pushToken != null) {
            assertUtil.info("Push token: '%s'", sInit.pushToken)
        } else {
            assertUtil.notInInfo("Push token: ")
        }

        // check foreground timer was created
        assertUtil.verbose(
            "Foreground timer configured to fire after " + sInit.foregroundTimerStart
                .toString() + ".0 seconds of starting and cycles every " + sInit.foregroundTimerCycle.toString() + ".0 seconds"
        )

        // check background timer was created
        if (sInit.sendInBackgroundConfigured) {
            assertUtil.info("Send in background configured")
        } else {
            assertUtil.notInInfo("Send in background configured")
        }
        if (sInit.delayStartConfigured) {
            assertUtil.info("Delay start configured")
            assertUtil.isTrue(sInit.internalState!!.isInDelayedStart)
        } else {
            assertUtil.notInInfo("Delay start configured")
            assertUtil.isFalse(sInit.internalState!!.isInDelayedStart)
        }
        if (sInit.startsSending) {
            assertUtil.test("PackageHandler init, startsSending: true")
            assertUtil.test("AttributionHandler init, startsSending: true")
            assertUtil.test("SdkClickHandler init, startsSending: true")
        } else {
            assertUtil.test("PackageHandler init, startsSending: false")
            assertUtil.test("AttributionHandler init, startsSending: false")
            if (sInit.sdkClickHandlerAlsoStartsPaused) {
                assertUtil.test("SdkClickHandler init, startsSending: false")
            } else {
                assertUtil.test("SdkClickHandler init, startsSending: true")
            }
        }
        if (sInit.updatePackages) {
            checkUpdatePackages(sInit.internalState!!, sInit.activityStateAlreadyCreated)
        } else {
            assertUtil.notInTest("PackageHandler updatePackages")
        }
        if (sInit.sendReferrer != null) {
            assertUtil.verbose("Referrer to parse (${sInit.sendReferrer})")
            assertUtil.test("SdkClickHandler sendSdkClick")
        } else {
            assertUtil.notInVerbose("Referrer to parse ")
            assertUtil.notInTest("SdkClickHandler sendSdkClick")
        }
    }

    private fun checkReadFile(fileLog: String?, objectName: String) {
        if (fileLog == null) {
            assertUtil.debug("$objectName file not found")
        } else {
            assertUtil.debug("Read $objectName: $fileLog")
        }
    }

    private fun getConfig(): MotrackConfig {
        return getConfig("sandbox", "123456789012", false, context!!)
    }

    private fun getConfig(
        environment: String,
        appToken: String,
        allowSupressLogLevel: Boolean,
        context: Context
    ): MotrackConfig {
        val motrackConfig: MotrackConfig = if (allowSupressLogLevel) {
            MotrackConfig(context, appToken, environment, allowSupressLogLevel)
        } else {
            MotrackConfig(context, appToken, environment)
        }

        if (environment === "sandbox") {
            assertUtil.test("MockLogger setLogLevel: " + LogLevel.INFO.toString() + ", isProductionEnvironment: " + false)
            assertUtil.warn("SANDBOX: Motrack is running in Sandbox mode. Use this setting for testing. Don't forget to set the environment to `production` before publishing!")
        } else if (environment === "production" && !allowSupressLogLevel) {
            assertUtil.test("MockLogger setLogLevel: " + LogLevel.INFO.toString() + ", isProductionEnvironment: " + true)
            assertUtil.warn("PRODUCTION: Motrack is running in Production mode. Use this setting only for the build that you want to publish. Set the environment to `sandbox` if you want to test your app!")
        } else if (environment === "production" && allowSupressLogLevel) {
            assertUtil.test("MockLogger setLogLevel: " + LogLevel.SUPPRESS.toString() + ", isProductionEnvironment: " + true)
            assertUtil.warn("PRODUCTION: Motrack is running in Production mode. Use this setting only for the build that you want to publish. Set the environment to `sandbox` if you want to test your app!")
        } else {
            assertUtil.fail()
        }
        return motrackConfig
    }

    private fun stopActivity(activityHandler: ActivityHandler) {
        // stop activity
        activityHandler.onPause()
        val internalState: ActivityHandler.InternalState? = activityHandler.internalState

        // goes to the background

        internalState?.let {
            assertUtil.isTrue(it.isInBackground)
        }
    }

    private fun checkEndSession() {
        val stateEndSession = StateEndSession()
        checkEndSession(stateEndSession)
    }

    private fun checkEndSession(stateEndSession: StateEndSession) {
        if (stateEndSession.checkOnPause) {
            checkOnPause(
                stateEndSession.foregroundAlreadySuspended,
                stateEndSession.backgroundTimerStarts
            )
        }
        if (stateEndSession.pausing) {
            checkHandlerStatus(stateEndSession.pausing, stateEndSession.eventBufferingEnabled, true)
        }
        if (stateEndSession.updateActivityState) {
            assertUtil.debug("Wrote Activity state: ")
        } else {
            assertUtil.notInDebug("Wrote Activity state: ")
        }
    }

    private fun checkOnPause(
        foregroundAlreadySuspended: Boolean,
        backgroundTimerStarts: Boolean
    ) {
        // stop foreground timer
        if (foregroundAlreadySuspended) {
            assertUtil.verbose("Foreground timer is already suspended")
        } else {
            assertUtil.verbose("Foreground timer suspended")
        }

        // start background timer
        if (backgroundTimerStarts) {
            assertUtil.verbose("Background timer starting.")
        } else {
            assertUtil.notInVerbose("Background timer starting.")
        }

        // starts the subsession
        assertUtil.verbose("Subsession end")
    }

    private fun checkUpdatePackages(
        internalState: ActivityHandler.InternalState,
        activityStateCreated: Boolean
    ) {
        // update packages
        assertUtil.test("PackageHandler updatePackages")
        assertUtil.isFalse(internalState.updatePackages)
        if (activityStateCreated) {
            assertUtil.debug("Wrote Activity state")
        } else {
            assertUtil.notInDebug("Wrote Activity state")
        }
    }
}