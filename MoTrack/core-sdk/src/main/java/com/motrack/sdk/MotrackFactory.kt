package com.motrack.sdk

import android.content.Context

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

class MotrackFactory {

    companion object {
        private var logger: ILogger? = null
        private var activityHandler: IActivityHandler? = null
        private var tryInstallReferrer = true


        private const val timerInterval: Long = -1
        private const val timerStart: Long = -1
        private const val sessionInterval: Long = -1
        private const val subsessionInterval: Long = -1


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
        }
    }
}