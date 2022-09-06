package com.motrack.sdk

import com.motrack.sdk.network.IActivityPackageSender
import com.motrack.sdk.scheduler.SingleThreadCachedScheduler
import com.motrack.sdk.scheduler.ThreadScheduler
import org.json.JSONException
import java.lang.ref.WeakReference
import java.util.*


class SdkClickHandler(
    activityHandler: IActivityHandler?,
    startsSending: Boolean,
    sdkClickHandlerActivityPackageSender: IActivityPackageSender
) : ISdkClickHandler {
    /**
     * Indicates whether SdkClickHandler is paused or not.
     */
    private var paused = false

    /**
     * MoTrack logger.
     */
    private var logger: ILogger?

    /**
     * Backoff strategy.
     */
    private var backoffStrategy: BackoffStrategy?

    /**
     * Sending queue.
     */
    private var packageQueue: MutableList<ActivityPackage>? = null

    /**
     * Custom actions scheduled executor.
     */
    private var scheduler: ThreadScheduler?

    /**
     * ActivityHandler instance.
     */
    private var activityHandlerWeakRef: WeakReference<IActivityHandler?>? = null
    private var activityPackageSender: IActivityPackageSender? = null

    /**
     * {@inheritDoc}
     */
    override fun init(
        activityHandler: IActivityHandler?,
        startsSending: Boolean,
        sdkClickHandlerActivityPackageSender: IActivityPackageSender
    ) {
        paused = !startsSending
        packageQueue = ArrayList()
        activityHandlerWeakRef = WeakReference(activityHandler)
        activityPackageSender = sdkClickHandlerActivityPackageSender
    }

    /**
     * {@inheritDoc}
     */
    override fun pauseSending() {
        paused = true
    }

    /**
     * {@inheritDoc}
     */
    override fun resumeSending() {
        paused = false
        sendNextSdkClick()
    }

    /**
     * {@inheritDoc}
     */
    override fun sendSdkClick(sdkClick: ActivityPackage) {
        scheduler!!.submit {
            packageQueue!!.add(sdkClick)
            logger!!.debug("Added sdk_click %d", packageQueue!!.size)
            logger!!.verbose("%s", sdkClick.getExtendedString())
            sendNextSdkClick()
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun sendReftagReferrers() {
        scheduler!!.submit {
            val activityHandler = activityHandlerWeakRef!!.get()
            val sharedPreferencesManager = SharedPreferencesManager.getDefaultInstance(
                activityHandler!!.getContext()
            )
            try {
                val rawReferrerArray = sharedPreferencesManager!!.getRawReferrerArray()
                var hasRawReferrersBeenChanged = false
                for (i in 0 until rawReferrerArray.length()) {
                    val savedRawReferrer = rawReferrerArray.getJSONArray(i)
                    val savedRawReferrerState = savedRawReferrer.optInt(2, -1)

                    // Don't send the one already sending or sent.
                    if (savedRawReferrerState != 0) {
                        continue
                    }
                    val savedRawReferrerString = savedRawReferrer.optString(0, null)
                    val savedClickTime = savedRawReferrer.optLong(1, -1)
                    // Mark install referrer as being sent.
                    savedRawReferrer.put(2, 1)
                    hasRawReferrersBeenChanged = true

                    // Create sdk click
                    val sdkClickPackage = PackageFactory.buildReftagSdkClickPackage(
                        savedRawReferrerString,
                        savedClickTime,
                        activityHandler.getActivityState(),
                        activityHandler.getMotrackConfig(),
                        activityHandler.getDeviceInfo(),
                        activityHandler.getSessionParameters()
                    )

                    // Send referrer sdk_click package.
                    sendSdkClick(sdkClickPackage!!)
                }
                if (hasRawReferrersBeenChanged) {
                    sharedPreferencesManager.saveRawReferrerArray(rawReferrerArray)
                }
            } catch (e: JSONException) {
                logger!!.error("Send saved raw referrers error (%s)", e.message!!)
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun sendPreinstallPayload(preinstallPayload: String?, preinstallLocation: String?) {
        scheduler!!.submit(Runnable {
            val activityHandler = activityHandlerWeakRef!!.get() ?: return@Runnable

            // Create sdk click
            val sdkClickPackage = PackageFactory.buildPreinstallSdkClickPackage(
                preinstallPayload,
                preinstallLocation,
                activityHandler.getActivityState(),
                activityHandler.getMotrackConfig(),
                activityHandler.getDeviceInfo(),
                activityHandler.getSessionParameters()
            )

            // Send preinstall info sdk_click package.
            sendSdkClick(sdkClickPackage!!)
        })
    }

    /**
     * {@inheritDoc}
     */
    override fun teardown() {
        logger!!.verbose("SdkClickHandler teardown")
        if (scheduler != null) {
            scheduler!!.teardown()
        }
        if (packageQueue != null) {
            packageQueue!!.clear()
        }
        if (activityHandlerWeakRef != null) {
            activityHandlerWeakRef!!.clear()
        }
        logger = null
        packageQueue = null
        backoffStrategy = null
        scheduler = null
    }

    /**
     * Send next sdk_click package from the queue.
     */
    private fun sendNextSdkClick() {
        scheduler!!.submit { sendNextSdkClickI() }
    }

    /**
     * Send next sdk_click package from the queue (runs within scheduled executor).
     */
    private fun sendNextSdkClickI() {
        val activityHandler = activityHandlerWeakRef!!.get()
        if (activityHandler!!.getActivityState() == null) {
            return
        }
        if (activityHandler.getActivityState()!!.isGdprForgotten) {
            return
        }
        if (paused) {
            return
        }
        if (packageQueue!!.isEmpty()) {
            return
        }
        val sdkClickPackage = packageQueue!!.removeAt(0)
        val retries = sdkClickPackage.retries
        val runnable = Runnable {
            sendSdkClickI(sdkClickPackage)
            sendNextSdkClick()
        }
        if (retries <= 0) {
            runnable.run()
            return
        }
        val waitTimeMilliSeconds = Util.getWaitingTime(
            retries,
            backoffStrategy!!
        )
        val waitTimeSeconds = waitTimeMilliSeconds / MILLISECONDS_TO_SECONDS_DIVISOR
        val secondsString = Util.SecondsDisplayFormat.format(waitTimeSeconds)
        logger!!.verbose(
            "Waiting for %s seconds before retrying sdk_click for the %d time",
            secondsString,
            retries
        )
        scheduler!!.schedule(runnable, waitTimeMilliSeconds)
    }

    /**
     * Send sdk_click package passed as the parameter (runs within scheduled executor).
     *
     * @param sdkClickPackage sdk_click package to be sent.
     */
    private fun sendSdkClickI(sdkClickPackage: ActivityPackage) {
        val activityHandler = activityHandlerWeakRef!!.get()
        val source = sdkClickPackage.parameters!!["source"]
        val isReftag = source != null && source == SOURCE_REFTAG
        val rawReferrerString = sdkClickPackage.parameters!!["raw_referrer"]
        if (isReftag) {
            // Check before sending if referrer was removed already.
            val rawReferrer =
                SharedPreferencesManager.getDefaultInstance(activityHandler!!.getContext())!!
                    .getRawReferrer(
                        rawReferrerString!!,
                        sdkClickPackage.clickTimeInMilliseconds
                    ) ?: return
        }
        val isInstallReferrer = source != null && source == SOURCE_INSTALL_REFERRER
        var clickTime: Long = -1
        var installBegin: Long = -1
        var installReferrer: String? = null
        var clickTimeServer: Long = -1
        var installBeginServer: Long = -1
        var installVersion: String? = null
        var googlePlayInstant: Boolean? = null
        var referrerApi: String? = null
        if (isInstallReferrer) {
            // Check if install referrer information is saved to activity state.
            // If yes, we have successfully sent it at earlier point and no need to do it again.
            // If not, proceed with sending of sdk_click package for install referrer.
            clickTime = sdkClickPackage.clickTimeInSeconds
            installBegin = sdkClickPackage.installBeginTimeInSeconds
            installReferrer = sdkClickPackage.parameters!!["referrer"]
            clickTimeServer = sdkClickPackage.clickTimeServerInSeconds
            installBeginServer = sdkClickPackage.installBeginTimeServerInSeconds
            installVersion = sdkClickPackage.installVersion
            googlePlayInstant = sdkClickPackage.googlePlayInstant
            referrerApi = sdkClickPackage.parameters!!["referrer_api"]
        }
        val isPreinstall = source != null && source == Constants.PREINSTALL
        val sendingParameters = generateSendingParametersI()
        val responseData = activityPackageSender!!.sendActivityPackageSync(
            sdkClickPackage,
            sendingParameters
        ) as? SdkClickResponseData ?: return
        val sdkClickResponseData = responseData
        if (sdkClickResponseData.willRetry) {
            retrySendingI(sdkClickPackage)
            return
        }
        if (activityHandler == null) {
            return
        }
        if (sdkClickResponseData.trackingState === TrackingState.OPTED_OUT) {
            activityHandler.gotOptOutResponse()
            return
        }
        if (isReftag) {
            // Remove referrer from shared preferences after sdk_click is sent.
            SharedPreferencesManager.getDefaultInstance(
                activityHandler.getContext()
            )!!.removeRawReferrer(
                rawReferrerString,
                sdkClickPackage.clickTimeInMilliseconds
            )
        }
        if (isInstallReferrer) {
            // After successfully sending install referrer, store sent values in activity state.
            sdkClickResponseData.clickTime = clickTime
            sdkClickResponseData.installBegin = installBegin
            sdkClickResponseData.installReferrer = installReferrer
            sdkClickResponseData.clickTimeServer = clickTimeServer
            sdkClickResponseData.installBeginServer = installBeginServer
            sdkClickResponseData.installVersion = installVersion
            sdkClickResponseData.googlePlayInstant = googlePlayInstant!!
            sdkClickResponseData.referrerApi = referrerApi
            sdkClickResponseData.isInstallReferrer = true
        }
        if (isPreinstall) {
            val payloadLocation = sdkClickPackage.parameters!!["found_location"]
            if (payloadLocation != null && !payloadLocation.isEmpty()) {
                // update preinstall flag in shared preferences after sdk_click is sent.
                val sharedPreferencesManager =
                    SharedPreferencesManager.getDefaultInstance(activityHandler.getContext())
                if (Constants.SYSTEM_INSTALLER_REFERRER.equals(
                        payloadLocation,
                        ignoreCase = true
                    )
                ) {
                    sharedPreferencesManager!!.removePreinstallReferrer()
                } else {
                    val currentStatus = sharedPreferencesManager!!.getPreinstallPayloadReadStatus()
                    val updatedStatus = PreinstallUtil.markAsRead(payloadLocation, currentStatus)
                    sharedPreferencesManager.setPreinstallPayloadReadStatus(updatedStatus)
                }
            }
        }
        activityHandler.finishedTrackingActivity(sdkClickResponseData)
    }

    private fun generateSendingParametersI(): Map<String, String> {
        val sendingParameters: HashMap<String, String> = HashMap()
        val now = System.currentTimeMillis()
        val dateString = Util.dateFormatter.format(now)
        PackageBuilder.addString(sendingParameters, "sent_at", dateString)
        val queueSize = packageQueue!!.size - 1
        if (queueSize > 0) {
            PackageBuilder.addLong(sendingParameters, "queue_size", queueSize.toLong())
        }
        return sendingParameters
    }

    /**
     * Retry sending of the sdk_click package passed as the parameter (runs within scheduled executor).
     *
     * @param sdkClickPackage sdk_click package to be retried.
     */
    private fun retrySendingI(sdkClickPackage: ActivityPackage) {
        val retries = sdkClickPackage.increaseRetries()
        logger!!.error("Retrying sdk_click package for the %d time", retries)
        sendSdkClick(sdkClickPackage)
    }

    /**
     * Print error log messages (runs within scheduled executor).
     *
     * @param sdkClickPackage sdk_click package for which error occured.
     * @param message         Message content.
     * @param throwable       Throwable to read the reason of the error.
     */
    private fun logErrorMessageI(
        sdkClickPackage: ActivityPackage,
        message: String,
        throwable: Throwable
    ) {
        val packageMessage = sdkClickPackage.getFailureMessage()
        val reasonString = Util.getReasonString(message, throwable)
        val finalMessage = Util.formatString("%s. (%s)", packageMessage, reasonString)
        logger!!.error(finalMessage)
    }

    companion object {
        /**
         * Divisor for milliseconds -> seconds conversion.
         */
        private const val MILLISECONDS_TO_SECONDS_DIVISOR = 1000.0

        /**
         * SdkClickHandler scheduled executor source.
         */
        private const val SCHEDULED_EXECUTOR_SOURCE = "SdkClickHandler"

        /**
         * Intent based referrer source name inside of sdk_click package.
         */
        private const val SOURCE_REFTAG = "reftag"

        /**
         * Install referrer service referrer source name inside of sdk_click package.
         */
        private const val SOURCE_INSTALL_REFERRER = "install_referrer"
    }

    /**
     * SdkClickHandler constructor.
     *
     * @param activityHandler ActivityHandler reference
     * @param startsSending   Is sending paused?
     */
    init {
        init(activityHandler, startsSending, sdkClickHandlerActivityPackageSender)
        logger = MotrackFactory.getLogger()
        backoffStrategy = MotrackFactory.getSdkClickBackoffStrategy()
        scheduler = SingleThreadCachedScheduler("SdkClickHandler")
    }
}