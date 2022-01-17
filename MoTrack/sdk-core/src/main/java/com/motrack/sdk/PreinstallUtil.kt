package com.motrack.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 10th October 2021
 */

class PreinstallUtil {
    companion object {


        private const val SYSTEM_PROPERTY_BITMASK: Long = 1                  //00...000000001
        private const val SYSTEM_PROPERTY_REFLECTION_BITMASK: Long = 2       //00...000000010
        private const val SYSTEM_PROPERTY_PATH_BITMASK: Long = 4             //00...000000100
        private const val SYSTEM_PROPERTY_PATH_REFLECTION_BITMASK: Long = 8  //00...000001000
        private const val CONTENT_PROVIDER_BITMASK: Long = 16                //00...000010000
        private const val CONTENT_PROVIDER_INTENT_ACTION_BITMASK: Long = 32  //00...000100000
        private const val FILE_SYSTEM_BITMASK: Long = 64                     //00...001000000
        private const val CONTENT_PROVIDER_NO_PERMISSION_BITMASK: Long = 128 //00...010000000


        // bitwise OR (|) of all above locations
        private const val ALL_LOCATION_BITMASK = SYSTEM_PROPERTY_BITMASK or
                SYSTEM_PROPERTY_REFLECTION_BITMASK or
                SYSTEM_PROPERTY_PATH_BITMASK or
                SYSTEM_PROPERTY_PATH_REFLECTION_BITMASK or
                CONTENT_PROVIDER_BITMASK or
                CONTENT_PROVIDER_INTENT_ACTION_BITMASK or
                FILE_SYSTEM_BITMASK or
                CONTENT_PROVIDER_NO_PERMISSION_BITMASK //00...011111111


        fun hasAllLocationsBeenRead(status: Long): Boolean {
            // Check if the given status has none of the valid location with bit `0`, indicating it has
            // not been read
            return status and ALL_LOCATION_BITMASK == ALL_LOCATION_BITMASK
        }

        fun hasNotBeenRead(location: String?, status: Long): Boolean {
            // Check if the given status has bit '0` (not `1`) for the given location, indicating it has not been read
            when (location) {
                Constants.SYSTEM_PROPERTIES -> return status and SYSTEM_PROPERTY_BITMASK != SYSTEM_PROPERTY_BITMASK
                Constants.SYSTEM_PROPERTIES_REFLECTION -> return status and SYSTEM_PROPERTY_REFLECTION_BITMASK != SYSTEM_PROPERTY_REFLECTION_BITMASK
                Constants.SYSTEM_PROPERTIES_PATH -> return status and SYSTEM_PROPERTY_PATH_BITMASK != SYSTEM_PROPERTY_PATH_BITMASK
                Constants.SYSTEM_PROPERTIES_PATH_REFLECTION -> return status and SYSTEM_PROPERTY_PATH_REFLECTION_BITMASK != SYSTEM_PROPERTY_PATH_REFLECTION_BITMASK
                Constants.CONTENT_PROVIDER -> return status and CONTENT_PROVIDER_BITMASK != CONTENT_PROVIDER_BITMASK
                Constants.CONTENT_PROVIDER_INTENT_ACTION -> return status and CONTENT_PROVIDER_INTENT_ACTION_BITMASK != CONTENT_PROVIDER_INTENT_ACTION_BITMASK
                Constants.FILE_SYSTEM -> return status and FILE_SYSTEM_BITMASK != FILE_SYSTEM_BITMASK
                Constants.CONTENT_PROVIDER_NO_PERMISSION -> return status and CONTENT_PROVIDER_NO_PERMISSION_BITMASK != CONTENT_PROVIDER_NO_PERMISSION_BITMASK
            }
            return false
        }

        fun markAsRead(location: String?, status: Long): Long {
            // Set the bit to '1` for the given location, indicating it has been read
            when (location) {
                Constants.SYSTEM_PROPERTIES -> return status or SYSTEM_PROPERTY_BITMASK
                Constants.SYSTEM_PROPERTIES_REFLECTION -> return status or SYSTEM_PROPERTY_REFLECTION_BITMASK
                Constants.SYSTEM_PROPERTIES_PATH -> return status or SYSTEM_PROPERTY_PATH_BITMASK
                Constants.SYSTEM_PROPERTIES_PATH_REFLECTION -> return status or SYSTEM_PROPERTY_PATH_REFLECTION_BITMASK
                Constants.CONTENT_PROVIDER -> return status or CONTENT_PROVIDER_BITMASK
                Constants.CONTENT_PROVIDER_INTENT_ACTION -> return status or CONTENT_PROVIDER_INTENT_ACTION_BITMASK
                Constants.FILE_SYSTEM -> return status or FILE_SYSTEM_BITMASK
                Constants.CONTENT_PROVIDER_NO_PERMISSION -> return status or CONTENT_PROVIDER_NO_PERMISSION_BITMASK
            }
            return status
        }

        fun getPayloadFromSystemProperty(
            packageName: String,
            logger: ILogger
        ): String? {
            return readSystemProperty(
                Constants.MOTRACK_PREINSTALL_SYSTEM_PROPERTY_PREFIX + packageName,
                logger
            )
        }

        fun getPayloadFromSystemPropertyReflection(
            packageName: String,
            logger: ILogger
        ): String? {
            return readSystemPropertyReflection(
                Constants.MOTRACK_PREINSTALL_SYSTEM_PROPERTY_PREFIX + packageName,
                logger
            )
        }

        fun getPayloadFromSystemPropertyFilePath(
            packageName: String,
            logger: ILogger
        ): String? {
            val filePath: String? =
                readSystemProperty(Constants.MOTRACK_PREINSTALL_SYSTEM_PROPERTY_PATH, logger)
            if (filePath == null || filePath.isEmpty()) {
                return null
            }
            val content: String? = readFileContent(filePath, logger)
            return if (content == null || content.isEmpty()) {
                null
            } else readPayloadFromJsonString(content, packageName, logger)
        }

        fun getPayloadFromSystemPropertyFilePathReflection(
            packageName: String,
            logger: ILogger
        ): String? {
            val filePath: String? = readSystemPropertyReflection(
                Constants.MOTRACK_PREINSTALL_SYSTEM_PROPERTY_PATH,
                logger
            )
            if (filePath == null || filePath.isEmpty()) {
                return null
            }
            val content: String? = readFileContent(filePath, logger)
            return if (content == null || content.isEmpty()) {
                null
            } else readPayloadFromJsonString(content, packageName, logger)
        }

        fun getPayloadFromContentProviderDefault(
            context: Context,
            packageName: String,
            logger: ILogger
        ): String? {
            if (!Util.resolveContentProvider(
                    context,
                    Constants.MOTRACK_PREINSTALL_CONTENT_URI_AUTHORITY
                )
            ) {
                return null
            }
            val defaultContentUri: String = String.format(
                "content://%s/%s",
                Constants.MOTRACK_PREINSTALL_CONTENT_URI_AUTHORITY,
                Constants.MOTRACK_PREINSTALL_CONTENT_URI_PATH
            )
            return readContentProvider(context, defaultContentUri, packageName, logger)
        }

        fun getPayloadsFromContentProviderIntentAction(
            context: Context,
            packageName: String,
            logger: ILogger
        ): List<String?>? {
            return readContentProviderIntentAction(
                context,
                packageName,
                Manifest.permission.INSTALL_PACKAGES,
                logger
            )
        }

        fun getPayloadsFromContentProviderNoPermission(
            context: Context,
            packageName: String,
            logger: ILogger
        ): List<String?>? {
            return readContentProviderIntentAction(
                context,
                packageName,
                null,  // no permission
                logger
            )
        }

        fun getPayloadFromFileSystem(
            packageName: String,
            filePath: String?,
            logger: ILogger
        ): String? {
            var content = readFileContent(Constants.MOTRACK_PREINSTALL_FILE_SYSTEM_PATH, logger)
            if (content == null || content.isEmpty()) {
                if (filePath != null && filePath.isNotEmpty()) {
                    content = readFileContent(filePath, logger)
                }
                if (content == null || content.isEmpty()) {
                    return null
                }
            }
            return readPayloadFromJsonString(content, packageName, logger)
        }

        private fun readSystemProperty(
            propertyKey: String,
            logger: ILogger
        ): String? {
            try {
                return System.getProperty(propertyKey)
            } catch (e: Exception) {
                logger.error("Exception read system property key [$propertyKey] error [${e.message!!}]")
            }
            return null
        }

        @SuppressLint("PrivateApi")
        private fun readSystemPropertyReflection(
            propertyKey: String,
            logger: ILogger
        ): String? {
            try {
                val classObject = Class.forName("android.os.SystemProperties")
                val methodObject = classObject.getDeclaredMethod("get", String::class.java)
                return methodObject.invoke(classObject, propertyKey) as String
            } catch (e: Exception) {
                logger.error("Exception read system property using reflection key [$propertyKey] error [${e.message}]")
            }
            return null
        }

        private fun readContentProvider(
            context: Context,
            contentUri: String,
            packageName: String,
            logger: ILogger
        ): String? {
            return try {
                val contentResolver = context.contentResolver
                val uri = Uri.parse(contentUri)
                val encryptedDataColumn = "encrypted_data"
                val projection = arrayOf(encryptedDataColumn)
                val selection = "package_name=?"
                val selectionArgs = arrayOf(packageName)
                val cursor = contentResolver.query(
                    uri, projection,
                    selection, selectionArgs, null
                )
                if (cursor == null) {
                    logger.debug("Read content provider cursor null content uri [$contentUri]")
                    return null
                }
                if (!cursor.moveToFirst()) {
                    logger.debug("Read content provider cursor empty content uri [$contentUri]")
                    cursor.close()
                    return null
                }
                val payload = cursor.getString(0)
                cursor.close()
                payload
            } catch (e: Exception) {
                logger.error("Exception read content provider uri [$contentUri] error [${e.message!!}]")
                null
            }
        }

        private fun readContentProviderIntentAction(
            context: Context,
            packageName: String,
            permission: String?,
            logger: ILogger
        ): List<String?>? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val providers = context.packageManager
                    .queryIntentContentProviders(
                        Intent(Constants.MOTRACK_PREINSTALL_CONTENT_PROVIDER_INTENT_ACTION), 0
                    )
                val payloads: MutableList<String?> = ArrayList()
                for (provider in providers) {
                    var permissionGranted = true
                    if (permission != null) {
                        val result = context.packageManager.checkPermission(
                            permission, provider.providerInfo.packageName
                        )
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            permissionGranted = false
                        }
                    }
                    if (permissionGranted) {
                        val authority = provider.providerInfo.authority
                        if (authority != null && authority.isNotEmpty()) {
                            val contentUri: String = String.format(
                                "content://%s/%s",
                                authority, Constants.MOTRACK_PREINSTALL_CONTENT_URI_PATH
                            )
                            val payload =
                                readContentProvider(context, contentUri, packageName, logger)
                            if (payload != null && payload.isNotEmpty()) {
                                payloads.add(payload)
                            }
                        }
                    }
                }
                if (payloads.isNotEmpty()) {
                    return payloads
                }
            }
            return null
        }

        private fun readFileContent(filePath: String, logger: ILogger): String? {
            val file = File(filePath)
            if (file.exists() && file.isFile && file.canRead()) {
                try {
                    val length = file.length().toInt()
                    if (length <= 0) {
                        logger.debug("Read file content empty file")
                        return null
                    }
                    val bytes = ByteArray(length)
                    val fileInputStream = FileInputStream(file)
                    try {
                        fileInputStream.read(bytes)
                    } catch (e: Exception) {
                        logger.error("Exception read file input stream error [${e.message!!}]")
                        return null
                    } finally {
                        fileInputStream.close()
                    }
                    return String(bytes)
                } catch (e: Exception) {
                    logger.error("Exception read file content error [${e.message!!}]")
                }
            }
            return null
        }

        private fun readPayloadFromJsonString(
            jsonString: String,
            packageName: String,
            logger: ILogger
        ): String? {
            try {
                val jsonObject = JSONObject(jsonString.trim { it <= ' ' })
                return jsonObject.optString(packageName)
            } catch (e: Exception) {
                logger.error("Exception read payload from json string error [${e.message!!}]")
            }
            return null
        }
    }
}