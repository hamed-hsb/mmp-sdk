package com.motrack.sdk

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

class MotrackFactory {

    companion object {
        private var logger: ILogger? = null

        fun getLogger(): ILogger {
            if (logger == null) {
                logger = Logger()
            }
            return logger as ILogger
        }
    }
}