package com.motrack.sdk

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */


class Constants {
    companion object {
        var ONE_SECOND: Long = 1000
        var ONE_MINUTE: Long = 60 * ONE_SECOND
        var THIRTY_MINUTES: Long = 30 * ONE_MINUTE
        var ONE_HOUR: Long = 2 * THIRTY_MINUTES

        var MAX_INSTALL_REFERRER_RETRIES = 2

        var ACTIVITY_STATE_FILENAME = "MotrackIoActivityState"
        var ATTRIBUTION_FILENAME = "MotrackAttribution"
        var SESSION_CALLBACK_PARAMETERS_FILENAME = "MotrackSessionCallbackParameters"
        var SESSION_PARTNER_PARAMETERS_FILENAME = "MotrackSessionPartnerParameters"

        var ENCODING = "UTF-8"
        var MALFORMED = "malformed"
        var REFTAG = "reftag"
        var INSTALL_REFERRER = "install_referrer"

        var MD5 = "MD5"
        var SHA1 = "SHA-1"
        var SHA256 = "SHA-256"


        const val LOGTAG = "MoTrack"
        var CLIENT_SDK = "android4.28.5"
        var THREAD_PREFIX = "Motrack-"

        var REFERRER_API_GOOGLE = "google"
        var REFERRER_API_HUAWEI = "huawei"
        var DEEPLINK = "deeplink"
        var PUSH = "push"


        var PREINSTALL = "preinstall"
        var SYSTEM_PROPERTIES = "system_properties"
        var SYSTEM_PROPERTIES_REFLECTION = "system_properties_reflection"
        var SYSTEM_PROPERTIES_PATH = "system_properties_path"
        var SYSTEM_PROPERTIES_PATH_REFLECTION = "system_properties_path_reflection"
        var CONTENT_PROVIDER = "content_provider"
        var CONTENT_PROVIDER_INTENT_ACTION = "content_provider_intent_action"
        var CONTENT_PROVIDER_NO_PERMISSION = "content_provider_no_permission"
        var FILE_SYSTEM = "file_system"
        var SYSTEM_INSTALLER_REFERRER = "system_installer_referrer"

        var MOTRACK_PREINSTALL_SYSTEM_PROPERTY_PREFIX = "motrack.preinstall."
        var MOTRACK_PREINSTALL_SYSTEM_PROPERTY_PATH = "motrack.preinstall.path"
        var MOTRACK_PREINSTALL_CONTENT_URI_AUTHORITY = "com.motrack.preinstall"
        var MOTRACK_PREINSTALL_CONTENT_URI_PATH = "trackers"
        var MOTRACK_PREINSTALL_CONTENT_PROVIDER_INTENT_ACTION = "com.attribution.REFERRAL_PROVIDER"
        var MOTRACK_PREINSTALL_FILE_SYSTEM_PATH = "/data/local/tmp/motrack.preinstall"
        var EXTRA_SYSTEM_INSTALLER_REFERRER = "com.attribution.EXTRA_SYSTEM_INSTALLER_REFERRER"

        var BASE_URL = "https://app.motrack.com"
        var GDPR_URL = "https://gdpr.motrack.com"
        var SUBSCRIPTION_URL = "https://subscription.motrack.com"

        var MINIMAL_ERROR_STATUS_CODE = 400


        var CALLBACK_PARAMETERS = "callback_params"
        var PARTNER_PARAMETERS = "partner_params"

        var FB_AUTH_REGEX = "^(fb|vk)[0-9]{5,}[^:]*://authorize.*access_token=.*"
    }
}