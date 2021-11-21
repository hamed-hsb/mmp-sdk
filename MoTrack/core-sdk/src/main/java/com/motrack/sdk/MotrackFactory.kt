package com.motrack.sdk

import android.content.Context
import com.motrack.sdk.network.IActivityPackageSender
import com.motrack.sdk.network.NetworkUtil
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

class MotrackFactory {

    companion object {
        private var activityHandler: IActivityHandler? = null
        var tryInstallReferrer = true
        private var sdkClickHandler: ISdkClickHandler? = null
        private var packageHandler: IPackageHandler? = null
        var baseUrl: String? = null
        var gdprUrl: String? = null
        var subscriptionUrl: String? = null

        private var attributionHandler: IAttributionHandler? = null

        private var logger: ILogger? = null

        public var maxDelayStart: Long = -1
            get() {
                return if (field == -1L) {
                    Constants.ONE_SECOND * 10 // 10 seconds
                } else field
            }

        public var connectionOptions: NetworkUtil.IConnectionOptions? = null
            get() = field ?: NetworkUtil.createDefaultConnectionOptions()

        public var httpsURLConnectionProvider: NetworkUtil.IHttpsURLConnectionProvider? = null
            get() = field ?: NetworkUtil.createDefaultHttpsURLConnectionProvider()

        public var timerInterval: Long = -1
            get() {
                return if (field == -1L) {
                    Constants.ONE_MINUTE
                } else field
            }

        public var timerStart: Long = -1
            get() {
                return if (field == -1L) {
                    Constants.ONE_MINUTE
                } else field
            }

        var sessionInterval: Long = -1
            get() {
                return if (field == -1L) {
                    Constants.THIRTY_MINUTES
                } else field
            }
        var subsessionInterval: Long = -1
            get() {
                return if (field == -1L) {
                    Constants.ONE_SECOND
                } else field
            }

        var packageHandlerBackoffStrategy: BackoffStrategy? = null
            get() = field ?: BackoffStrategy.LONG_WAIT

        public var installSessionBackoffStrategy: BackoffStrategy? = null
            get() = field ?: BackoffStrategy.SHORT_WAIT

        public var sdkClickBackoffStrategy: BackoffStrategy? = null
            get() = field ?: BackoffStrategy.SHORT_WAIT

        fun getActivityHandler(config: MotrackConfig): IActivityHandler? {
            if (activityHandler == null) {
                return ActivityHandler.getInstance(config)!!
            }
            activityHandler!!.init(config)
            return activityHandler
        }

        public fun getLogger(): ILogger {
            if (logger == null) {
                logger = Logger()
            }
            return logger as ILogger
        }

        public fun setLogger(logger: ILogger?) {
            this.logger = logger
        }

        fun setActivityHandler(activityHandler: IActivityHandler?) {
            MotrackFactory.activityHandler = activityHandler
        }

        fun setAttributionHandler(attributionHandler: IAttributionHandler?) {
            MotrackFactory.attributionHandler = attributionHandler
        }

        fun setSdkClickHandler(sdkClickHandler: ISdkClickHandler?) {
            MotrackFactory.sdkClickHandler = sdkClickHandler
        }

        fun setPackageHandler(packageHandler: IPackageHandler?) {
            MotrackFactory.packageHandler = packageHandler
        }

        fun getPackageHandler(
            activityHandler: IActivityHandler?,
            context: Context?,
            startsSending: Boolean,
            packageHandlerActivityPackageSender: IActivityPackageSender?
        ): IPackageHandler {
            if (packageHandler == null) {
                return PackageHandler(
                    activityHandler,
                    context,
                    startsSending,
                    packageHandlerActivityPackageSender
                )
            }
            packageHandler?.init(
                activityHandler,
                context,
                startsSending,
                packageHandlerActivityPackageSender
            )
            return packageHandler as IPackageHandler
        }

        fun getAttributionHandler(
            activityHandler: IActivityHandler,
            startsSending: Boolean,
            packageHandlerActivityPackageSender: IActivityPackageSender
        ): IAttributionHandler {
            if (attributionHandler == null) {
                return AttributionHandler(
                    activityHandler,
                    startsSending,
                    packageHandlerActivityPackageSender
                )
            }
            attributionHandler?.init(
                activityHandler,
                startsSending,
                packageHandlerActivityPackageSender
            )
            return attributionHandler as IAttributionHandler
        }

        fun getSdkClickHandler(
            activityHandler: IActivityHandler?,
            startsSending: Boolean,
            packageHandlerActivityPackageSender: IActivityPackageSender
        ): ISdkClickHandler {
            if (sdkClickHandler == null) {
                return SdkClickHandler(
                    activityHandler,
                    startsSending,
                    packageHandlerActivityPackageSender
                )
            }
            sdkClickHandler?.init(
                activityHandler,
                startsSending,
                packageHandlerActivityPackageSender
            )
            return sdkClickHandler as ISdkClickHandler
        }

        fun enableSigning() {
            logger?.let { MotrackSigner.enableSigning(it) }
        }

        fun disableSigning() {
            logger?.let { MotrackSigner.disableSigning(it) }
        }

        private fun byte2HexFormatted(arr: ByteArray): String {
            val str = StringBuilder(arr.size * 2)
            for (i in arr.indices) {
                var h = Integer.toHexString(arr[i].toInt())
                val l = h.length
                if (l == 1) {
                    h = "0$h"
                }
                if (l > 2) {
                    h = h.substring(l - 2, l)
                }
                str.append(h.uppercase(Locale.getDefault()))

                // if (i < (arr.length - 1)) str.append(':');
            }
            return str.toString()
        }

        fun teardown(context: Context?) {
            context?.let {
                ActivityHandler.deleteState(context)
                PackageHandler.deleteState(context)
            }

            packageHandler = null
            attributionHandler = null
            activityHandler = null
            logger = null
            sdkClickHandler = null

            timerInterval = -1
            timerStart = -1
            sessionInterval = -1
            subsessionInterval = -1
            sdkClickBackoffStrategy = null
            packageHandlerBackoffStrategy = null
            maxDelayStart = -1
            baseUrl = Constants.BASE_URL
            gdprUrl = Constants.GDPR_URL
            subscriptionUrl = Constants.SUBSCRIPTION_URL
            connectionOptions = null
            httpsURLConnectionProvider = null
            tryInstallReferrer = true
        }
    }
}