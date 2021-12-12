package com.motrack.sdk

import android.content.Context
import com.motrack.sdk.network.IActivityPackageSender

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

interface IPackageHandler {
    fun init(
        activityHandler: IActivityHandler?,
        context: Context?,
        startsSending: Boolean,
        packageHandlerActivityPackageSender: IActivityPackageSender?
    )

    fun addPackage(activityPackage: ActivityPackage)

    fun sendFirstPackage()

    fun pauseSending()

    fun resumeSending()

    fun updatePackages(sessionParameters: SessionParameters?)

    fun flush()

    fun teardown()
}