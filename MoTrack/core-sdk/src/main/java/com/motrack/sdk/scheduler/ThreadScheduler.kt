package com.motrack.sdk.scheduler

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

interface ThreadScheduler : ThreadExecutor {
    fun schedule(task: Runnable, milliSecondsDelay: Long)
}