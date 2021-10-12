package com.motrack.sdk.play

import android.content.Context
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.motrack.sdk.ILogger
import com.motrack.sdk.PackageBuilder
import java.io.IOException

/**
 * @author yaya (@yahyalmh)
 * @since 10th October 2021
 */

class Util {
    companion object {
        fun getPlayParameters(context: Context, logger: ILogger): Map<String, String> {
            val parameters: HashMap<String, String> = HashMap()
            injectPlayInfo(parameters, context, logger)
            return parameters
        }

        private fun injectPlayInfo(
            parameters: HashMap<String, String>,
            context: Context,
            logger: ILogger
        ) {
            var advertisingIdInfo: AdvertisingIdClient.Info? = null
            try {
                advertisingIdInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
            } catch (e: IOException) {
                logger.error("IOException when trying to get Advertising Id Info: ${e.message!!}")
            } catch (e: GooglePlayServicesNotAvailableException) {
                logger.error("GooglePlayServicesNotAvailableException when trying to get Advertising Id Info: ${e.message!!}")
            } catch (e: GooglePlayServicesRepairableException) {
                logger.error("GooglePlayServicesRepairableException when trying to get Advertising Id Info: ${e.message!!}")
            }
            if (advertisingIdInfo == null) {
                return
            }
            val id = advertisingIdInfo.id
            val limitAdTrackingEnabled = advertisingIdInfo.isLimitAdTrackingEnabled
            PackageBuilder.addString(parameters, "gps_adid", id)
            PackageBuilder.addBoolean(parameters, "tracking_enabled", limitAdTrackingEnabled)
        }
    }
}