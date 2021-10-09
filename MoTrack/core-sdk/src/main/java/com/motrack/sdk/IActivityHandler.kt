package com.motrack.sdk

import android.content.Context
import android.net.Uri
import org.json.JSONObject

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

interface IActivityHandler {
    fun init(motrackConfig: MotrackConfig)

    fun onResume()

    fun onPause()

    fun trackEvent(event: MotrackEvent)

    fun finishingTrackingActivity(respondData: RespondData)

    fun setEnabled(enabled: Boolean)

    fun isEnabled(): Boolean

    fun readOpenUrl(url: Uri, clickTime: Long)

    fun updateAttributionI(attribution: MotrackAttribution)

    fun launchEventResponseTasks(eventResponseData: EventResponseData)

    fun launchSessionResponseTasks(sessionResponseData: SessionResponseData)

    fun launchSdkClickResponseTasks(sdkClickResponseData: SdkClickResponseData)

    fun launchAttributionResponseTasks(attributionResponseData: AttributionResponseData)

    fun sendRefTagReferrer()

    fun sendPreinstallReferrer()

    fun sendInstallReferrer(referrerDetails: ReferrerDetails, referrerApi: String)

    fun setOfflineMode(enabled: Boolean)

    fun setAskingAttribution(askingAttribution: Boolean)

    fun sendFirstPackage()

    fun addSessionCallbackParameter(key: String?, value: String?)

    fun addSessionPartnerParameter(key: String?, value: String?)

    fun removeSessionCallbackParameter(key: String?)

    fun removeSessionPartnerParameter(key: String?)

    fun resetSessionCallbackParameters()

    fun resetSessionPartnerParameters()

    fun teardown()

    fun setPushToken(token: String, preSaved: Boolean)

    fun gdprForgetMe()

    fun disableThirdPartySharing()

    fun trackThirdPartySharing(motrackThirdPlaySharing: MotrackThirdPlaySharing)

    fun trackMeasurementConsent(consentMeasurement: Boolean)

    fun trackAdRevenue(source: String, adRevenueJson: JSONObject)

    fun trackAdRevenue(motrackAdRevenue: MotrackAdRevenue)

    fun trackPlayStoreSubscription(motrackPlayStoreSubscription: MotrackPlayStoreSubscription)

    fun gotOptOutResponse()

    fun getContext(): Context

    fun getAdid(): String

    fun getAttribution(): MotrackAttribution

    fun getAdjustConfig(): MotrackConfig

    fun getDeviceInfo(): DeviceInfo

    fun getActivityState(): ActivityState

    fun getSessionParameters(): SessionParameters

}