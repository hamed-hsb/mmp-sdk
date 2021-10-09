package com.motrack.sdk

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings.Secure
import com.motrack.sdk.scheduler.SingleThreadFutureScheduler
import java.io.*
import java.math.BigInteger
import java.security.MessageDigest
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.*

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

class Util {
    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'Z"
        val SecondsDisplayFormat: DecimalFormat = newLocalDecimalFormat()
        val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale.US)

        // https://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
        @Volatile
        private var playAdIdScheduler: SingleThreadFutureScheduler? = null

        private fun newLocalDecimalFormat(): DecimalFormat {
            val symbols = DecimalFormatSymbols(Locale.US)
            return DecimalFormat("0.0", symbols)
        }

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

        fun getAndroidId(context: Context?): String? {
            return Secure.getString(context!!.contentResolver, Secure.ANDROID_ID)
        }

        fun sha1(text: String): String? {
            return hash(text, Constants.SHA1)
        }

        fun sha256(text: String): String? {
            return hash(text, Constants.SHA256)
        }

        fun md5(text: String): String? {
            return hash(text, Constants.MD5)
        }

        fun hash(text: String, method: String?): String? {
            var hashString: String? = null
            try {
                val bytes: ByteArray = text.toByteArray()
                val mesd = MessageDigest.getInstance(method)
                mesd.update(bytes, 0, bytes.size)
                val hash = mesd.digest()
                hashString = Util.convertToHex(hash)
            } catch (e: java.lang.Exception) {
            }
            return hashString
        }

        fun convertToHex(bytes: ByteArray): String? {
            val bigInt = BigInteger(1, bytes)
            val formatString = "%0" + (bytes.size shl 1) + "x"
            return String.format(Locale.US, formatString, bigInt)
        }

        fun getSdkVersion(): String {
            return Constants.CLIENT_SDK
        }

        fun resolveContentProvider(
            applicationContext: Context,
            authority: String
        ): Boolean {
            return try {
                applicationContext.packageManager
                    .resolveContentProvider(authority, 0) != null
            } catch (e: java.lang.Exception) {
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

        fun hasRootCause(ex: java.lang.Exception): Boolean {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            ex.printStackTrace(pw)
            val sStackTrace = sw.toString() // stack trace as a string
            return sStackTrace.contains("Caused by:")
        }

        fun getRootCause(ex: java.lang.Exception): String? {
            if (!hasRootCause(ex)) {
                return null
            }
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            ex.printStackTrace(pw)
            val sStackTrace = sw.toString() // stack trace as a string
            val startOccurrenceOfRootCause = sStackTrace.indexOf("Caused by:")
            val endOccurrenceOfRootCause = sStackTrace.indexOf("\n", startOccurrenceOfRootCause)
            return sStackTrace.substring(startOccurrenceOfRootCause, endOccurrenceOfRootCause)
        }

        fun getPlayAdId(
            context: Context,
            advertisingInfoObject: Any?,
            timeoutMilli: Long
        ): String? {
            return runSyncInPlayAdIdSchedulerWithTimeout<String>(
                context,
                Callable { Reflection.getPlayAdId(context, advertisingInfoObject!!)!! },
                timeoutMilli
            )
        }

        fun isPlayTrackingEnabled(
            context: Context,
            advertisingInfoObject: Any?,
            timeoutMilli: Long
        ): Boolean? {
            return runSyncInPlayAdIdSchedulerWithTimeout<Boolean>(
                context,
                { Reflection.isPlayTrackingEnabled(context, advertisingInfoObject!!)!! },
                timeoutMilli
            )
        }

        private fun <R> runSyncInPlayAdIdSchedulerWithTimeout(
            context: Context,
            callable: Callable<R?>,
            timeoutMilli: Long
        ): R? {
            if (playAdIdScheduler == null) {
                synchronized(Util::class.java) {
                    if (playAdIdScheduler == null) {
                        playAdIdScheduler =
                            SingleThreadFutureScheduler("PlayAdIdLibrary", true)
                    }
                }
            }
            val playAdIdFuture: ScheduledFuture<R?> =
                playAdIdScheduler!!.scheduleFutureWithReturn(callable, 0)!!
            try {
                return playAdIdFuture[timeoutMilli, TimeUnit.MILLISECONDS]
            } catch (e: ExecutionException) {
            } catch (e: InterruptedException) {
            } catch (e: TimeoutException) {
            }
            return null
        }

        fun getAdvertisingInfoObject(context: Context, timeoutMilli: Long): Any? {
            val callable = Callable {
                try {
                    return@Callable Reflection.getAdvertisingInfoObject(context)!!
                } catch (e: java.lang.Exception) {
                    return@Callable null
                }
            }
            return runSyncInPlayAdIdSchedulerWithTimeout<Any>(
                context,
                callable,
                timeoutMilli
            )
        }

        fun createUuid(): String {
            return UUID.randomUUID().toString()
        }

        fun <T> readObject(
            context: Context,
            filename: String?,
            objectName: String?,
            type: Class<T>
        ): T? {
            var closable: Closeable? = null
            var resultObject: T? = null

            try {
                val inputStream = context.openFileInput(filename)
                closable = inputStream

                val bufferedStream = BufferedInputStream(inputStream)
                closable = bufferedStream

                val objectStream = ObjectInputStream(bufferedStream)
                closable = objectStream

                try {
                    resultObject = type.cast(objectStream.readObject())
                    getLogger().debug("Read $objectName: $objectName")

                } catch (e: ClassNotFoundException) {
                    getLogger().error("Failed to find $objectName class (${e.message})")

                } catch (e: ClassCastException) {
                    getLogger().error("Failed to cast $objectName object (${e.message})")

                } catch (e: java.lang.Exception) {
                    getLogger().error("Failed to read $objectName object (${e.message})")
                }

            } catch (e: FileNotFoundException) {
                getLogger().debug("$objectName file not found")

            } catch (e: java.lang.Exception) {
                getLogger().error("Failed to open $objectName file for reading ($e)")
            }

            try {
                closable?.close()
            } catch (e: java.lang.Exception) {
                getLogger().error("Failed to close $objectName file for reading ($e)")
            }
            return resultObject
        }

        private fun getLogger(): ILogger {
            return MotrackFactory.getLogger()
        }
    }
}