package com.motrack.sdk

import android.content.Context
import com.motrack.sdk.network.IActivityPackageSender
import com.motrack.sdk.scheduler.SingleThreadCachedScheduler
import com.motrack.sdk.scheduler.ThreadScheduler
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList

/**
 * @author yaya (@yahyalmh)
 * @since 12th October 2021
 */

class PackageHandler(
    activityHandler: IActivityHandler?,
    context: Context?,
    startsSending: Boolean,
    packageHandlerActivityPackageSender: IActivityPackageSender?
) : IPackageHandler, IActivityPackageSender.ResponseDataCallbackSubscriber {

    companion object {
        private const val PACKAGE_QUEUE_FILENAME = "MotrackIoPackageQueue"
        private const val PACKAGE_QUEUE_NAME = "Package queue"
    }

    private var scheduler: ThreadScheduler? = null
    private var activityPackageSender: IActivityPackageSender? = null
    private var activityHandlerWeakRef: WeakReference<IActivityHandler>? = null
    private var packageQueue: ArrayList<ActivityPackage>? = null
    private var isSending: AtomicBoolean? = null
    private var paused = false
    private var context: Context? = null
    private var logger: ILogger? = null
    private var backoffStrategy: BackoffStrategy?
    private var backoffStrategyForInstallSession: BackoffStrategy

    init {
        scheduler = SingleThreadCachedScheduler("PackageHandler")
        logger = MotrackFactory.getLogger()
        backoffStrategy = MotrackFactory.getPackageHandlerBackoffStrategy()
        backoffStrategyForInstallSession = MotrackFactory.getInstallSessionBackoffStrategy()
        init(activityHandler, context, startsSending, packageHandlerActivityPackageSender)
        scheduler!!.submit { initI() }
    }

    override fun init(
        activityHandler: IActivityHandler?,
        context: Context?,
        startsSending: Boolean,
        packageHandlerActivityPackageSender: IActivityPackageSender?
    ) {
        activityHandlerWeakRef = WeakReference(activityHandler)
        this.context = context
        paused = !startsSending
        activityPackageSender = packageHandlerActivityPackageSender
    }


    // add a package to the queue
    override fun addPackage(activityPackage: ActivityPackage) {
        scheduler!!.submit { addI(activityPackage) }
    }

    // try to send the oldest package
    override fun sendFirstPackage() {
        scheduler!!.submit { sendFirstI() }
    }

    override fun onResponseDataCallback(responseData: ResponseData) {
        logger!!.debug("Got response in PackageHandler")
        val activityHandler = activityHandlerWeakRef!!.get()
        if (activityHandler != null &&
            responseData.trackingState === TrackingState.OPTED_OUT
        ) {
            activityHandler.gotOptOutResponse()
        }
        if (!responseData.willRetry) {
            scheduler!!.submit { sendNextI() }
            activityHandler?.finishedTrackingActivity(responseData)
            return
        }
        activityHandler?.finishedTrackingActivity(responseData)
        val runnable = Runnable {
            logger!!.verbose("Package handler can send")
            isSending!!.set(false)

            // Try to send the same package after sleeping
            sendFirstPackage()
        }
        if (responseData.activityPackage == null) {
            runnable.run()
            return
        }
        val retries = responseData.activityPackage!!.increaseRetries()
        val waitTimeMilliSeconds: Long
        val sharedPreferencesManager = SharedPreferencesManager(context!!)
        waitTimeMilliSeconds = if (responseData.activityPackage!!.getActivityKind() ===
            ActivityKind.SESSION && !sharedPreferencesManager.getInstallTracked()
        ) {
            Util.getWaitingTime(retries, backoffStrategyForInstallSession)
        } else {
            Util.getWaitingTime(retries, backoffStrategy!!)
        }
        val waitTimeSeconds = waitTimeMilliSeconds / 1000.0
        val secondsString = Util.SecondsDisplayFormat.format(waitTimeSeconds)
        logger!!.verbose("Waiting for $secondsString seconds before retrying the $retries time")
        scheduler!!.schedule(runnable, waitTimeMilliSeconds)
    }

    // interrupt the sending loop after the current request has finished
    override fun pauseSending() {
        paused = true
    }

    // allow sending requests again
    override fun resumeSending() {
        paused = false
    }

    override fun updatePackages(sessionParameters: SessionParameters?) {
        val sessionParametersCopy: SessionParameters? = sessionParameters?.deepCopy()
        scheduler!!.submit { updatePackagesI(sessionParametersCopy) }
    }

    override fun flush() {
        scheduler!!.submit { flushI() }
    }


    override fun teardown() {
        logger!!.verbose("PackageHandler teardown")
        if (scheduler != null) {
            scheduler!!.teardown()
        }
        activityHandlerWeakRef?.clear()
        packageQueue?.clear()
        scheduler = null
        activityHandlerWeakRef = null
        packageQueue = null
        isSending = null
        context = null
        logger = null
        backoffStrategy = null
    }

    fun deleteState(context: Context) {
        deletePackageQueue(context)
    }

    // internal methods run in dedicated queue thread
    private fun initI() {
        isSending = AtomicBoolean()
        readPackageQueueI()
    }

    private fun addI(newPackage: ActivityPackage) {
        packageQueue!!.add(newPackage)
        logger!!.debug("Added package ${packageQueue!!.size} ($newPackage)")
        logger!!.verbose(newPackage.getExtendedString())
        writePackageQueueI()
    }

    private fun sendFirstI() {
        if (packageQueue!!.isEmpty()) {
            return
        }
        if (paused) {
            logger!!.debug("Package handler is paused")
            return
        }
        if (isSending!!.getAndSet(true)) {
            logger!!.verbose("Package handler is already sending")
            return
        }
        val sendingParameters = generateSendingParametersI()
        val firstPackage = packageQueue!![0]
        activityPackageSender!!.sendActivityPackage(
            firstPackage,
            sendingParameters,
            this
        )
    }

    private fun generateSendingParametersI(): HashMap<String, String> {
        val sendingParameters = HashMap<String, String>()
        val now = System.currentTimeMillis()
        val dateString = Util.dateFormatter.format(now)
        PackageBuilder.addString(sendingParameters, "sent_at", dateString)
        val queueSize = packageQueue!!.size - 1
        if (queueSize > 0) {
            PackageBuilder.addLong(sendingParameters, "queue_size", queueSize.toLong())
        }
        return sendingParameters
    }

    private fun sendNextI() {
        if (packageQueue!!.isEmpty()) {
            return
        }
        packageQueue!!.removeAt(0)
        writePackageQueueI()
        isSending!!.set(false)
        logger!!.verbose("Package handler can send")
        sendFirstI()
    }

    fun updatePackagesI(sessionParameters: SessionParameters?) {
        if (sessionParameters == null) {
            return
        }
        logger!!.debug("Updating package handler queue")
        logger!!.verbose("Session callback parameters: %s", sessionParameters.callbackParameters!!)
        logger!!.verbose("Session partner parameters: %s", sessionParameters.partnerParameters!!)
        for (activityPackage in packageQueue!!) {
            val parameters = activityPackage.getParameters()
            // callback parameters
            val mergedCallbackParameters: HashMap<String, String>? = Util.mergeParameters(
                sessionParameters.callbackParameters,
                activityPackage.getCallbackParameters(),
                "Callback"
            )
            PackageBuilder.addMapJson(
                parameters!!,
                Constants.CALLBACK_PARAMETERS,
                mergedCallbackParameters
            )
            // partner parameters
            val mergedPartnerParameters: HashMap<String, String>? = Util.mergeParameters(
                sessionParameters.partnerParameters,
                activityPackage.getPartnerParameters(),
                "Partner"
            )
            PackageBuilder.addMapJson(
                parameters,
                Constants.PARTNER_PARAMETERS,
                mergedPartnerParameters
            )
        }
        writePackageQueueI()
    }

    private fun flushI() {
        packageQueue!!.clear()
        writePackageQueueI()
    }

    private fun readPackageQueueI() {
        packageQueue = try {
            Util.readObject(
                context!!,
                PACKAGE_QUEUE_FILENAME,
                PACKAGE_QUEUE_NAME,
                ArrayList::class.java as Class<ArrayList<ActivityPackage>?>
            )
        } catch (e: Exception) {
            logger!!.error("Failed to read $PACKAGE_QUEUE_NAME file (${e.message!!})")
            null
        }
        if (packageQueue != null) {
            logger!!.debug("Package handler read ${packageQueue!!.size} packages")
        } else {
            packageQueue = java.util.ArrayList()
        }
    }

    private fun writePackageQueueI() {
        Util.writeObject(
            packageQueue,
            context!!, PACKAGE_QUEUE_FILENAME, PACKAGE_QUEUE_NAME
        )
        logger!!.debug("Package handler wrote %d packages", packageQueue!!.size)
    }

    fun deletePackageQueue(context: Context): Boolean {
        return context.deleteFile(PACKAGE_QUEUE_FILENAME)
    }
}