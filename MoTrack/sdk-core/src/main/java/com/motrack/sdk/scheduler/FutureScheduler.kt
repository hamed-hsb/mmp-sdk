package com.motrack.sdk.scheduler

import java.util.concurrent.Callable
import java.util.concurrent.ScheduledFuture

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

interface FutureScheduler {
    fun scheduleFuture(command: Runnable?, millisecondDelay: Long): ScheduledFuture<*>?
    fun scheduleFutureWithFixedDelay(
        command: Runnable?,
        initialMillisecondDelay: Long,
        millisecondDelay: Long
    ): ScheduledFuture<*>?

    fun <V> scheduleFutureWithReturn(
        callable: Callable<V>?,
        millisecondDelay: Long
    ): ScheduledFuture<V>?

    fun teardown()
}