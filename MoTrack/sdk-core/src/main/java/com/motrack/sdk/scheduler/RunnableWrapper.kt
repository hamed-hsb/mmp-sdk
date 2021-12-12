package com.motrack.sdk.scheduler

import com.motrack.sdk.MotrackFactory

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

class RunnableWrapper(private var runnable: Runnable?) : Runnable{
    override fun run() {
        try {
            runnable!!.run()
        } catch (t: Throwable) {
            MotrackFactory.getLogger().error(
                "Runnable error [${ t.message}] of type [${t.javaClass.canonicalName}]",
            )
        }
    }
}