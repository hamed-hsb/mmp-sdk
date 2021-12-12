package com.motrack.sdk.oaid

import android.content.Context
import com.bun.miitmdid.core.InfoCode
import com.bun.miitmdid.core.MdidSdkHelper
import com.motrack.sdk.ILogger
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * @author yaya (@yahyalmh)
 * @since 12th December 2021
 */

class MsaSdkClient {
    companion object {
        fun getOaid(context: Context?, logger: ILogger, maxWaitTimeInMilli: Long): String? {
            val oaidHolder: BlockingQueue<String> = LinkedBlockingQueue(1)
            try {
                val msaInternalLogging = false
                val result = MdidSdkHelper.InitSdk(
                    context, msaInternalLogging
                ) { idSupplier ->
                    try {
                        if (idSupplier == null || idSupplier.oaid == null) {
                            // so to avoid waiting for timeout
                            oaidHolder.offer("")
                        } else {
                            oaidHolder.offer(idSupplier.oaid)
                        }
                    } catch (e: Exception) {
                        logger.error("Fail to add %s", e.message!!)
                    }
                }
                if (!isError(result, logger)) {
                    return oaidHolder.poll(maxWaitTimeInMilli, TimeUnit.MILLISECONDS)
                }
            } catch (ex: NoClassDefFoundError) {
                logger.error("Couldn't find msa sdk " + ex.message)
            } catch (e: InterruptedException) {
                logger.error(
                    "Waiting to read oaid from callback interrupted: %s",
                    e.message!!
                )
            } catch (t: Throwable) {
                logger.error("Oaid reading process failed %s", t.message!!)
            }
            return null
        }

        private fun isError(result: Int, logger: ILogger): Boolean {
            return when (result) {
                InfoCode.INIT_ERROR_CERT_ERROR -> {
                    logger.error("msa sdk error - INIT_ERROR_CERT_ERROR")
                    true
                }
                InfoCode.INIT_ERROR_DEVICE_NOSUPPORT -> {
                    logger.error("msa sdk error - INIT_ERROR_DEVICE_NOSUPPORT")
                    true
                }
                InfoCode.INIT_ERROR_LOAD_CONFIGFILE -> {
                    logger.error("msa sdk error - INIT_ERROR_LOAD_CONFIGFILE")
                    true
                }
                InfoCode.INIT_ERROR_MANUFACTURER_NOSUPPORT -> {
                    logger.error("msa sdk error - INIT_ERROR_MANUFACTURER_NOSUPPORT")
                    true
                }
                InfoCode.INIT_ERROR_SDK_CALL_ERROR -> {
                    logger.error("msa sdk error - INIT_ERROR_SDK_CALL_ERROR")
                    true
                }
                else -> false
            }
        }
    }
}