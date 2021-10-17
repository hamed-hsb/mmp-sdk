package com.motrack.sdk

import android.net.Uri
import android.net.UrlQuerySanitizer
import android.net.UrlQuerySanitizer.ParameterValuePair
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 12th October 2021
 */

class PackageFactory {
    companion object {

        private const val MOTRACK_PREFIX = "motrack_"

        fun buildReftagSdkClickPackage(
            rawReferrer: String?,
            clickTime: Long,
            activityState: ActivityState,
            motrackConfig: MotrackConfig,
            deviceInfo: DeviceInfo,
            sessionParameters: SessionParameters
        ): ActivityPackage? {
            if (rawReferrer == null || rawReferrer.isEmpty()) {
                return null
            }
            var referrer: String?
            try {
                referrer = URLDecoder.decode(rawReferrer, Constants.ENCODING)
            } catch (e: UnsupportedEncodingException) {
                referrer = Constants.MALFORMED
                MotrackFactory.getLogger()
                    .error("Referrer decoding failed due to UnsupportedEncodingException. Message: (${e.message})")
            } catch (e: IllegalArgumentException) {
                referrer = Constants.MALFORMED
                MotrackFactory.getLogger()
                    .error("Referrer decoding failed due to IllegalArgumentException. Message: (${e.message})")
            } catch (e: Exception) {
                referrer = Constants.MALFORMED
                MotrackFactory.getLogger()
                    .error("Referrer decoding failed. Message: (${e.message})")
            }
            MotrackFactory.getLogger().verbose("Referrer to parse ($referrer)")
            val querySanitizer = UrlQuerySanitizer()
            querySanitizer.unregisteredParameterValueSanitizer =
                UrlQuerySanitizer.getAllButNulLegal()
            querySanitizer.allowUnregisteredParamaters = true
            querySanitizer.parseQuery(referrer)
            val clickPackageBuilder: PackageBuilder =
                queryStringClickPackageBuilder(
                    querySanitizer.parameterList,
                    activityState,
                    motrackConfig,
                    deviceInfo,
                    sessionParameters
                ) ?: return null
            clickPackageBuilder.referrer = referrer
            clickPackageBuilder.clickTimeInMilliseconds = clickTime
            clickPackageBuilder.rawReferrer = rawReferrer
            return clickPackageBuilder.buildClickPackage(Constants.REFTAG)
        }

        fun buildDeeplinkSdkClickPackage(
            url: Uri?,
            clickTime: Long,
            activityState: ActivityState,
            motrackConfig: MotrackConfig,
            deviceInfo: DeviceInfo,
            sessionParameters: SessionParameters
        ): ActivityPackage? {
            if (url == null) {
                return null
            }
            val urlString = url.toString()
            if (urlString.isEmpty()) {
                return null
            }
            var urlStringDecoded: String?
            try {
                urlStringDecoded = URLDecoder.decode(urlString, Constants.ENCODING)
            } catch (e: UnsupportedEncodingException) {
                urlStringDecoded = urlString
                MotrackFactory.getLogger()
                    .error("Deeplink url decoding failed due to UnsupportedEncodingException. Message: (${e.message})")
            } catch (e: IllegalArgumentException) {
                urlStringDecoded = urlString
                MotrackFactory.getLogger()
                    .error("Deeplink url decoding failed due to IllegalArgumentException. Message: (${e.message})")
            } catch (e: Exception) {
                urlStringDecoded = urlString
                MotrackFactory.getLogger()
                    .error("Deeplink url decoding failed. Message: (${e.message})")
            }
            MotrackFactory.getLogger().verbose("Url to parse (${urlStringDecoded})")
            val querySanitizer = UrlQuerySanitizer()
            querySanitizer.unregisteredParameterValueSanitizer =
                UrlQuerySanitizer.getAllButNulLegal()
            querySanitizer.allowUnregisteredParamaters = true
            querySanitizer.parseUrl(urlStringDecoded)
            val clickPackageBuilder: PackageBuilder =
                queryStringClickPackageBuilder(
                    querySanitizer.parameterList,
                    activityState,
                    motrackConfig,
                    deviceInfo,
                    sessionParameters
                ) ?: return null
            clickPackageBuilder.deeplink = url.toString()
            clickPackageBuilder.clickTimeInMilliseconds = clickTime
            return clickPackageBuilder.buildClickPackage(Constants.DEEPLINK)
        }

        fun buildInstallReferrerSdkClickPackage(
            referrerDetails: ReferrerDetails,
            referrerApi: String,
            activityState: ActivityState,
            motrackConfig: MotrackConfig,
            deviceInfo: DeviceInfo,
            sessionParameters: SessionParameters
        ): ActivityPackage {
            val now = System.currentTimeMillis()
            val clickPackageBuilder = PackageBuilder(
                motrackConfig,
                deviceInfo,
                activityState,
                sessionParameters,
                now
            )
            clickPackageBuilder.referrer = referrerDetails.installReferrer
            clickPackageBuilder.clickTimeInSeconds = referrerDetails.referrerClickTimestampSeconds
            clickPackageBuilder.installBeginTimeInSeconds =
                referrerDetails.installBeginTimestampSeconds
            clickPackageBuilder.clickTimeServerInSeconds =
                referrerDetails.referrerClickTimestampServerSeconds
            clickPackageBuilder.installBeginTimeServerInSeconds =
                referrerDetails.installBeginTimestampServerSeconds
            clickPackageBuilder.installVersion = referrerDetails.installVersion
            clickPackageBuilder.googlePlayInstant = referrerDetails.googlePlayInstant
            clickPackageBuilder.referrerApi = referrerApi
            return clickPackageBuilder.buildClickPackage(Constants.INSTALL_REFERRER)
        }

        fun buildPreinstallSdkClickPackage(
            preinstallPayload: String?,
            preinstallLocation: String?,
            activityState: ActivityState,
            motrackConfig: MotrackConfig,
            deviceInfo: DeviceInfo,
            sessionParameters: SessionParameters
        ): ActivityPackage? {
            if (preinstallPayload.isNullOrEmpty()) {
                return null
            }
            val now = System.currentTimeMillis()
            val clickPackageBuilder = PackageBuilder(
                motrackConfig,
                deviceInfo,
                activityState,
                sessionParameters,
                now
            )
            clickPackageBuilder.preinstallPayload = preinstallPayload
            clickPackageBuilder.preinstallLocation = preinstallLocation
            return clickPackageBuilder.buildClickPackage(Constants.PREINSTALL)
        }

        private fun queryStringClickPackageBuilder(
            queryList: List<ParameterValuePair>?,
            activityState: ActivityState?,
            motrackConfig: MotrackConfig,
            deviceInfo: DeviceInfo,
            sessionParameters: SessionParameters
        ): PackageBuilder? {
            if (queryList == null) {
                return null
            }
            val queryStringParameters: MutableMap<String, String> = LinkedHashMap()
            val queryStringAttribution = MotrackAttribution()
            for (parameterValuePair in queryList) {
                readQueryString(
                    parameterValuePair.mParameter,
                    parameterValuePair.mValue,
                    queryStringParameters,
                    queryStringAttribution
                )
            }
            val now = System.currentTimeMillis()
            val reftag = queryStringParameters.remove(Constants.REFTAG)

            // Check if activity state != null
            // (referrer can be called before onResume)
            if (activityState != null) {
                val lastInterval = now - activityState.lastActivity
                activityState.lastInterval = lastInterval
            }
            val builder = PackageBuilder(
                motrackConfig,
                deviceInfo,
                activityState,
                sessionParameters,
                now
            )
            builder.extraParameters = queryStringParameters
            builder.attribution = queryStringAttribution
            builder.reftag = reftag
            return builder
        }

        private fun readQueryString(
            key: String?,
            value: String?,
            extraParameters: MutableMap<String, String>,
            queryStringAttribution: MotrackAttribution
        ): Boolean {
            if (key == null || value == null) {
                return false
            }

            // Parameter key does not start with "motrack_" prefix.
            if (!key.startsWith(MOTRACK_PREFIX)) {
                return false
            }
            val keyWOutPrefix = key.substring(MOTRACK_PREFIX.length)
            if (keyWOutPrefix.isEmpty()) {
                return false
            }
            if (value.isEmpty()) {
                return false
            }
            if (!tryToSetAttribution(queryStringAttribution, keyWOutPrefix, value)) {
                extraParameters[keyWOutPrefix] = value
            }
            return true
        }

        private fun tryToSetAttribution(
            queryStringAttribution: MotrackAttribution,
            key: String,
            value: String
        ): Boolean {
            if (key == "tracker") {
                queryStringAttribution.trackerName = value
                return true
            }
            if (key == "campaign") {
                queryStringAttribution.campaign = value
                return true
            }
            if (key == "adgroup") {
                queryStringAttribution.adgroup = value
                return true
            }
            if (key == "creative") {
                queryStringAttribution.creative = value
                return true
            }
            return false
        }

    }
}