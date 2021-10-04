package com.motrack.sdk

import android.content.Context
import android.content.pm.PackageManager

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

class Util {
    companion object {
        public fun checkPermission(context: Context, permission: String): Boolean {
            return try {
                val result = context.checkCallingOrSelfPermission(permission)
                result == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                getLogger().debug("Unable to check permission $permission, with message ${e.message}")
                false
            }
        }

        private fun getLogger(): ILogger {
            return MotrackFactory.getLogger()
        }
    }
}