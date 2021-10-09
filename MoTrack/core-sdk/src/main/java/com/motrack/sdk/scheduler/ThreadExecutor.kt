package com.motrack.sdk.scheduler

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

interface ThreadExecutor {
    fun submit(task: Runnable?)
    fun teardown()
}