package com.motrack.sdk.scheduler

import com.motrack.sdk.MotrackFactory
import java.util.*
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

class SingleThreadCachedScheduler(source: String) : ThreadScheduler {
    private var queue: ArrayList<Runnable>? = null
    private var isThreadProcessing = false
    private var isTeardown = false
    private var threadPoolExecutor: ThreadPoolExecutor? = null

    init {
        queue = ArrayList()
        isThreadProcessing = false
        isTeardown = false
        // Same configuration as Executors.newCachedThreadPool().
        threadPoolExecutor = ThreadPoolExecutor(
            0, Int.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            SynchronousQueue(),
            ThreadFactoryWrapper(source),
            // Logs rejected runnable rejected from the entering the pool
            { runnable, _ ->

                // Logs rejected runnable rejected from the entering the pool
                MotrackFactory.getLogger().warn(
                    "Runnable [${runnable}] rejected from [$source] "
                )
            }
        )
    }

    override fun schedule(task: Runnable, milliSecondsDelay: Long) {
        synchronized(queue!!) {
            if (isTeardown) {
                return
            }
            threadPoolExecutor!!.submit {
                try {
                    Thread.sleep(milliSecondsDelay)
                } catch (e: InterruptedException) {
                    MotrackFactory.getLogger().warn(
                        "Sleep delay exception: ${e.message}"
                    )
                }
                submit(task)
            }
        }
    }

    override fun submit(task: Runnable?) {
        if (task == null){
            return
        }

        synchronized(queue!!) {
            if (isTeardown) {
                return
            }
            if (!isThreadProcessing) {
                isThreadProcessing = true
                processQueue(task)
            } else {
                queue!!.add(task)
            }
        }
    }

    override fun teardown() {
        synchronized(queue!!) {
            isTeardown = true
            queue!!.clear()
            threadPoolExecutor!!.shutdown()
        }
    }

    private fun processQueue(firstRunnable: Runnable) {
        threadPoolExecutor!!.submit(Runnable {
            // Execute the first task.
            tryExecuteRunnable(firstRunnable)
            var runnable: Runnable
            // Process all available items in the queue.
            while (true) {
                synchronized(queue!!) {

                    // Possible teardown happened meanwhile.
                    if (isTeardown) {
                        return@Runnable
                    }

                    if (queue!!.isEmpty()) {
                        isThreadProcessing = false
                        return@Runnable
                    }

                    runnable = queue!![0]
                    queue!!.removeAt(0)
                }
                tryExecuteRunnable(runnable)
            }
        })
    }


    private fun tryExecuteRunnable(runnable: Runnable) {
        try {
            if (isTeardown) {
                return
            }
            runnable.run()
        } catch (t: Throwable) {
            t.printStackTrace()
            MotrackFactory.getLogger().warn("Execution failed: ${t.message}")
        }
    }
}