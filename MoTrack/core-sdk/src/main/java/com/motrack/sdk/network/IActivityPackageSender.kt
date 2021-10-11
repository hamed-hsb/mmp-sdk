package com.motrack.sdk.network

import com.motrack.sdk.ActivityPackage
import com.motrack.sdk.ResponseData

/**
 * @author yaya (@yahyalmh)
 * @since 11th October 2021
 */

interface IActivityPackageSender {
    interface ResponseDataCallbackSubscriber {
        fun onResponseDataCallback(responseData: ResponseData?)
    }

    fun sendActivityPackage(
        activityPackage: ActivityPackage,
        sendingParameters: Map<String?, String?>,
        responseCallback: ResponseDataCallbackSubscriber
    )

    fun sendActivityPackageSync(
        activityPackage: ActivityPackage,
        sendingParameters: Map<String?, String?>
    ): ResponseData
}