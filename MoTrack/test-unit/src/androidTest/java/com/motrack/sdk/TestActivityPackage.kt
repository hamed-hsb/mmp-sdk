package com.motrack.sdk

import com.google.gson.JsonParser
import junit.framework.Assert

/**
 * @author yaya (@yahyalmh)
 * @since 20th November 2021
 */

class TestActivityPackage(private val activityPackage: ActivityPackage) {
    var parameters: HashMap<String, String>? = activityPackage.parameters
    var appToken: String = "123456789012"
    var environment: String = "sandbox"
    var clientSdk: String = "android4.28.7"
    var deviceKnow: Boolean? = null
    var playServices = true
    var eventBufferingEnabled = false
    var pushToken: String? = null

    // session
    var sessionCount: Int? = null
    var defaultTracker: String? = null
    var subsessionCount: Int? = null

    // event
    var eventToken: String? = null
    var eventCount: String? = null
    var suffix: String = ""
    var revenueString: String? = null
    var currency: String? = null
    var callbackParams: String? = null
    var partnerParams: String? = null
    var savedCallbackParameters: Map<String, String>? = null
    var savedPartnerParameters: Map<String, String>? = null

    // click
    var reftag: String? = null
    var otherParameters: String? = null
    var attribution: MotrackAttribution = MotrackAttribution()
    var referrer: String? = null
    var deeplink: String? = null

    fun testSessionPackage(sessionCount: Int) {
        // set the session count
        this.sessionCount = sessionCount

        // test default package attributes
        testDefaultAttributes("/session", ActivityKind.SESSION, "session")

        // check default parameters
        testDefaultParameters()

        // session parameters
        // last_interval
        if (sessionCount == 1) {
            assertParameterNull("last_interval")
        } else {
            assertParameterNotNull("last_interval")
        }
        // default_tracker
        assertParameterEquals("default_tracker", defaultTracker)
        // callback_params
        assertJsonParameterEquals("callback_params", callbackParams)
        // partner_params
        assertJsonParameterEquals("partner_params", partnerParams)
    }

    fun testEventPackage(eventToken: String?) {
        // set the event token
        this.eventToken = eventToken

        // test default package attributes
        testDefaultAttributes("/event", ActivityKind.EVENT, "event")

        // check default parameters
        testDefaultParameters()

        // event parameters
        // event_count
        if (eventCount == null) {
            assertParameterNotNull("event_count")
        } else {
            assertParameterEquals("event_count", eventCount)
        }
        // event_token
        assertParameterEquals("event_token", eventToken)
        // revenue and currency must come together
        if (parameters!!["revenue"] != null &&
            parameters!!["currency"] == null
        ) {
            assertFail()
        }
        if (parameters!!["revenue"] == null &&
            parameters!!["currency"] != null
        ) {
            assertFail()
        }
        // revenue
        assertParameterEquals("revenue", revenueString)
        // currency
        assertParameterEquals("currency", currency)
        // callback_params
        assertJsonParameterEquals("callback_params", callbackParams)
        // partner_params
        assertJsonParameterEquals("partner_params", partnerParams)

        // saved callback parameters
        if (savedCallbackParameters == null) {
            Assert.assertNull(
                activityPackage.getExtendedString(),
                activityPackage.callbackParameters
            )
        } else {
            Assert.assertTrue(
                activityPackage.getExtendedString(),
                savedCallbackParameters == activityPackage.callbackParameters
            )
        }
        // saved partner parameters
        if (savedPartnerParameters == null) {
            Assert.assertNull(
                activityPackage.getExtendedString(),
                activityPackage.partnerParameters
            )
        } else {
            Assert.assertTrue(
                activityPackage.getExtendedString(),
                savedPartnerParameters == activityPackage.partnerParameters
            )
        }
    }

    fun testInfoPackage(source: String) {
        // test default package attributes
        testDefaultAttributes("/sdk_info", ActivityKind.INFO, "info")

        // check device ids parameters
        testDeviceIdsParameters()

        // click parameters
        // source
        assertParameterEquals("source", source)
        if (source === Constants.PUSH) {
        } else {
            assertFail()
        }
    }

    fun testAttributionPackage() {
        // test default package attributes
        testDefaultAttributes("attribution", ActivityKind.ATTRIBUTION, "attribution")
        testDeviceIdsParameters()
    }

    fun testQueryStringRequest(queryStringRequest: String, queueSize: Int?) {
        val queryPairs = queryStringRequest.split("&".toRegex()).toTypedArray()
        var wasSentAtFound = false
        var wasQueueSizeFound = false
        var queueSizeFound = -1
        for (pair in queryPairs) {
            val pairComponents = pair.split("=".toRegex()).toTypedArray()
            val key = pairComponents[0]
            val value = pairComponents[1]
            if (key == "sent_at") {
                wasSentAtFound = true
            }
            if (key == "queue_size") {
                wasQueueSizeFound = true
                queueSizeFound = value.toInt()
            }
        }
        if (!wasSentAtFound) {
            Assert.fail(queryStringRequest)
        }
        if (queueSize == null) {
            Assert.assertFalse(queryStringRequest, wasQueueSizeFound)
            return
        }
        if (!wasQueueSizeFound) {
            Assert.fail(queryStringRequest)
        } else {
            Assert.assertEquals(
                queryStringRequest,
                queueSize.toInt(), queueSizeFound
            )
        }
    }

    fun testClickPackage(source: String) {
        testClickPackage(source, true)
    }

    fun testClickPackage(source: String, hasActivityState: Boolean) {
        // test default package attributes
        testDefaultAttributes("/sdk_click", ActivityKind.CLICK, "click")

        // check device ids parameters
        testDefaultParameters(hasActivityState)

        // click parameters
        // source
        assertParameterEquals("source", source)

        // referrer
        assertParameterEquals(Constants.REFTAG, reftag)
        if (source === Constants.REFTAG) {
            assertParameterEquals("referrer", referrer)
            assertParameterNull("deeplink")
        } else if (source === Constants.DEEPLINK) {
            assertParameterEquals("deeplink", deeplink)
            assertParameterNull("referrer")
        } else {
            assertFail()
        }

        // params
        assertJsonParameterEquals("params", otherParameters)

        // click_time
        // TODO add string click time to compare
        assertParameterNotNull("click_time")

        // attributions
        // tracker
        assertParameterEquals("tracker", attribution.trackerName)
        // campaign
        assertParameterEquals("campaign", attribution.campaign)
        // adgroup
        assertParameterEquals("adgroup", attribution.adgroup)
        // creative
        assertParameterEquals("creative", attribution.creative)
    }

    private fun testDefaultAttributes(
        path: String,
        activityKind: ActivityKind,
        activityKindString: String
    ) {
        // check the Sdk version is being tested
        activityPackage.clientSdk?.let { assertEquals(it, clientSdk) }
        // check the path
        activityPackage.path?.let { assertEquals(it, path) }
        // test activity kind
        // check the activity kind
        assertEquals(activityPackage.activityKind, activityKind)
        // the conversion from activity kind to String
        assertEquals(activityPackage.activityKind.toString(), activityKindString)
        // the conversion from String to activity kind
        assertEquals(activityPackage.activityKind, ActivityKind.fromString(activityKindString))
        // test suffix
        activityPackage.suffix?.let { assertEquals(it, suffix) }
    }

    private fun testDefaultParameters() {
        testDefaultParameters(true)
    }

    private fun testDefaultParameters(hasActivityState: Boolean) {
        testDeviceInfo()
        testConfig()
        if (hasActivityState) {
            testActivityState()
        } else {
            testWithoutActivityState()
        }
        testCreatedAt()
    }

    private fun testDeviceIdsParameters() {
        testDeviceInfoIds()
        testConfig()
        testCreatedAt()
    }

    private fun testCreatedAt() {
        // created_at
        assertParameterNotNull("created_at")
    }

    private fun testDeviceInfo() {
        testDeviceInfoIds()
        // fb_id
        //assertParameterNotNull("fb_id");
        // package_name
        assertParameterNotNull("package_name")
        // app_version
        // device_type
        assertParameterNotNull("device_type")
        // device_name
        assertParameterNotNull("device_name")
        // device_manufacturer
        assertParameterNotNull("device_manufacturer")
        // os_name
        assertParameterEquals("os_name", "android")
        // os_version
        assertParameterNotNull("os_version")
        // language
        assertParameterNotNull("language")
        // country
        assertParameterNotNull("country")
        // screen_size
        assertParameterNotNull("screen_size")
        // screen_format
        assertParameterNotNull("screen_format")
        // screen_density
        assertParameterNotNull("screen_density")
        // display_width
        assertParameterNotNull("display_width")
        // display_height
        assertParameterNotNull("display_height")
        // hardware_name
        assertParameterNotNull("hardware_name")
        // cpu_type
        assertParameterNotNull("cpu_type")
        // os_build
        assertParameterNotNull("os_build")
        // vm_isa
        assertParameterNotNull("vm_isa")
    }

    private fun testDeviceInfoIds() {
        // play services
        if (playServices) {
            // mac_sha1
            assertParameterNull("mac_sha1")
            // mac_md5
            assertParameterNull("mac_md5")
            // android_id
            assertParameterNull("android_id")
        } else {
            // mac_sha1
            assertParameterNotNull("mac_sha1")
            // mac_md5
            assertParameterNotNull("mac_md5")
            // android_id
            assertParameterNotNull("android_id")
        }
    }

    private fun testConfig() {
        // app_token
        assertParameterEquals("app_token", appToken)
        // environment
        assertParameterEquals("environment", environment)
        // device_known
        testParameterBoolean("device_known", deviceKnow)
        // needs_attribution_data
        testParameterBoolean("needs_response_details", true)
        // play services
        if (playServices) {
            // gps_adid
            assertParameterNotNull("gps_adid")
            // tracking_enabled
            assertParameterNotNull("tracking_enabled")
        } else {
            // gps_adid
            assertParameterNull("gps_adid")
            // tracking_enabled
            assertParameterNull("tracking_enabled")
        }
        // event_buffering_enabled
        testParameterBoolean("event_buffering_enabled", eventBufferingEnabled)
        // push_token
        assertParameterEquals("push_token", pushToken)
    }

    private fun testWithoutActivityState() {
        // android_uuid
        assertParameterNull("android_uuid")
        // session_count
        assertParameterNull("session_count")
        // subsession_count
        assertParameterNull("subsession_count")
        // session_length
        assertParameterNull("session_length")
        // time_spent
        assertParameterNull("time_spent")
    }

    private fun testActivityState() {
        testActivityStateIds()
        // session_count
        if (sessionCount == null) {
            assertParameterNotNull("session_count")
        } else {
            assertParameterEquals("session_count", sessionCount!!)
        }
        // first session
        if (sessionCount != null && sessionCount == 1) {
            // subsession_count
            assertParameterNull("subsession_count")
            // session_length
            assertParameterNull("session_length")
            // time_spent
            assertParameterNull("time_spent")
        } else {
            // subsession_count
            if (subsessionCount == null) assertParameterNotNull("subsession_count") else assertParameterEquals(
                "subsession_count",
                subsessionCount!!
            )
            // session_length
            assertParameterNotNull("session_length")
            // time_spent
            assertParameterNotNull("time_spent")
        }
    }

    private fun testActivityStateIds() {
        // android_uuid
        assertParameterNotNull("android_uuid")
    }

    private fun assertParameterNotNull(parameterName: String) {
        Assert.assertNotNull(
            activityPackage.getExtendedString(),
            parameters!![parameterName]
        )
    }

    private fun assertParameterNull(parameterName: String) {
        Assert.assertNull(
            activityPackage.getExtendedString(),
            parameters!![parameterName]
        )
    }

    private fun assertParameterEquals(parameterName: String, value: String?) {
        if (value == null) {
            assertParameterNull(parameterName)
            return
        }
        Assert.assertEquals(
            activityPackage.getExtendedString(),
            value, parameters!![parameterName]
        )
    }

    private fun assertJsonParameterEquals(parameterName: String, value: String?) {
        if (value == null) {
            assertParameterNull(parameterName)
            return
        }
        val parser = JsonParser()
        val e1 = parser.parse(value)
        val e2 = parser.parse(parameters!![parameterName])
        assertEquals(e1, e2)
    }

    private fun assertParameterEquals(parameterName: String, value: Int) {
        Assert.assertEquals(
            activityPackage.getExtendedString(),
            value, parameters!![parameterName]!!.toInt()
        )
    }


    private fun assertEquals(field: String, value: String) {
        Assert.assertEquals(
            activityPackage.getExtendedString(),
            value, field
        )
    }

    private fun assertEquals(field: Any, value: Any) {
        Assert.assertEquals(
            activityPackage.getExtendedString(),
            value, field
        )
    }

    private fun assertFail() {
        Assert.fail(activityPackage.getExtendedString())
    }

    private fun testParameterBoolean(parameterName: String, value: Boolean?) {
        when (value) {
            null -> assertParameterNull(parameterName)
            true -> assertParameterEquals(parameterName, "1")
            else -> assertParameterEquals(parameterName, "0")
        }
    }
}