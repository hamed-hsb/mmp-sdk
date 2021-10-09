package com.motrack.sdk.scheduler

import com.motrack.sdk.MotrackFactory
import java.util.concurrent.Callable
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

class SingleThreadFutureScheduler(source: String, doKeepAlive: Boolean) : FutureScheduler {
    private var scheduledThreadPoolExecutor: ScheduledThreadPoolExecutor? = null

    init {
        scheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(
            1,
            ThreadFactoryWrapper(source),
            { runnable, _ ->
                // Logs rejected runnable rejected from the entering the pool
                MotrackFactory.getLogger().warn(
                    "Runnable [${runnable}] rejected from [$source] "
                )
            }
        )
        if (!doKeepAlive) {
            scheduledThreadPoolExecutor!!.setKeepAliveTime(10L, TimeUnit.MILLISECONDS)
            scheduledThreadPoolExecutor!!.allowCoreThreadTimeOut(true)
        }
    }

    override fun scheduleFuture(command: Runnable?, millisecondDelay: Long): ScheduledFuture<*>? {
        return scheduledThreadPoolExecutor!!.schedule(
            RunnableWrapper(command),
            millisecondDelay,
            TimeUnit.MILLISECONDS
        )
    }

    override fun scheduleFutureWithFixedDelay(
        command: Runnable?,
        initialMillisecondDelay: Long,
        millisecondDelay: Long
    ): ScheduledFuture<*>? {
        return scheduledThreadPoolExecutor!!.scheduleWithFixedDelay(
            RunnableWrapper(command),
            initialMillisecondDelay,
            millisecondDelay,
            TimeUnit.MILLISECONDS
        )

    }

    override fun <V> scheduleFutureWithReturn(
        callable: Callable<V>?,
        millisecondDelay: Long
    ): ScheduledFuture<V>? {
        return scheduledThreadPoolExecutor!!.schedule(Callable {
            try {
                return@Callable callable!!.call()
            } catch (t: Throwable) {
                MotrackFactory.getLogger().error(
                    "Callable error [${t.message}] of type [${t.javaClass.canonicalName}]"
                )
                return@Callable null
            }
        }, millisecondDelay, TimeUnit.MILLISECONDS)
    }

    override fun teardown() {
        scheduledThreadPoolExecutor!!.shutdownNow()
    }
}