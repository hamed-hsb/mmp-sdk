package com.motrack.sdk

import com.motrack.sdk.network.IActivityPackageSender

/**
 * @author yaya (@yahyalmh)
 * @since 20th November 2021
 */

class MockAttributionHandler(private val testLogger: MockLogger) : IAttributionHandler {
    private val prefix = "AttributionHandler "
    lateinit var activityHandler: IActivityHandler
    lateinit var attributionPackage: IActivityPackageSender
    var lastSessionResponseData: SessionResponseData? = null
    var lastSdkClickResponseData: SdkClickResponseData? = null

    override fun init(
        activityHandler: IActivityHandler,
        startsSending: Boolean,
        attributionHandlerActivityPackageSender: IActivityPackageSender
    ) {
        testLogger.test(prefix + "init, startsSending: " + startsSending)
        this.activityHandler = activityHandler
        this.attributionPackage = attributionHandlerActivityPackageSender
    }

    override fun getAttribution() {
        testLogger.test(prefix + "getAttribution")
    }

    override fun checkSessionResponse(sessionResponseData: SessionResponseData) {
        testLogger.test(prefix + "checkSessionResponse")
        lastSessionResponseData = sessionResponseData
    }

    override fun checkSdkClickResponse(sdkClickResponseData: SdkClickResponseData) {
        testLogger.test(prefix + "checkSdkClickResponse")
        lastSdkClickResponseData = sdkClickResponseData
    }

    override fun pauseSending() {
        testLogger.test(prefix + "pauseSending")
    }

    override fun resumeSending() {
        testLogger.test(prefix + "resumeSending")
    }

    override fun teardown() {
        testLogger.test(prefix + "teardown")
    }
}