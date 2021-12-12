package com.motrack.sdk

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

enum class ActivityKind {
    UNKNOWN,
    SESSION,
    EVENT,
    CLICK,
    ATTRIBUTION,
    REVENUE,
    REATTRIBUTION,
    INFO,
    GDPR,
    AD_REVENUE,
    DISABLE_THIRD_PARTY_SHARING,
    SUBSCRIPTION,
    THIRD_PARTY_SHARING,
    MEASUREMENT_CONSENT;

    companion object {
        public fun fromString(string: String): ActivityKind {
            return when (string) {
                "session" -> SESSION
                "event" -> EVENT
                "click" -> CLICK
                "attribution" -> ATTRIBUTION
                "reattribution" -> REATTRIBUTION
                "info" -> INFO
                "gdpr" -> GDPR
                "disable_third_party_sharing" -> DISABLE_THIRD_PARTY_SHARING
                "ad_revenue" -> AD_REVENUE
                "revenue" -> REVENUE
                "subscription" -> SUBSCRIPTION
                "third_party_sharing" -> THIRD_PARTY_SHARING
                "measurement_consent" -> MEASUREMENT_CONSENT
                else -> UNKNOWN
            }

        }
    }

    override fun toString(): String {
        return when (this) {
            SESSION -> "session"
            EVENT -> "event"
            CLICK -> "click"
            ATTRIBUTION -> "attribution"
            REATTRIBUTION -> "reattribution"
            INFO -> "info"
            GDPR -> "gdpr"
            DISABLE_THIRD_PARTY_SHARING -> "disable_third_party_sharing"
            AD_REVENUE -> "ad_revenue"
            REVENUE -> "revenue"
            SUBSCRIPTION -> "subscription"
            THIRD_PARTY_SHARING -> "third_party_sharing"
            MEASUREMENT_CONSENT -> "measurement_consent"
            else -> "unknown"
        }
    }
}
