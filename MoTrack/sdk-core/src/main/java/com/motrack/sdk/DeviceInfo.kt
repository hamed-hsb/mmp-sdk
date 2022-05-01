package com.motrack.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Configuration.*
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 06th October 2021
 */

class DeviceInfo(private val context: Context, sdkPrefix: String?) {
    var playAdId: String? = null
    var playAdIdSource: String? = null
    var playAdIdAttempt = -1
    var isTrackingEnabled: Boolean? = false
    private var nonGoogleIdsReadOnce: Boolean = false
    var androidId: String? = null
    lateinit var fabAttributionId: String
    var clientSdk: String
    var packageName: String?
    var appVersion: String?
    var deviceType: String?
    var deviceName: String
    var deviceManufacturer: String
    public var osName: String
    public var osVersion: String
    var apiLevel: String
    public var language: String?
    var country: String?
    var screenSize: String?
    var screenFormat: String?
    var screenDensity: String?
    var displayWidth: String
    var displayHeight: String
    var hardwareName: String
    var abi: String?
    public var buildName: String
    var appInstallTime: String?
    public var appUpdateTime: String?
    var uiMode: Int? = null
    var fbAttributionId: String?


    init {
        val displayMetric = context.resources.displayMetrics
        val configuration = context.resources.configuration
        val locale = Util.getLocale(configuration)
        val screenLayout = configuration.screenLayout

        packageName = getPackageName(context)
        appVersion = getAppVersion(context)
        deviceType = getDeviceType(configuration)
        deviceName = fetchDeviceName()
        deviceManufacturer = fetchDeviceManufacturer()
        osName = fetchOsName()
        osVersion = fetchOsVersion()
        apiLevel = fetchApiLevel()
        language = getLanguage(locale)
        country = getCountry(locale)
        screenSize = getScreenSize(screenLayout)
        screenFormat = getScreenFormat(screenLayout)
        screenDensity = getScreenDensity(displayMetric)
        displayWidth = getDisplayWidth(displayMetric)
        displayHeight = getDisplayHigh(displayMetric)
        clientSdk = getClientSdk(sdkPrefix)
        fbAttributionId = getFacebookAttributionId(context)
        hardwareName = getHardWareName()
        abi = getABI()
        buildName = fetchBuildName()
        appInstallTime = getAppInstallTime(context)
        appUpdateTime = getAppUpdateTime(context)
        uiMode = getDeviceUiMode(configuration)

    }

    private fun getDeviceUiMode(configuration: Configuration?): Int {
        return configuration!!.uiMode and UI_MODE_TYPE_MASK
    }

    private fun getAppUpdateTime(context: Context): String? {
        return try {

            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            Util.dateFormatter.format(Date(packageInfo.lastUpdateTime))

        } catch (e: Exception) {
            null
        }
    }

    private fun getAppInstallTime(context: Context): String? {
        return try {

            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            Util.dateFormatter.format(Date(packageInfo.firstInstallTime))

        } catch (e: Exception) {
            null
        }
    }

    private fun fetchBuildName(): String {
        return Build.ID
    }

    private fun getABI(): String? {
        val supportedAbis = getSupportedAbis()

        // SUPPORTED_ABIS is only supported in API level 21
        // get CPU_ABI instead
        if (supportedAbis.isNullOrEmpty()) {
            return getCpuAbi()
        }
        return supportedAbis[0]
    }

    private fun getSupportedAbis(): Array<String?>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS
        } else null
    }

    private fun getCpuAbi(): String? {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Build.CPU_ABI
        } else null
    }

    private fun getHardWareName(): String {
        return Build.DISPLAY
    }

    private fun getClientSdk(sdkPrefix: String?): String {
        return when {
            sdkPrefix.isNullOrEmpty() -> Constants.CLIENT_SDK
            else -> "$sdkPrefix@${Constants.CLIENT_SDK}"
        }
    }

    private fun getDisplayHigh(displayMetric: DisplayMetrics): String {
        return displayMetric.heightPixels.toString()
    }

    private fun getDisplayWidth(displayMetric: DisplayMetrics): String {
        return displayMetric.widthPixels.toString()
    }

    private fun getScreenDensity(displayMetric: DisplayMetrics): String? {
        val density = displayMetric.densityDpi
        val low = (DisplayMetrics.DENSITY_MEDIUM + DisplayMetrics.DENSITY_LOW) / 2
        val high = (DisplayMetrics.DENSITY_MEDIUM + DisplayMetrics.DENSITY_HIGH) / 2
        return when {
            (density == 0) -> null
            (density < low) -> LOW
            (density > high) -> HIGH
            else -> MEDIUM
        }
    }

    private fun getScreenFormat(screenLayout: Int): String? {
        val screenFormat = screenLayout and SCREENLAYOUT_LONG_MASK
        return when (screenFormat) {
            SCREENLAYOUT_LONG_YES -> LONG
            SCREENLAYOUT_LONG_NO -> NORMAL
            SCREENLAYOUT_LONG_UNDEFINED -> UNDEFINED
            else -> null
        }
    }

    private fun getScreenSize(screenLayout: Int): String? {
        val screenSize = screenLayout and SCREENLAYOUT_SIZE_MASK
        return when (screenSize) {
            SCREENLAYOUT_SIZE_SMALL -> SMALL
            SCREENLAYOUT_SIZE_NORMAL -> NORMAL
            SCREENLAYOUT_SIZE_LARGE -> LARGE
            SCREENLAYOUT_SIZE_XLARGE -> XLARGE
            else -> null
        }
    }

    private fun getCountry(locale: Locale?): String? {
        return locale?.country
    }

    private fun getLanguage(locale: Locale?): String? {
        return locale?.language
    }

    private fun fetchApiLevel(): String {
        return "" + Build.VERSION.SDK_INT
    }

    private fun fetchOsVersion(): String {
        return Build.VERSION.RELEASE
    }

    private fun fetchOsName(): String {
        return ANDROID_OS_NAME
    }

    private fun fetchDeviceManufacturer(): String {
        return Build.MANUFACTURER
    }

    private fun fetchDeviceName(): String {
        return Build.MODEL
    }

    private fun getDeviceType(configuration: Configuration): String? {
        val uiMode = configuration.uiMode and UI_MODE_TYPE_MASK
        if (uiMode == UI_MODE_TYPE_TELEVISION) {
            return "tv"
        }

        val screenSize = configuration.screenLayout and SCREENLAYOUT_SIZE_MASK
        return when (screenSize) {
            SCREENLAYOUT_SIZE_SMALL, SCREENLAYOUT_SIZE_NORMAL -> return "phone"
            SCREENLAYOUT_SIZE_LARGE, SCREENLAYOUT_SIZE_XLARGE -> return "tablet"
            else -> null
        }
    }

    private fun getAppVersion(context: Context): String? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            null
        }
    }

    private fun getPackageName(context: Context): String {
        return context.packageName
    }

    fun reloadPlayIds( motrackConfig: MotrackConfig) {

        if (!canReadPlayIds(motrackConfig)) {
            return
        }
        val context: Context? = motrackConfig.context
        val previousPlayAdId: String? = playAdId
        val previousIsTrackingEnabled = isTrackingEnabled
        playAdId = null
        isTrackingEnabled = null
        playAdIdSource = null
        playAdIdAttempt = -1


        // attempt connecting to Google Play Service by own
        var serviceAttempt = 1
        while (serviceAttempt <= 3) {
            try {
                // timeout is a multiplier of the attempt number with 3 seconds
                // so first 3 seconds, second 6 seconds and third and last 9 seconds
                val timeoutServiceMilli = (Constants.ONE_SECOND * 3 * serviceAttempt)
                val gpsInfo: GooglePlayServicesClient.Companion.GooglePlayServicesInfo =
                    GooglePlayServicesClient.getGooglePlayServicesInfo(
                        context!!,
                        timeoutServiceMilli
                    )!!
                if (playAdId == null) {
                    playAdId = gpsInfo.gpsAdid
                }
                if (isTrackingEnabled == null) {
                    isTrackingEnabled = gpsInfo.isTrackingEnabled
                }
                if (playAdId != null && isTrackingEnabled != null) {
                    playAdIdSource = "service"
                    playAdIdAttempt = serviceAttempt
                    return
                }
            } catch (e: java.lang.Exception) {
            }
            serviceAttempt += 1
        }

        // as fallback attempt connecting to Google Play Service using library
        var libAttempt = 1
        while (libAttempt <= 3) {

            // timeout inside library is 10 seconds, so 10 + 1 seconds are given
            val advertisingInfoObject: Any? = Util.getAdvertisingInfoObject(
                context!!, Constants.ONE_SECOND * 11
            )
            if (advertisingInfoObject == null) {
                libAttempt += 1
                continue
            }
            if (playAdId == null) {
                // just needs a short timeout since it should be just accessing a POJO
                playAdId = Util.getPlayAdId(
                    context, advertisingInfoObject, Constants.ONE_SECOND
                )
            }
            if (isTrackingEnabled == null) {
                // just needs a short timeout since it should be just accessing a POJO
                isTrackingEnabled = Util.isPlayTrackingEnabled(
                    context, advertisingInfoObject, Constants.ONE_SECOND
                )
            }
            if (playAdId != null && isTrackingEnabled != null) {
                playAdIdSource = "library"
                playAdIdAttempt = libAttempt
                return
            }
            libAttempt += 1
        }

        // if both weren't found, use previous values
        if (playAdId == null) {
            playAdId = previousPlayAdId
        }
        if (isTrackingEnabled == null) {
            isTrackingEnabled = previousIsTrackingEnabled
        }
    }

    fun reloadNonPlayIds(motrackConfig: MotrackConfig) {

        if (!canReadNonPlayIds(motrackConfig)) {
            return
        }
        if (nonGoogleIdsReadOnce) {
            return
        }
        androidId = Util.getAndroidId(motrackConfig.context)
        nonGoogleIdsReadOnce = true
    }

    private fun getFacebookAttributionId(context: Context): String? {
        return try {
            @SuppressLint("PackageManagerGetSignatures") val signatures =
                context.packageManager.getPackageInfo(
                    "com.facebook.katana",
                    PackageManager.GET_SIGNATURES
                ).signatures
            if (signatures == null || signatures.size != 1) {
                // Unable to find the correct signatures for this APK
                return null
            }
            val facebookApkSignature = signatures[0]
            if (OFFICIAL_FACEBOOK_SIGNATURE != facebookApkSignature.toCharsString()) {
                // not the official Facebook application
                return null
            }
            val contentResolver = context.contentResolver
            val uri = Uri.parse("content://com.facebook.katana.provider.AttributionIdProvider")
            val columnName = "aid"
            val projection = arrayOf(columnName)
            val cursor = contentResolver.query(uri, projection, null, null, null) ?: return null
            if (!cursor.moveToFirst()) {
                cursor.close()
                return null
            }
            val attributionId = cursor.getString(cursor.getColumnIndex(columnName))
            cursor.close()
            attributionId
        } catch (e: java.lang.Exception) {
            null
        }
    }

    companion object {
        const val ANDROID_OS_NAME = "android"
        const val SMALL = "small"
        const val NORMAL = "normal"
        const val UNDEFINED = "undefined"
        const val LONG = "long"
        const val LARGE = "large"
        const val XLARGE = "xlarge"
        const val LOW = "low"
        const val MEDIUM = "medium"
        const val HIGH = "high"
        private const val OFFICIAL_FACEBOOK_SIGNATURE =
            "30820268308201d102044a9c4610300d06092a864886f70d0101040500307a310b3009060355040613" +
                    "025553310b3009060355040813024341311230100603550407130950616c6f20416c746f31" +
                    "183016060355040a130f46616365626f6f6b204d6f62696c653111300f060355040b130846" +
                    "616365626f6f6b311d301b0603550403131446616365626f6f6b20436f72706f726174696f" +
                    "6e3020170d3039303833313231353231365a180f32303530303932353231353231365a307a" +
                    "310b3009060355040613025553310b30090603550408130243413112301006035504071309" +
                    "50616c6f20416c746f31183016060355040a130f46616365626f6f6b204d6f62696c653111" +
                    "300f060355040b130846616365626f6f6b311d301b0603550403131446616365626f6f6b20" +
                    "436f72706f726174696f6e30819f300d06092a864886f70d010101050003818d0030818902" +
                    "818100c207d51df8eb8c97d93ba0c8c1002c928fab00dc1b42fca5e66e99cc3023ed2d214d" +
                    "822bc59e8e35ddcf5f44c7ae8ade50d7e0c434f500e6c131f4a2834f987fc46406115de201" +
                    "8ebbb0d5a3c261bd97581ccfef76afc7135a6d59e8855ecd7eacc8f8737e794c60a761c536" +
                    "b72b11fac8e603f5da1a2d54aa103b8a13c0dbc10203010001300d06092a864886f70d0101" +
                    "040500038181005ee9be8bcbb250648d3b741290a82a1c9dc2e76a0af2f2228f1d9f9c4007" +
                    "529c446a70175c5a900d5141812866db46be6559e2141616483998211f4a673149fb2232a1" +
                    "0d247663b26a9031e15f84bc1c74d141ff98a02d76f85b2c8ab2571b6469b232d8e768a7f7" +
                    "ca04f7abe4a775615916c07940656b58717457b42bd928a2"
    }


    fun canReadPlayIds(motrackConfig: MotrackConfig): Boolean {
        if (motrackConfig.playStoreKidsAppEnabled != null && motrackConfig.playStoreKidsAppEnabled!!) {
            return false
        }
        return if (motrackConfig.coppaCompliantEnabled != null && motrackConfig.coppaCompliantEnabled!!) {
            false
        } else true
    }

    fun canReadNonPlayIds(motrackConfig: MotrackConfig): Boolean {
        if (motrackConfig.playStoreKidsAppEnabled != null && motrackConfig.playStoreKidsAppEnabled!!) {
            return false
        }
        return if (motrackConfig.coppaCompliantEnabled != null && motrackConfig.coppaCompliantEnabled!!) {
            false
        } else true
    }
}