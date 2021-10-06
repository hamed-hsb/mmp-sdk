package com.motrack.sdk

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

class Util {
    companion object {
        @JvmStatic
        public fun checkPermission(context: Context, permission: String): Boolean {
            return try {
                val result = context.checkCallingOrSelfPermission(permission)
                result == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                getLogger().debug("Unable to check permission $permission, with message ${e.message}")
                false
            }
        }

        @JvmStatic
        public fun isValidParameter(
            attribute: String?,
            attributeType: String,
            parameterName: String
        ): Boolean {
            if (attribute == null) {
                getLogger().error("$parameterName parameter $attributeType is missing")
                return false
            }
            if (attribute.isEmpty() || attribute.isBlank()) {
                getLogger().error("$parameterName parameter $attributeType is empty")
                return false
            }
            return true
        }

        @JvmStatic
        public fun getSdkPrefix(clientSdk: String): String? {
            if (clientSdk.isNullOrEmpty()) {
                return null
            }

            if (!clientSdk.contains("@")) {
                return null
            }

            val split = clientSdk.split("@")
            if (split.isNullOrEmpty()) {
                return null
            }

            if (split.size != 2) {
                return null
            }

            return split[0]
        }

        @JvmStatic
        public fun getSdkPrefixPlatform(clientSdk: String): String? {
            val sdkPrefix = getSdkPrefix(clientSdk)

            if (sdkPrefix.isNullOrEmpty()) {
                return null
            }

            val split = sdkPrefix.split(Regex("\\d+"), 2)
            if (split.isNullOrEmpty()) {
                return null
            }

            return split[0]
        }

        @JvmStatic
        public fun getLocale(configuration: Configuration): Locale? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val locales = configuration.locales
                if (!locales.isEmpty) {
                    return locales[0]
                }
            } else {
                return configuration.locale
            }

            return null
        }

        private fun getLogger(): ILogger {
            return MotrackFactory.getLogger()
        }
    }
}