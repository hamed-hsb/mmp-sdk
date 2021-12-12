package com.motrack.sdk

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Looper
import android.provider.Settings.Secure
import android.telephony.TelephonyManager
import android.text.TextUtils
import com.motrack.sdk.scheduler.AsyncTaskExecutor
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

class AndroidUtil {
    companion object {

        fun getGoogleAdId(context: Context, onDeviceIdRead: OnDeviceIdsRead) {
            val logger: ILogger = MotrackFactory.getLogger()
            if (Looper.myLooper() != Looper.getMainLooper()) {
                logger.debug("GoogleAdId being read in the background")
                val googleAdId: String? = getGoogleAdId(context)
                logger.debug("GoogleAdId read $googleAdId")
                onDeviceIdRead.onGoogleAdIdRead(googleAdId)
                return
            }
            logger.debug("GoogleAdId being read in the foreground")
            object : AsyncTaskExecutor<Context?, String?>() {

                protected override fun onPostExecute(result: String?) {
                    val logger: ILogger = MotrackFactory.getLogger()
                    onDeviceIdRead.onGoogleAdIdRead(result)
                }

                override fun doInBackground(params: Array<out Context?>): String? {
                    val logger: ILogger = MotrackFactory.getLogger()
                    val innerContext = params[0]
                    val innerResult: String? = innerContext?.let { getGoogleAdId(it) }
                    logger.debug("GoogleAdId read $innerResult")
                    return innerResult
                }
            }.execute(context)
        }

        fun getGoogleAdId(context: Context): String? {
            var googleAdId: String? = null
            try {
                val gpsInfo: GooglePlayServicesClient.Companion.GooglePlayServicesInfo? =
                    GooglePlayServicesClient.getGooglePlayServicesInfo(
                        context,
                        Constants.ONE_SECOND * 11
                    )
                if (gpsInfo != null) {
                    googleAdId = gpsInfo.gpsAdid
                }
            } catch (e: java.lang.Exception) {
            }
            if (googleAdId == null) {
                val advertisingInfoObject: Any? = Util.getAdvertisingInfoObject(
                    context, Constants.ONE_SECOND * 11
                )
                if (advertisingInfoObject != null) {
                    googleAdId = Util.getPlayAdId(
                        context,
                        advertisingInfoObject,
                        Constants.ONE_SECOND
                    )
                }
            }
            return googleAdId
        }

        fun getFireAdvertisingId(contentResolver: ContentResolver?): String? {
            if (contentResolver == null) {
                return null
            }
            try {
                // get advertising
                return Secure.getString(contentResolver, "advertising_id")
            } catch (ex: java.lang.Exception) {
                // not supported
            }
            return null
        }

        fun getFireTrackingEnabled(contentResolver: ContentResolver?): Boolean? {
            try {
                // get user's tracking preference
                return Secure.getInt(contentResolver, "limit_ad_tracking") == 0
            } catch (ex: java.lang.Exception) {
                // not supported
            }
            return null
        }

        @SuppressLint("MissingPermission")
        fun getConnectivityType(context: Context): Int {
            try {
                val cm =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                // for api 22 or lower, still need to get raw type
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    val activeNetwork = cm.activeNetworkInfo
                    return activeNetwork!!.type
                }

                // .getActiveNetwork() is only available from api 23
                val activeNetwork = cm.activeNetwork ?: return -1
                val activeNetworkCapabilities =
                    cm.getNetworkCapabilities(activeNetwork) ?: return -1

                // check each network capability available from api 23
                if (activeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return NetworkCapabilities.TRANSPORT_WIFI
                }
                if (activeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return NetworkCapabilities.TRANSPORT_CELLULAR
                }
                if (activeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    return NetworkCapabilities.TRANSPORT_ETHERNET
                }
                if (activeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    return NetworkCapabilities.TRANSPORT_VPN
                }
                if (activeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                    return NetworkCapabilities.TRANSPORT_BLUETOOTH
                }

                // only after api 26, that more transport capabilities were added
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    return -1
                }
                if (activeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)) {
                    return NetworkCapabilities.TRANSPORT_WIFI_AWARE
                }

                // and then after api 27, that more transport capabilities were added
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
                    return -1
                }
                if (activeNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN)) {
                    return NetworkCapabilities.TRANSPORT_LOWPAN
                }
            } catch (e: java.lang.Exception) {
                MotrackFactory.getLogger()
                    .warn("Couldn't read connectivity type (${e.message})")
            }
            return -1
        }

        @SuppressLint("MissingPermission")
        fun getNetworkType(context: Context): Int {
            var networkType = -1 // default value that will not be send
            try {
                val teleMan =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    teleMan.dataNetworkType
                } else {
                    teleMan.networkType
                }
            } catch (e: java.lang.Exception) {
                MotrackFactory.getLogger().warn("Couldn't read network type (${e.message})")
            }
            return networkType
        }

        fun getMcc(context: Context): String? {
            return try {
                val tel = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val networkOperator = tel.networkOperator
                if (TextUtils.isEmpty(networkOperator)) {
                    MotrackFactory.getLogger()
                        .warn("Couldn't receive networkOperator string to read MCC")
                    return null
                }
                networkOperator.substring(0, 3)
            } catch (ex: Exception) {
                MotrackFactory.getLogger().warn("Couldn't return mcc")
                null
            }
        }

        fun getMnc(context: Context): String? {
            return try {
                val tel = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val networkOperator = tel.networkOperator
                if (TextUtils.isEmpty(networkOperator)) {
                    MotrackFactory.getLogger()
                        .warn("Couldn't receive networkOperator string to read MNC")
                    return null
                }
                networkOperator.substring(3)
            } catch (ex: Exception) {
                MotrackFactory.getLogger().warn("Couldn't return mnc")
                null
            }
        }

        fun getMacAddress(context: Context): String? {
            val rawAddress = getRawMacAddress(context) ?: return null
            val upperAddress = rawAddress.uppercase(Locale.US)
            return removeSpaceString(upperAddress)
        }

        private fun getRawMacAddress(context: Context): String? {
            // android devices should have a wlan address
            val wlanAddress = loadAddress("wlan0")
            if (wlanAddress != null) {
                return wlanAddress
            }

            // emulators should have an ethernet address
            val ethAddress = loadAddress("eth0")
            if (ethAddress != null) {
                return ethAddress
            }

            // query the wifi manager (requires the ACCESS_WIFI_STATE permission)
            try {
                val wifiManager =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiAddress = wifiManager.connectionInfo.macAddress
                if (wifiAddress != null) {
                    return wifiAddress
                }
            } catch (e: Exception) {
                /* no-op */
            }
            return null
        }

        private fun loadAddress(interfaceName: String): String? {
            return try {
                val filePath = "/sys/class/net/$interfaceName/address"
                val fileData = StringBuilder(1000)
                val reader = BufferedReader(FileReader(filePath), 1024)
                val buf = CharArray(1024)
                var numRead: Int
                var readData: String
                while (reader.read(buf).also { numRead = it } != -1) {
                    readData = String(buf, 0, numRead)
                    fileData.append(readData)
                }
                reader.close()
                fileData.toString()
            } catch (e: IOException) {
                null
            }
        }

        private fun removeSpaceString(inputString: String?): String? {
            if (inputString == null) {
                return null
            }
            val outputString = inputString.replace("\\s".toRegex(), "")
            return if (TextUtils.isEmpty(outputString)) {
                null
            } else outputString
        }
    }
}