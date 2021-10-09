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

class TimerCycle(
    private var command: Runnable?,
    private var initialDelay: Long,
    private var cycleDelay: Long,
    name: String
) {
    private var scheduler: FutureScheduler? = null
    private var waitingTask: ScheduledFuture<*>? = null
    private var name: String? = name
    private var isPaused = false
    private var logger: ILogger = MotrackFactory.getLogger()

    init {
        scheduler = SingleThreadFutureScheduler(name, true)
        isPaused = true
        val cycleDelaySecondsString: String = Util.SecondsDisplayFormat.format(cycleDelay / 1000.0)
        val initialDelaySecondsString: String =
            Util.SecondsDisplayFormat.format(initialDelay / 1000.0)
        logger.verbose(
            "$name!! configured to fire after $initialDelaySecondsString seconds of starting and cycles every $cycleDelaySecondsString seconds",
        )
    }

    fun start() {
        if (!isPaused) {
            logger.verbose("$name is already started")
            return
        }

        logger.verbose("$name starting")
        waitingTask = scheduler!!.scheduleFutureWithFixedDelay({
            logger.verbose("$name fired")
            command!!.run()
        }, initialDelay, cycleDelay)
        isPaused = false
    }


    fun suspend() {
        if (isPaused) {
            logger.verbose("$name is already suspended")
            return
        }

        // get the remaining delay
        initialDelay = waitingTask!!.getDelay(TimeUnit.MILLISECONDS)

        // cancel the timer
        waitingTask!!.cancel(false)
        val initialDelaySeconds = Util.SecondsDisplayFormat.format(initialDelay / 1000.0)
        logger.verbose("$name suspended with %s seconds left", initialDelaySeconds)
        isPaused = true
    }

    private fun cancel(mayInterruptIfRunning: Boolean) {
        waitingTask?.cancel(mayInterruptIfRunning)
        waitingTask = null
    }

    fun teardown() {
        cancel(true)
        scheduler?.teardown()
        scheduler = null
    }
}