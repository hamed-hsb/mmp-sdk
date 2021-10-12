package com.motrack.sdk

import com.motrack.sdk.network.IActivityPackageSender

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

interface IAttributionHandler {
    fun init(
        activityHandler: IActivityHandler,
        startsSending: Boolean,
        attributionHandlerActivityPackageSender: IActivityPackageSender
    )

    fun checkSessionResponse(sessionResponseData: SessionResponseData)
    fun checkSdkClickResponse(sdkClickResponseData: SdkClickResponseData)
    fun pauseSending()
    fun resumeSending()
    fun getAttribution()
    fun teardown()
}