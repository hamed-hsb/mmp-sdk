package com.motrack.sdk

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.Settings.Secure
import com.motrack.sdk.scheduler.AsyncTaskExecutor
import com.motrack.sdk.scheduler.SingleThreadFutureScheduler
import java.io.*
import java.math.BigInteger
import java.security.MessageDigest
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.*
import kotlin.math.pow

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

class Util {
    companion object {
        private const val fieldReadErrorMessage =
            "Unable to read '%s' field in migration device with message (%s)"

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
        fun checkPermission(context: Context, permission: String): Boolean {
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

        fun sha256(text: String): String? {
            return hash(text, Constants.SHA256)
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

        fun runInBackground(command: Runnable) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                command.run()
                return
            }
            object : AsyncTaskExecutor<Any?, Void?>() {
                override fun doInBackground(params: Array<out Any?>): Void? {
                    val command = params[0] as Runnable
                    command.run()
                    return null
                }
            }.execute(command as Any)
        }

        @JvmStatic
        fun isValidParameter(
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
        fun getSdkPrefix(clientSdk: String): String? {
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
        fun getSdkPrefixPlatform(clientSdk: String): String? {
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
        fun getLocale(configuration: Configuration): Locale? {
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

        fun mergeParameters(
            target: HashMap<String, String>?,
            source: HashMap<String, String>?,
            parameterName: String?
        ): HashMap<String, String>? {
            if (target == null) {
                return source
            }
            if (source == null) {
                return target
            }
            val mergedParameters: HashMap<String, String> = HashMap(target)
            for ((key, value) in source) {
                val oldValue = mergedParameters.put(key, value)
                if (oldValue != null) {
                    getLogger().warn("Key $key with value $oldValue from ${parameterName!!} parameter was replaced by value $value")
                }
            }
            return mergedParameters
        }

        fun getWaitingTime(retries: Int, backoffStrategy: BackoffStrategy): Long {
            if (retries < backoffStrategy.minRetries) {
                return 0
            }
            // start with expon 0
            val expon = retries - backoffStrategy.minRetries
            // get the exponential Time from the power of 2: 1, 2, 4, 8, 16, ... * times the multiplier
            val exponentialTime =
                2.0.pow(expon.toDouble()).toLong() * backoffStrategy.milliSecondMultiplier
            // limit the maximum allowed time to wait
            val ceilingTime = exponentialTime.coerceAtMost(backoffStrategy.maxWait)
            // get the random range
            val randomDouble: Double = randomInRange(
                backoffStrategy.minRange,
                backoffStrategy.maxRange
            )
            // apply jitter factor
            val waitingTime = ceilingTime * randomDouble
            return waitingTime.toLong()
        }

        private fun randomInRange(minRange: Double, maxRange: Double): Double {
            val random = Random()
            val range = maxRange - minRange
            val scaled = random.nextDouble() * range
            return scaled + minRange
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

        fun isUrlFilteredOut(url: Uri?): Boolean {
            if (url == null) {
                return true
            }
            val urlString = url.toString()
            if (urlString.isEmpty()) {
                return true
            }

            // Url with FB credentials to be filtered out
            return urlString.matches(Regex(Constants.FB_AUTH_REGEX))
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

        fun formatString(format: String?, vararg args: Any?): String {
            return String.format(Locale.US, format!!, *args)
        }

        fun equalEnum(first: Enum<*>?, second: Enum<*>?): Boolean {
            return equalObject(first, second)
        }

        fun equalLong(first: Long?, second: Long?): Boolean {
            return equalObject(first, second)
        }

        fun equalInt(first: Int?, second: Int?): Boolean {
            return equalObject(first, second)
        }

        fun hashBoolean(value: Boolean?): Int {
            return value?.hashCode() ?: 0
        }

        fun hashLong(value: Long?): Int {
            return value?.hashCode() ?: 0
        }

        fun hashDouble(value: Double?): Int {
            return value?.hashCode() ?: 0
        }

        fun hashString(value: String?): Int {
            return value?.hashCode() ?: 0
        }

        fun hashEnum(value: Enum<*>?): Int {
            return value?.hashCode() ?: 0
        }

        fun hashObject(value: Any?): Int {
            return value?.hashCode() ?: 0
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

        fun <T> writeObject(anObject: T, context: Context, filename: String, objectName: String) {
            var closable: Closeable? = null
            try {
                val outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE)
                closable = outputStream
                val bufferedStream = BufferedOutputStream(outputStream)
                closable = bufferedStream
                val objectStream = ObjectOutputStream(bufferedStream)
                closable = objectStream

                try {
                    objectStream.writeObject(anObject)
                    getLogger().debug("Wrote $objectName: $anObject")
                } catch (e: NotSerializableException) {
                    getLogger().error("Failed to serialize $objectName")
                } finally {
                    objectStream.flush()
                    objectStream.close()
                }

            } catch (e: java.lang.Exception) {
                getLogger().error("Failed to open $objectName for writing ($e)")
            }
            try {
                closable?.close()
            } catch (e: java.lang.Exception) {
                getLogger().error("Failed to close $objectName file for writing $e)")
            }
        }

        fun readStringField(
            fields: ObjectInputStream.GetField,
            name: String?,
            defaultValue: String?
        ): String? {
            return readObjectField(fields, name, defaultValue)
        }

        fun <T> readObjectField(
            fields: ObjectInputStream.GetField,
            name: String?,
            defaultValue: T
        ): T {
            return try {
                fields[name, defaultValue] as T
            } catch (e: java.lang.Exception) {
                getLogger()
                    .debug(fieldReadErrorMessage, name!!, e.message!!)
                defaultValue
            }
        }

        fun readBooleanField(
            fields: ObjectInputStream.GetField,
            name: String?,
            defaultValue: Boolean
        ): Boolean {
            return try {
                fields[name, defaultValue]
            } catch (e: java.lang.Exception) {
                getLogger()
                    .debug(fieldReadErrorMessage, name!!, e.message!!)
                defaultValue
            }
        }

        fun readIntField(
            fields: ObjectInputStream.GetField,
            name: String?,
            defaultValue: Int
        ): Int {
            return try {
                fields[name, defaultValue]
            } catch (e: java.lang.Exception) {
                getLogger()
                    .debug(fieldReadErrorMessage, name!!, e.message!!)
                defaultValue
            }
        }

        fun readLongField(
            fields: ObjectInputStream.GetField,
            name: String?,
            defaultValue: Long
        ): Long {
            return try {
                fields[name, defaultValue]
            } catch (e: java.lang.Exception) {
                getLogger()
                    .debug(fieldReadErrorMessage, name!!, e.message!!)
                defaultValue
            }
        }

        fun isEqualReferrerDetails(
            referrerDetails: ReferrerDetails,
            referrerApi: String,
            activityState: ActivityState
        ): Boolean {
            if (referrerApi == Constants.REFERRER_API_GOOGLE) {
                return isEqualGoogleReferrerDetails(
                    referrerDetails,
                    activityState
                )
            } else if (referrerApi == Constants.REFERRER_API_HUAWEI_ADS) {
                return isEqualHuaweiReferrerAdsDetails(
                    referrerDetails,
                    activityState
                )
            } else if (referrerApi == Constants.REFERRER_API_HUAWEI_APP_GALLERY) {
                return isEqualHuaweiReferrerAppGalleryDetails(
                    referrerDetails,
                    activityState
                )
            }

            return false
        }

        private fun isEqualHuaweiReferrerAppGalleryDetails(
            referrerDetails: ReferrerDetails,
            activityState: ActivityState
        ): Boolean {
            return referrerDetails.referrerClickTimestampSeconds == activityState.clickTimeHuawei && referrerDetails.installBeginTimestampSeconds == activityState.installBeginHuawei && equalString(
                referrerDetails.installReferrer,
                activityState.installReferrerHuaweiAppGallery
            )
        }


        fun canReadPlayIds(motrackConfig: MotrackConfig): Boolean {
            if (isPlayStoreKidsAppEnabled(motrackConfig)) {
                return false
            }
            return if (isCoppaEnabled(motrackConfig)) {
                false
            } else true
        }

        fun canReadNonPlayIds(motrackConfig: MotrackConfig): Boolean {
            if (isPlayStoreKidsAppEnabled(motrackConfig)) {
                return false
            }
            return if (isCoppaEnabled(motrackConfig)) {
                false
            } else true
        }

        fun isCoppaEnabled(motrackConfig: MotrackConfig): Boolean {
            return if (motrackConfig.coppaCompliantEnabled != null && motrackConfig.coppaCompliantEnabled!!) {
                true
            } else false
        }

        fun isPlayStoreKidsAppEnabled(motrackConfig: MotrackConfig): Boolean {
            return if (motrackConfig.playStoreKidsAppEnabled != null && motrackConfig.playStoreKidsAppEnabled!!) {
                true
            } else false
        }

        fun getImeiParameters(
            motrackConfig: MotrackConfig,
            logger: ILogger?
        ): Map<String, String>? {
            return if (isCoppaEnabled(motrackConfig)) {
                null
            } else Reflection.getImeiParameters(motrackConfig.context, logger)
        }

        fun getOaidParameters(
            motrackConfig: MotrackConfig,
            logger: ILogger?
        ): Map<String, String>? {
            return if (isCoppaEnabled(motrackConfig)) {
                null
            } else Reflection.getOaidParameters(motrackConfig.context, logger)
        }

        fun getFireAdvertisingId(motrackConfig: MotrackConfig): String? {
            return if (isCoppaEnabled(motrackConfig)) {
                null
            } else getFireAdvertisingId(motrackConfig)
        }


        fun getFireTrackingEnabled(motrackConfig: MotrackConfig): Boolean? {
            return if (Util.isCoppaEnabled(motrackConfig)) {
                null
            } else getFireTrackingEnabled(motrackConfig.context.getContentResolver())
        }

        private fun isEqualGoogleReferrerDetails(
            referrerDetails: ReferrerDetails,
            activityState: ActivityState
        ): Boolean {
            return (referrerDetails.referrerClickTimestampSeconds == activityState.clickTime
                    && referrerDetails.installBeginTimestampSeconds == activityState.installBegin
                    && referrerDetails.referrerClickTimestampServerSeconds == activityState.clickTimeServer
                    && referrerDetails.installBeginTimestampServerSeconds == activityState.installBeginServer
                    && equalString(
                referrerDetails.installReferrer,
                activityState.installReferrer
            )
                    && equalString(
                referrerDetails.installVersion,
                activityState.installVersion
            )
                    && equalBoolean(
                referrerDetails.googlePlayInstant,
                activityState.googlePlayInstant
            ))
        }

        private fun isEqualHuaweiReferrerAdsDetails(
            referrerDetails: ReferrerDetails,
            activityState: ActivityState
        ): Boolean {
            return referrerDetails.referrerClickTimestampSeconds == activityState.clickTimeHuawei
                    && referrerDetails.installBeginTimestampSeconds == activityState.installBeginHuawei
                    && equalString(
                referrerDetails.installReferrer,
                activityState.installReferrerHuawei
            )
        }

        fun equalObject(first: Any?, second: Any?): Boolean {
            return if (first == null || second == null) {
                first == null && second == null
            } else first == second
        }

        fun equalsDouble(first: Double?, second: Double?): Boolean {
            return if (first == null || second == null) {
                first == null && second == null
            } else java.lang.Double.doubleToLongBits(first) == java.lang.Double.doubleToLongBits(
                second
            )
        }

        fun equalString(first: String?, second: String?): Boolean {
            return equalObject(first, second)
        }

        fun equalBoolean(first: Boolean?, second: Boolean?): Boolean {
            return equalObject(first, second)
        }

        fun getReasonString(message: String?, throwable: Throwable?): String {
            return if (throwable != null) {
                "$message: $throwable"
            } else {
                "$message"
            }
        }

        private fun getLogger(): ILogger {
            return MotrackFactory.getLogger()
        }
    }
}