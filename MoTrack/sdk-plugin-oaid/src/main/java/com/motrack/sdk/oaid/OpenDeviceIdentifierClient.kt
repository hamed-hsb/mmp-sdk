package com.motrack.sdk.oaid

import android.content.Context
import android.content.Intent
import android.os.RemoteException
import com.motrack.sdk.ILogger
import java.util.concurrent.TimeUnit

/**
 * @author yaya (@yahyalmh)
 * @since 12th December 2021
 */

class OpenDeviceIdentifierClient(private val context: Context, private val logger: ILogger, private var maxWaitTime: Long) {

    companion object {
        const val OAID_INTENT_ACTION = "com.uodis.opendevice.OPENIDS_SERVICE"
        const val HUAWEI_PACKAGE_NAME = "com.huawei.hwid"

        fun getOaidInfo(context: Context, logger: ILogger, maxWaitTimeInMilli: Long): Info? {
            var oaidInfo: Info? = null
            try {
                val openDeviceIdentifierClient =
                    OpenDeviceIdentifierClient(context, logger, maxWaitTimeInMilli)
                oaidInfo = openDeviceIdentifierClient.getOaidInfo()
            } catch (e: Throwable) {
                logger.error("Fail to read oaid, ${e.message!!}")
            }
            return oaidInfo
        }

        class Info(val oaid: String, val isOaidTrackLimited: Boolean) {

            override fun toString(): String {
                return "Info{" +
                        "oaid='" + oaid + '\'' +
                        ", isOaidTrackLimited=" + isOaidTrackLimited +
                        '}'
            }
        }
    }

    @Synchronized
    @Throws(RemoteException::class)
    private fun getOaidInfo(): Info? {
        val serviceConnector: OpenDeviceIdentifierConnector = getServiceConnector(context)
            ?: return null
        val service: OpenDeviceIdentifierService? =
            serviceConnector.getOpenDeviceIdentifierService(maxWaitTime, TimeUnit.MILLISECONDS)
        if (service == null) {
            // since service bind fails due to any reason (even timeout), its reasonable to
            // unbind it rather than keeping it open
            serviceConnector.unbindAndReset()
            return null
        }
        return Info(service.getOaid(), service.isOaidTrackLimited())
    }

    private fun getServiceConnector(context: Context?): OpenDeviceIdentifierConnector? {
        val connector: OpenDeviceIdentifierConnector? =
            OpenDeviceIdentifierConnector.getInstance(context, logger)

        // see if we still have a connected service, and return it
        if (connector!!.isServiceConnected()) {
            return connector
        }

        // try to bind to the service and return it
        val intentForOaidService = Intent(OAID_INTENT_ACTION)
        intentForOaidService.setPackage(HUAWEI_PACKAGE_NAME)
        var couldBind = false
        try {
            // letting the connector know that it should unbind in all possible failure cases
            // also it should attempt to unbind only once after each bind attempt
            connector.shouldUnbind()
            couldBind =
                context!!.bindService(intentForOaidService, connector, Context.BIND_AUTO_CREATE)
            if (couldBind) {
                return connector
            }
        } catch (e: Exception) {
            logger.error("Fail to bind service %s", e.message!!)
        } finally {
            if (!couldBind) {
                connector.unbindAndReset()
            }
        }
        logger.warn("OpenDeviceIdentifierService is not available to bind")
        return null
    }
}