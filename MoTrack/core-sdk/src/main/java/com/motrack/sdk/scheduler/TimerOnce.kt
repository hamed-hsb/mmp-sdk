package com.motrack.sdk.scheduler

import com.motrack.sdk.ILogger
import com.motrack.sdk.MotrackFactory
import com.motrack.sdk.Util
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

class TimerOnce(private var command: Runnable, private var name: String) {
    private var scheduler: FutureScheduler? = null

    private var waitingTask: ScheduledFuture<*>? = null
    private var logger: ILogger = MotrackFactory.getLogger()

    init {
        scheduler = SingleThreadFutureScheduler(name, true)
    }

    fun startIn(fireIn: Long) {
        // cancel previous
        cancel(false)
        val fireInSeconds: String = Util.SecondsDisplayFormat.format(fireIn / 1000.0)

        logger.verbose("$name starting. Launching in $fireInSeconds seconds")
        waitingTask = scheduler!!.scheduleFuture({
            logger.verbose("$name fired")
            command.run()
            waitingTask = null
        }, fireIn)
    }

    fun getFireIn(): Long {
        return if (waitingTask == null) {
            0
        } else waitingTask!!.getDelay(TimeUnit.MILLISECONDS)
    }

    private fun cancel(mayInterruptIfRunning: Boolean) {
        waitingTask?.cancel(mayInterruptIfRunning)
        waitingTask = null
        logger.verbose("$name canceled")
    }

    fun cancel() {
        cancel(false)
    }

    fun teardown() {
        cancel(true)
        scheduler?.teardown()
        scheduler = null
    }
}