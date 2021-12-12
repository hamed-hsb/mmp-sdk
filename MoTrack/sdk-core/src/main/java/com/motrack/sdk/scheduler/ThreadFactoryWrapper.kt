package com.motrack.sdk.scheduler

import android.os.Process
import com.motrack.sdk.Constants
import com.motrack.sdk.MotrackFactory
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

class ThreadFactoryWrapper(private val source: String) : ThreadFactory {
    override fun newThread(r: Runnable?): Thread {
        val thread = Executors.defaultThreadFactory().newThread(r)

        thread.priority =
            Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE
        thread.name = Constants.THREAD_PREFIX + thread.name + "-" + source
        thread.isDaemon = true
        thread.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { th, tr ->
            MotrackFactory.getLogger().error(
                "Thread [${th.name}] with error [${tr.message}]"
            )
        }

        return thread
    }
}