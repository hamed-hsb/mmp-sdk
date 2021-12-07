package com.motrack.sdk.imei

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.text.TextUtils
import com.motrack.sdk.ILogger
import com.motrack.sdk.PackageBuilder
import java.util.ArrayList

/**
 * @author yaya (@yahyalmh)
 * @since 07th December 2021
 */

class TelephonyIdsUtil {
    companion object{
        private var imei: String? = null
        private var meid: String? = null
        private var deviceId: String? = null
        private var imeis: String? = null
        private var meids: String? = null
        private var deviceIds: String? = null

        fun injectImei(parameters: HashMap<String, String>, context: Context, logger: ILogger) {
            if (!MotrackImei.isImeiToBeRead) {
                return
            }
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            PackageBuilder.addString(parameters, "imei", getDefaultImei(telephonyManager, logger))
            PackageBuilder.addString(parameters, "meid", getDefaultMeid(telephonyManager, logger))
            PackageBuilder.addString(
                parameters,
                "device_id",
                getDefaultDeviceId(telephonyManager, logger)
            )
            PackageBuilder.addString(parameters, "imeis", getImeis(telephonyManager, logger))
            PackageBuilder.addString(parameters, "meids", getMeids(telephonyManager, logger))
            PackageBuilder.addString(
                parameters,
                "device_ids",
                getDeviceIds(telephonyManager, logger)
            )
        }

        private fun getDeviceIds(telephonyManager: TelephonyManager, logger: ILogger): String? {
            if (deviceIds != null) {
                return deviceIds
            }
            val telephonyIdList: MutableList<String?> = ArrayList()
            for (i in 0..9) {
                val telephonyId = getDeviceIdByIndex(telephonyManager, i, logger)
                if (!tryAddToStringList(telephonyIdList, telephonyId)) {
                    break
                }
            }
            deviceIds = TextUtils.join(",", telephonyIdList)
            return deviceIds
        }

        // Test difference mentioned here https://stackoverflow.com/a/35343531
        private fun getDefaultDeviceId(
            telephonyManager: TelephonyManager,
            logger: ILogger
        ): String? {
            if (deviceId != null) {
                return deviceId
            }
            try {
                deviceId = telephonyManager.deviceId
                return deviceId
            } catch (e: SecurityException) {
                e.message?.let { logger.debug("Couldn't read default Device Id: %s", it) }
            }
            return null
        }

        private fun getDeviceIdByIndex(
            telephonyManager: TelephonyManager,
            index: Int,
            logger: ILogger
        ): String? {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return telephonyManager.getDeviceId(index)
                }
            } catch (e: SecurityException) {
                e.message?.let {
                    logger.debug("Couldn't read Device Id in position %d: %s", index,
                        it
                    )
                }
            }
            return null
        }

        private fun getImeis(telephonyManager: TelephonyManager, logger: ILogger): String? {
            if (imeis != null) {
                return imeis
            }
            val imeiList: MutableList<String?> = ArrayList()
            for (i in 0..9) {
                val imei = getImeiByIndex(telephonyManager, i, logger)
                if (!tryAddToStringList(imeiList, imei)) {
                    break
                }
            }
            imeis = TextUtils.join(",", imeiList)
            return imeis
        }

        private fun getDefaultImei(telephonyManager: TelephonyManager, logger: ILogger): String? {
            if (TelephonyIdsUtil.imei != null) {
                return TelephonyIdsUtil.imei
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    TelephonyIdsUtil.imei = telephonyManager.imei
                    return TelephonyIdsUtil.imei
                }
            } catch (e: SecurityException) {
                e.message?.let { logger.debug("Couldn't read default IMEI: %s", it) }
            }
            return null
        }

        private fun getImeiByIndex(
            telephonyManager: TelephonyManager,
            index: Int,
            logger: ILogger
        ): String? {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return telephonyManager.getImei(index)
                }
            } catch (e: SecurityException) {
                e.message?.let { logger.debug("Couldn't read IMEI in position %d: %s", index, it) }
            }
            return null
        }

        fun getMeids(telephonyManager: TelephonyManager, logger: ILogger): String? {
            if (meids != null) {
                return meids
            }
            val meidList: MutableList<String?> = ArrayList()
            for (i in 0..9) {
                val meid = getMeidByIndex(telephonyManager, i, logger)
                if (!tryAddToStringList(meidList, meid)) {
                    break
                }
            }
            meids = TextUtils.join(",", meidList)
            return meids
        }

        private fun getDefaultMeid(telephonyManager: TelephonyManager, logger: ILogger): String? {
            if (meid != null) {
                return meid
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    meid = telephonyManager.meid
                    return meid
                }
            } catch (e: SecurityException) {
                e.message?.let { logger.debug("Couldn't read default MEID: %s", it) }
            }
            return null
        }

        private fun getMeidByIndex(
            telephonyManager: TelephonyManager,
            index: Int,
            logger: ILogger
        ): String? {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return telephonyManager.getMeid(index)
                }
            } catch (e: SecurityException) {
                e.message?.let { logger.debug("Couldn't read MEID in position %d: %s", index, it) }
            }
            return null
        }

        private fun tryAddToStringList(list: MutableList<String?>, value: String?): Boolean {
            if (value == null) {
                return false
            }
            return if (list.contains(value)) {
                false
            } else list.add(value)
        }
    }
}