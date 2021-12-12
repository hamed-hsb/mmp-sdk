package com.motrack.sdk

import com.motrack.sdk.network.IActivityPackageSender

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

interface ISdkClickHandler {
    /**
     * Initialise SdkClickHandler instance.
     *
     * @param activityHandler Activity handler instance.
     * @param startsSending   Is sending paused?
     */
    fun init(
        activityHandler: IActivityHandler?,
        startsSending: Boolean,
        sdkClickHandlerActivityPackageSender: IActivityPackageSender
    )

    /**
     * Pause sending from SdkClickHandler.
     */
    fun pauseSending()

    /**
     * Resume sending from SdkClickHandler.
     */
    fun resumeSending()

    /**
     * Send sdk_click package.
     *
     * @param sdkClick sdk_click package to be sent.
     */
    fun sendSdkClick(sdkClick: ActivityPackage)

    /**
     * Send sdk_click packages made from all the persisted intent type referrers.
     */
    fun sendReftagReferrers()

    /**
     * Send sdk_click package carrying preinstall info.
     */
    fun sendPreinstallPayload(payload: String?, location: String?)

    /**
     * Teardown SdkClickHandler instance.
     */
    fun teardown()
}