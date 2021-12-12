package com.motrack.sdk.oaid

import android.content.Context
import android.os.Build
import android.util.Log
import com.motrack.sdk.ILogger
import com.motrack.sdk.PackageBuilder
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 12th December 2021
 */

class Util {
    companion object {
        @Synchronized
        fun getOaidParameters(context: Context, logger: ILogger): Map<String, String>? {
            if (!MotrackOaid.isOaidToBeRead) {
                return null
            }
            val oaidParameters: Map<String, String>?

            // IMPORTANT:
            // if manufacturer is huawei then try reading the oaid with hms (huawei mobile service)
            // approach first, as it can read both oaid and limit tracking status
            // otherwise use the msa sdk which only gives the oaid currently
            return if (isManufacturerHuawei(logger)) {
                oaidParameters = getOaidParametersUsingHMS(context, logger)
                oaidParameters ?: getOaidParametersUsingMSA(context, logger)
            } else {
                oaidParameters = getOaidParametersUsingMSA(context, logger)
                oaidParameters ?: getOaidParametersUsingHMS(context, logger)
            }
        }

        private fun isManufacturerHuawei(logger: ILogger): Boolean {
            try {
                val manufacturer = Build.MANUFACTURER
                if (manufacturer != null && manufacturer.equals("huawei", ignoreCase = true)) {
                    return true
                }
            } catch (e: Exception) {
                logger.debug("Manufacturer not available")
            }
            return false
        }

        private fun getOaidParametersUsingHMS(
            context: Context,
            logger: ILogger
        ): Map<String, String>? {
            var attempt = 1
            while (attempt <= 3) {
                val oaidInfo: OpenDeviceIdentifierClient.Companion.Info? =
                    OpenDeviceIdentifierClient.getOaidInfo(
                        context,
                        logger,
                        (3000 * attempt).toLong()
                    )
                if (oaidInfo != null) {
                    val parameters: HashMap<String, String> = HashMap()
                    PackageBuilder.addString(parameters, "oaid", oaidInfo.oaid)
                    PackageBuilder.addBoolean(
                        parameters,
                        "oaid_tracking_enabled",
                        !oaidInfo.isOaidTrackLimited
                    )
                    PackageBuilder.addString(parameters, "oaid_src", "hms")
                    PackageBuilder.addLong(parameters, "oaid_attempt", attempt.toLong())
                    return parameters
                }
                attempt += 1
            }
            logger.debug("Fail to read the OAID using HMS")
            return null
        }

        private fun getOaidParametersUsingMSA(
            context: Context,
            logger: ILogger
        ): Map<String, String>? {
            if (!MotrackOaid.isMsaSdkAvailable) {
                return null
            }
            var attempt = 1
            while (attempt <= 3) {
                val oaid: String? = MsaSdkClient.getOaid(context, logger, 3000 * attempt.toLong())
                if (oaid != null && oaid.isNotEmpty()) {
                    val parameters: HashMap<String, String> = HashMap()
                    PackageBuilder.addString(parameters, "oaid", oaid)
                    PackageBuilder.addString(parameters, "oaid_src", "msa")
                    PackageBuilder.addLong(parameters, "oaid_attempt", attempt.toLong())
                    return parameters
                }
                attempt += 1
            }
            logger.debug("Fail to read the OAID using MSA")
            return null
        }

        fun readCertFromAssetFile(context: Context): String? {
            return try {
                val assetFileName = context.packageName + ".cert.pem"
                val `is` = context.assets.open(assetFileName)
                val `in` = BufferedReader(InputStreamReader(`is`))
                val builder = StringBuilder()
                var line: String?
                while (`in`.readLine().also { line = it } != null) {
                    builder.append(line)
                    builder.append('\n')
                }
                builder.toString()
            } catch (e: Exception) {
                Log.e("Adjust", "readCertFromAssetFile failed")
                ""
            }
        }
    }
}