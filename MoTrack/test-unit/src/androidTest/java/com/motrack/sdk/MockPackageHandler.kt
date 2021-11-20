package com.motrack.sdk

import android.content.Context
import com.motrack.sdk.network.IActivityPackageSender
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 20th November 2021
 */

class MockPackageHandler(private val testLogger: MockLogger) : IPackageHandler {
    private val prefix = "PackageHandler "
    var activityHandler: IActivityHandler? = null
    lateinit var queue: ArrayList<ActivityPackage>
    var context: Context? = null

    override fun init(
        activityHandler: IActivityHandler?,
        context: Context?,
        startsSending: Boolean,
        packageHandlerActivityPackageSender: IActivityPackageSender?
    ) {
        testLogger.test(prefix + "init, startsSending: " + startsSending)
        this.activityHandler = activityHandler
        this.context = context
        queue = ArrayList()
    }

    override fun addPackage(activityPackage: ActivityPackage) {
        testLogger.test(prefix + "addPackage")
        queue.add(activityPackage)
    }

    override fun sendFirstPackage() {
        testLogger.test(prefix + "sendFirstPackage")
        /*
        if (activityHandler != null) {
            activityHandler.finishedTrackingActivity(jsonResponse);
        }
        */
    }

    fun sendNextPackage(responseData: ResponseData) {
        testLogger.test(prefix + "sendNextPackage, " + responseData)
    }

    fun closeFirstPackage(responseData: ResponseData, activityPackage: ActivityPackage) {
        testLogger.test(prefix + "closeFirstPackage, responseData" + responseData)
        testLogger.test(prefix + "closeFirstPackage, activityPackage" + activityPackage)
    }

    override fun pauseSending() {
        testLogger.test(prefix + "pauseSending")
    }

    override fun resumeSending() {
        testLogger.test(prefix + "resumeSending")
    }

    override fun updatePackages(sessionParameters: SessionParameters?) {
        testLogger.test(prefix + "updatePackages, sessionParameters" + sessionParameters)
    }

    override fun flush() {
        testLogger.test(prefix + "flush state")
    }

    override fun teardown() {
        testLogger.test(prefix + "teardown deleteState, ")
    }

}