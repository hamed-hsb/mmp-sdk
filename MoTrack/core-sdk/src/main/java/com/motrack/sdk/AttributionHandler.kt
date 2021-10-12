package com.motrack.sdk

import android.net.Uri
import com.motrack.sdk.network.IActivityPackageSender
import com.motrack.sdk.scheduler.SingleThreadCachedScheduler
import com.motrack.sdk.scheduler.ThreadScheduler
import com.motrack.sdk.scheduler.TimerOnce
import java.lang.ref.WeakReference

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

class AttributionHandler(
    activityHandler: IActivityHandler?,
    startsSending: Boolean,
    attributionHandlerActivityPackageSender: IActivityPackageSender
) : IAttributionHandler, IActivityPackageSender.ResponseDataCallbackSubscriber {

    companion object {
        private const val ATTRIBUTION_TIMER_NAME = "Attribution timer"

    }

    private var paused = false
    private var lastInitiatedBy: String? = null
    private var activityPackageSender: IActivityPackageSender? = null

    private var logger: ILogger? = null
    private var timer: TimerOnce? = null
    private var scheduler: ThreadScheduler? = null
    private var activityHandlerWeakRef: WeakReference<IActivityHandler>? = null

    init {
        logger = MotrackFactory.getLogger()
        scheduler = SingleThreadCachedScheduler("AttributionHandler")
        timer = TimerOnce({ sendAttributionRequest() }, ATTRIBUTION_TIMER_NAME)
        init(activityHandler!!, startsSending, attributionHandlerActivityPackageSender)
    }

    override fun init(
        activityHandler: IActivityHandler,
        startsSending: Boolean,
        attributionHandlerActivityPackageSender: IActivityPackageSender
    ) {
        activityHandlerWeakRef = WeakReference(activityHandler)
        paused = !startsSending
        activityPackageSender = attributionHandlerActivityPackageSender
    }

    private fun sendAttributionRequest() {
        scheduler!!.submit { sendAttributionRequestI() }
    }

    private fun sendAttributionRequestI() {
        if (activityHandlerWeakRef!!.get()!!.getActivityState().isGdprForgotten) {
            return
        }
        if (paused) {
            logger!!.debug("Attribution handler is paused")
            return
        }

        // Create attribution package before sending attribution request.
        val attributionPackage: ActivityPackage = buildAndGetAttributionPackage()
        logger!!.verbose(attributionPackage.getExtendedString())
        val sendingParameters: HashMap<String, String> = generateSendingParametersI()
        activityPackageSender!!.sendActivityPackage(
            attributionPackage,
            sendingParameters,
            this
        )
    }

    private fun buildAndGetAttributionPackage(): ActivityPackage {
        val now = System.currentTimeMillis()
        val activityHandler = activityHandlerWeakRef!!.get()
        val packageBuilder = PackageBuilder(
            activityHandler!!.getMotrackConfig(),
            activityHandler.getDeviceInfo(),
            activityHandler.getActivityState(),
            activityHandler.getSessionParameters(),
            now
        )
        val activityPackage: ActivityPackage =
            packageBuilder.buildAttributionPackage(lastInitiatedBy)
        lastInitiatedBy = null
        return activityPackage
    }

    override fun teardown() {
        logger!!.verbose("AttributionHandler teardown")
        if (timer != null) {
            timer!!.teardown()
        }
        if (scheduler != null) {
            scheduler!!.teardown()
        }
        if (activityHandlerWeakRef != null) {
            activityHandlerWeakRef!!.clear()
        }
        timer = null
        logger = null
        scheduler = null
        activityHandlerWeakRef = null
    }

    override fun getAttribution() {
        scheduler!!.submit {
            lastInitiatedBy = "sdk"
            getAttributionI(0)
        }
    }

    override fun checkSessionResponse(sessionResponseData: SessionResponseData) {
        scheduler!!.submit(Runnable {
            val activityHandler = activityHandlerWeakRef!!.get() ?: return@Runnable
            checkSessionResponseI(activityHandler, sessionResponseData)
        })
    }

    override fun checkSdkClickResponse(sdkClickResponseData: SdkClickResponseData) {
        scheduler!!.submit(Runnable {
            val activityHandler = activityHandlerWeakRef!!.get() ?: return@Runnable
            checkSdkClickResponseI(activityHandler, sdkClickResponseData)
        })
    }

    fun checkAttributionResponse(attributionResponseData: AttributionResponseData) {
        scheduler!!.submit(Runnable {
            val activityHandler = activityHandlerWeakRef!!.get() ?: return@Runnable
            checkAttributionResponseI(activityHandler, attributionResponseData)
        })
    }

    override fun pauseSending() {
        paused = true
    }

    override fun resumeSending() {
        paused = false
    }

    private fun getAttributionI(delayInMilliseconds: Long) {
        // Don't reset if new time is shorter than last one.
        if (timer!!.getFireIn() > delayInMilliseconds) {
            return
        }
        if (delayInMilliseconds != 0L) {
            val waitTimeSeconds = delayInMilliseconds / 1000.0
            val secondsString = Util.SecondsDisplayFormat.format(waitTimeSeconds)
            logger!!.debug("Waiting to query attribution in $secondsString seconds")
        }

        // Set the new time the timer will fire in.
        timer!!.startIn(delayInMilliseconds)
    }

    private fun checkAttributionI(activityHandler: IActivityHandler, responseData: ResponseData) {
        if (responseData.jsonResponse == null) {
            return
        }
        val timerMilliseconds =
            responseData.askIn // responseData.jsonResponse.optLong("ask_in", -1);
        if (timerMilliseconds != null && timerMilliseconds >= 0) {
            activityHandler.setAskingAttribution(true)
            lastInitiatedBy = "backend"
            getAttributionI(timerMilliseconds)
            return
        }
        activityHandler.setAskingAttribution(false)
    }

    private fun checkSessionResponseI(
        activityHandler: IActivityHandler,
        sessionResponseData: SessionResponseData
    ) {
        checkAttributionI(activityHandler, sessionResponseData)
        activityHandler.launchSessionResponseTasks(sessionResponseData)
    }

    private fun checkSdkClickResponseI(
        activityHandler: IActivityHandler,
        sdkClickResponseData: SdkClickResponseData
    ) {
        checkAttributionI(activityHandler, sdkClickResponseData)
        activityHandler.launchSdkClickResponseTasks(sdkClickResponseData)
    }

    private fun checkAttributionResponseI(
        activityHandler: IActivityHandler,
        attributionResponseData: AttributionResponseData
    ) {
        checkAttributionI(activityHandler, attributionResponseData)
        checkDeeplinkI(attributionResponseData)
        activityHandler.launchAttributionResponseTasks(attributionResponseData)
    }

    private fun checkDeeplinkI(attributionResponseData: AttributionResponseData) {
        if (attributionResponseData.jsonResponse == null) {
            return
        }
        val attributionJson = attributionResponseData.jsonResponse!!.optJSONObject("attribution")
            ?: return
        val deeplinkString = attributionJson.optString("deeplink", null) ?: return
        attributionResponseData.deeplink = Uri.parse(deeplinkString)
    }

    private fun generateSendingParametersI(): HashMap<String, String> {
        val sendingParameters = java.util.HashMap<String, String>()
        val now = System.currentTimeMillis()
        val dateString = Util.dateFormatter.format(now)
        PackageBuilder.addString(sendingParameters, "sent_at", dateString)
        return sendingParameters
    }


    override fun onResponseDataCallback(responseData: ResponseData) {
        scheduler!!.submit(Runnable {
            val activityHandler = activityHandlerWeakRef!!.get() ?: return@Runnable
            if (responseData.trackingState === TrackingState.OPTED_OUT) {
                activityHandler.gotOptOutResponse()
                return@Runnable
            }
            if (responseData !is AttributionResponseData) {
                return@Runnable
            }
            checkAttributionResponseI(activityHandler, responseData)
        })
    }
}