package com.motrack.sdk

import android.content.Context
import com.motrack.sdk.network.IActivityPackageSender
import com.motrack.sdk.network.NetworkUtil

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

class MotrackFactory {

    companion object {
        private var logger: ILogger? = null
        private var activityHandler: IActivityHandler? = null
        private var tryInstallReferrer = true
        private val sdkClickHandler: ISdkClickHandler? = null

        var baseUrl: String? = null
        var gdprUrl: String? = null
        var subscriptionUrl: String? = null
        private val connectionOptions: NetworkUtil.IConnectionOptions? = null
        private val httpsURLConnectionProvider: NetworkUtil.IHttpsURLConnectionProvider? = null

        private const val timerInterval: Long = -1
        private const val timerStart: Long = -1
        private const val sessionInterval: Long = -1
        private const val subsessionInterval: Long = -1

        private var packageHandlerBackoffStrategy: BackoffStrategy? = null
        private var installSessionBackoffStrategy: BackoffStrategy? = null
        private var sdkClickBackoffStrategy: BackoffStrategy? = null


        fun getLogger(): ILogger {
            if (logger == null) {
                logger = Logger()
            }
            return logger as ILogger
        }

        fun getActivityHandler(config: MotrackConfig): IActivityHandler? {
            if (activityHandler == null) {
                return ActivityHandler.getInstance(config)!!
            }
            activityHandler!!.init(config)
            return activityHandler
        }

        fun getTryInstallReferrer(): Boolean {
            return tryInstallReferrer
        }

        fun setTryInstallReferrer(tryInstallReferrer: Boolean) {
            MotrackFactory.tryInstallReferrer = tryInstallReferrer
        }

        fun getConnectionOptions(): NetworkUtil.IConnectionOptions {
            return connectionOptions ?: NetworkUtil.createDefaultConnectionOptions()
        }

        fun getHttpsURLConnectionProvider(): NetworkUtil.IHttpsURLConnectionProvider {
            return httpsURLConnectionProvider
                ?: NetworkUtil.createDefaultHttpsURLConnectionProvider()
        }

        fun getPackageHandlerBackoffStrategy(): BackoffStrategy {
            return packageHandlerBackoffStrategy ?: BackoffStrategy.LONG_WAIT
        }

        fun getInstallSessionBackoffStrategy(): BackoffStrategy {
            return installSessionBackoffStrategy ?: BackoffStrategy.SHORT_WAIT
        }

        fun setSdkClickBackoffStrategy(sdkClickBackoffStrategy: BackoffStrategy) {
            this.sdkClickBackoffStrategy = sdkClickBackoffStrategy
        }

        fun setPackageHandlerBackoffStrategy(packageHandlerBackoffStrategy: BackoffStrategy) {
            this.packageHandlerBackoffStrategy = packageHandlerBackoffStrategy
        }

        fun getSdkClickHandler(
            activityHandler: IActivityHandler?,
            startsSending: Boolean,
            packageHandlerActivityPackageSender: IActivityPackageSender?
        ): ISdkClickHandler {
            if (sdkClickHandler == null) {
                return SdkClickHandler(
                    activityHandler,
                    startsSending,
                    packageHandlerActivityPackageSender
                )
            }
            sdkClickHandler.init(
                activityHandler,
                startsSending,
                packageHandlerActivityPackageSender
            )
            return sdkClickHandler
        }

        fun getTimerInterval(): Long {
            return if (timerInterval == -1L) {
                Constants.ONE_MINUTE
            } else timerInterval
        }

        fun getTimerStart(): Long {
            return if (timerStart == -1L) {
                Constants.ONE_MINUTE
            } else timerStart
        }

        fun getSessionInterval(): Long {
            return if (sessionInterval == -1L) {
                Constants.THIRTY_MINUTES
            } else sessionInterval
        }

        fun getSubsessionInterval(): Long {
            return if (subsessionInterval == -1L) {
                Constants.ONE_SECOND
            } else subsessionInterval
        }

        fun teardown(context: Context?) {

            activityHandler = null
            logger = null
            tryInstallReferrer = true

            baseUrl = Constants.BASE_URL
            gdprUrl = Constants.GDPR_URL
            subscriptionUrl = Constants.SUBSCRIPTION_URL

            sdkClickBackoffStrategy = null
            packageHandlerBackoffStrategy = null

        }
    }
}