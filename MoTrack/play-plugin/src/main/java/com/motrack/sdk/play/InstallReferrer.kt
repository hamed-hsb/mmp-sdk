package com.motrack.sdk.play

/**
 * @author yaya (@yahyalmh)
 * @since 10th October 2021
 */
import android.content.Context
import android.os.RemoteException
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.motrack.sdk.*
import com.motrack.sdk.Constants.Companion.ONE_SECOND
import com.motrack.sdk.scheduler.TimerOnce
import java.util.concurrent.atomic.AtomicBoolean

class InstallReferrer(
    private val context: Context?,
    private val referrerCallback: InstallReferrerReadListener,
    private val logger: ILogger
) : InstallReferrerStateListener {
    companion object {
        private const val MAX_INSTALL_REFERRER_RETRIES = 2
        private val RETRY_WAIT_TIME: Long = ONE_SECOND * 3
    }

    private var client: InstallReferrerClient? = null
    private var retryNumber = 0

    private var hasInstallReferrerBeenRead: AtomicBoolean? = null
    private var retryTimer: TimerOnce? = null

    init {
        hasInstallReferrerBeenRead = AtomicBoolean(false)
        retryTimer = TimerOnce(Runnable { startConnection() }, "InstallReferrer")
        retryNumber = 0
    }

    fun startConnection() {
        if (!MotrackFactory.getTryInstallReferrer()) {
            return
        }
        closeReferrerClient()
        if (hasInstallReferrerBeenRead!!.get()) {
            logger.debug("Install referrer has already been read")
            return
        }
        client = InstallReferrerClient.newBuilder(context).build()
        client!!.startConnection(this)
    }

    private fun closeReferrerClient() {
        if (client == null) {
            return
        }
        client!!.endConnection()
        logger.debug("Install Referrer API connection closed")
        client = null
    }

    /**
     * Called to notify that setup is complete.
     *
     * @param responseCode The response code from {@link InstallReferrerClient.InstallReferrerResponse} which returns the
     *     status of the setup process.
     */
    override fun onInstallReferrerSetupFinished(responseCode: Int) {
        var retryAtEnd = false
        when (responseCode) {

            /** Success. */
            InstallReferrerClient.InstallReferrerResponse.OK ->
                // Connection established
                try {
                    val installReferrerDetails = client!!.installReferrer
                    val installReferrer = installReferrerDetails.installReferrer
                    val referrerClickTimestampSeconds =
                        installReferrerDetails.referrerClickTimestampSeconds
                    val installBeginTimestampSeconds =
                        installReferrerDetails.installBeginTimestampSeconds
                    val installVersion = installReferrerDetails.installVersion
                    val referrerClickTimestampServerSeconds =
                        installReferrerDetails.referrerClickTimestampServerSeconds
                    val installBeginTimestampServerSeconds =
                        installReferrerDetails.installBeginTimestampServerSeconds
                    val googlePlayInstantParam = installReferrerDetails.googlePlayInstantParam
                    referrerCallback.onInstallReferrerRead(
                        ReferrerDetails(
                            installReferrer,
                            referrerClickTimestampSeconds, installBeginTimestampSeconds,
                            referrerClickTimestampServerSeconds, installBeginTimestampServerSeconds,
                            installVersion, googlePlayInstantParam
                        ), Constants.REFERRER_API_GOOGLE
                    )
                    hasInstallReferrerBeenRead!!.set(true)
                    logger.debug("Install Referrer read successfully. Closing connection")
                } catch (e: RemoteException) {
                    logger.warn("Couldn't get install referrer from client (${e.message!!}). Retrying...")
                    retryAtEnd = true
                }

            /** Install Referrer API not supported by the installed Play Store app. */
            InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED ->
                // API not available on the current Play Store app
                logger.debug("Install Referrer API not supported by the installed Play Store app. Closing connection")

            /** Could not initiate connection to the Install Referrer service. */
            InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                // Connection could not be established
                logger.debug("Could not initiate connection to the Install Referrer service. Retrying...")
                retryAtEnd = true
            }

            /**
             * Play Store service is not connected now - potentially transient state.
             *
             * <p>E.g. Play Store could have been updated in the background while your app was still
             * running. So feel free to introduce your retry policy for such use case. It should lead to a
             * call to {@link #startConnection(InstallReferrerStateListener)} right after or in some time
             * after you received this code.
             */
            InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED -> {
                // Connection could not be established
                logger.debug("Play Store service is not connected now. Retrying ...")
                retryAtEnd = true
            }

            /** General errors caused by incorrect usage */
            InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR -> {
                logger.debug("Install Referrer API general errors caused by incorrect usage. Retrying...")
                retryAtEnd = true
            }
            else -> logger.debug("Unexpected response code of install referrer response: $responseCode. Closing connection")
        }

        if (retryAtEnd) {
            retry()
        } else {
            closeReferrerClient()
        }
    }

    /**
     * Called to notify that connection to install referrer service was lost.
     *
     * <p>Note: This does not remove install referrer service connection itself - this binding to the
     * service will remain active, and you will receive a call to {@link
     * #onInstallReferrerSetupFinished(int)} when install referrer service is next running and setup
     * is complete.
     */
    override fun onInstallReferrerServiceDisconnected() {
        // Try to restart the connection on the next request to
        // Google Play by calling the startConnection() method.

        // Try to restart the connection on the next request to
        // Google Play by calling the startConnection() method.
        logger.debug("Connection to install referrer service was lost. Retrying ...")
        retry()
    }

    private fun retry() {
        if (hasInstallReferrerBeenRead!!.get()) {
            logger.debug("Install referrer has already been read")
            closeReferrerClient()
            return
        }

        // Check increase retry counter
        if (retryNumber + 1 > MAX_INSTALL_REFERRER_RETRIES) {
            logger.debug("Limit number of retry of $MAX_INSTALL_REFERRER_RETRIES for install referrer surpassed")
            return
        }
        val firingIn = retryTimer!!.getFireIn()
        if (firingIn > 0) {
            logger.debug("Already waiting to retry to read install referrer in $firingIn milliseconds")
            return
        }
        retryNumber++
        logger.debug("Retry number %d to connect to install referrer API", retryNumber)
        retryTimer!!.startIn(RETRY_WAIT_TIME)
    }

}