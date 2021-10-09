package com.motrack.sdk

import android.content.Context
import android.net.wifi.WifiManager
import android.text.TextUtils
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

class MacAddressUtil {
    companion object {
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