package com.motrack.sdk

import com.motrack.sdk.network.IActivityPackageSender
import java.util.ArrayList

/**
 * @author yaya (@yahyalmh)
 * @since 20th November 2021
 */

class MockSdkClickHandler(private val testLogger: MockLogger): ISdkClickHandler {
    private val prefix = "SdkClickHandler "
    var queue: ArrayList<ActivityPackage> = ArrayList()
    var activityHandler: IActivityHandler? = null

    override fun init(
        activityHandler: IActivityHandler?,
        startsSending: Boolean,
        sdkClickHandlerActivityPackageSender: IActivityPackageSender
    ) {
        this.activityHandler = activityHandler
        testLogger.test(prefix + "init, startsSending: " + startsSending)
    }

    override fun pauseSending() {
        testLogger.test(prefix + "pauseSending")
    }

    override fun resumeSending() {
        testLogger.test(prefix + "resumeSending")
    }

    override fun sendSdkClick(sdkClick: ActivityPackage) {
        testLogger.test(prefix + "sendSdkClick")
        queue.add(sdkClick)
    }

    override fun sendReftagReferrers() {
        testLogger.test(prefix + "sendReftagReferrers")
    }

    override fun sendPreinstallPayload(payload: String?, location: String?) {
        testLogger.test(prefix + "sendPreinstallPayload")
    }

    override fun teardown() {
        testLogger.test(prefix + "teardown")
    }
}