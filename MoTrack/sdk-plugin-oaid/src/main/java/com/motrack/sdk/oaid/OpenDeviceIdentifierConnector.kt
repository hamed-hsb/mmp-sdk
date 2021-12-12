package com.motrack.sdk.oaid

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.IBinder.DeathRecipient
import com.motrack.sdk.ILogger
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * @author yaya (@yahyalmh)
 * @since 12th December 2021
 */

class OpenDeviceIdentifierConnector(private val context: Context, private val logger: ILogger) :
    ServiceConnection, DeathRecipient {

    private var binders: BlockingQueue<IBinder> = LinkedBlockingQueue(1)
    private var shouldUnbind = false

    companion object {
        @Volatile
        private var instance: OpenDeviceIdentifierConnector? = null
        private val lockObject = Any()

        // Lazy-initialized singleton
        fun getInstance(context: Context?, logger: ILogger?): OpenDeviceIdentifierConnector? {
            if (instance == null) {
                synchronized(OpenDeviceIdentifierConnector::class.java) {
                    if (instance == null) {
                        instance = OpenDeviceIdentifierConnector(
                            context!!,
                            logger!!
                        )
                    }
                }
            }
            return instance
        }
    }

    fun isServiceConnected(): Boolean {
        return !binders.isEmpty()
    }

    fun getOpenDeviceIdentifierService(
        timeOut: Long,
        timeUnit: TimeUnit?
    ): OpenDeviceIdentifierService? {
        // poll in order to wait & retrieve the service
        val service: IBinder? = try {
            binders.poll(timeOut, timeUnit)
        } catch (e: InterruptedException) {
            logger.error(
                "Waiting for OpenDeviceIdentifier Service interrupted: %s",
                e.message!!
            )
            return null
        }
        if (service == null) {
            logger.warn("Timed out waiting for OpenDeviceIdentifier service connection")
            return null
        }

        // set back for next poll
        set(service)
        return OpenDeviceIdentifierService.Companion.Stub.asInterface(service)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder) {
        //lets use the latest instance
        set(service)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        reset()
    }

    override fun onBindingDied(name: ComponentName?) {
        unbindAndReset()
    }

    override fun onNullBinding(name: ComponentName?) {
        unbindAndReset()
    }

    override fun binderDied() {
        unbindAndReset()
    }

    @Synchronized
    fun unbindAndReset() {
        if (shouldUnbind) {
            try {
                shouldUnbind = false
                reset()
                context.unbindService(this)
            } catch (e: Exception) {
                logger.error("Fail to unbind %s", e.message!!)
            }
        }
    }

    fun shouldUnbind() {
        shouldUnbind = true
    }

    private fun reset() {
        try {
            synchronized(lockObject) { binders.clear() }
        } catch (e: Exception) {
            logger.debug("Fail to reset queue %s", e.message!!)
        }
    }

    private fun set(service: IBinder) {
        try {
            synchronized(lockObject) {
                binders.clear()
                binders.add(service)
            }
        } catch (e: Exception) {
            logger.debug("Fail to add in queue %s", e.message!!)
        }
    }
}