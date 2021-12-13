package com.motrack.sdk.network

import com.motrack.sdk.ActivityKind
import com.motrack.sdk.Constants
import com.motrack.sdk.MotrackConfig.Companion.DATA_RESIDENCY_EU
import com.motrack.sdk.MotrackConfig.Companion.DATA_RESIDENCY_TR
import com.motrack.sdk.MotrackConfig.Companion.DATA_RESIDENCY_US
import com.motrack.sdk.MotrackConfig.Companion.URL_STRATEGY_CHINA
import com.motrack.sdk.MotrackConfig.Companion.URL_STRATEGY_INDIA

/**
 * @author yaya (@yahyalmh)
 * @since 11th October 2021
 */

class UrlStrategy(
    private val baseUrlOverwrite: String?,
    private val gdprUrlOverwrite: String?,
    private val subscriptionUrlOverwrite: String?,
    motrackUrlStrategy: String?
) {
    companion object {
        private const val BASE_URL_INDIA = "https://app.motrack.net.in"
        private const val GDPR_URL_INDIA = "https://gdpr.motrack.net.in"
        private const val SUBSCRIPTION_URL_INDIA = "https://subscription.motrack.net.in"

        private const val BASE_URL_CHINA = "https://app.motrack.world"
        private const val GDPR_URL_CHINA = "https://gdpr.motrack.world"
        private const val SUBSCRIPTION_URL_CHINA = "https://subscription.motrack.world"

        private const val BASE_URL_EU = "https://app.eu.motrack.com"
        private const val GDPR_URL_EU = "https://gdpr.eu.motrack.com"
        private const val SUBSCRIPTION_URL_EU = "https://subscription.eu.motrack.com"

        private const val BASE_URL_TR = "https://app.tr.motrack.com"
        private const val GDPR_URL_TR = "https://gdpr.tr.motrack.com"
        private const val SUBSCRIPTION_URL_TR = "https://subscription.tr.motrack.com"

        private const val BASE_URL_US = "https://app.us.motrack.com"
        private const val GDPR_URL_US = "https://gdpr.us.motrack.com"
        private const val SUBSCRIPTION_URL_US = "https://subscription.us.motrack.com"

        private fun baseUrlChoices(urlStrategy: String?): List<String> {
            return when (urlStrategy) {
                URL_STRATEGY_INDIA -> listOf(BASE_URL_INDIA, Constants.BASE_URL)
                URL_STRATEGY_CHINA -> listOf(BASE_URL_CHINA, Constants.BASE_URL)
                DATA_RESIDENCY_EU -> listOf(BASE_URL_EU)
                DATA_RESIDENCY_TR -> listOf(BASE_URL_TR)
                DATA_RESIDENCY_US -> listOf(BASE_URL_US)
                else -> listOf(Constants.BASE_URL, BASE_URL_INDIA, BASE_URL_CHINA)
            }
        }

        private fun gdprUrlChoices(urlStrategy: String?): List<String> {
            return when (urlStrategy) {
                URL_STRATEGY_INDIA -> listOf(GDPR_URL_INDIA, Constants.GDPR_URL)
                URL_STRATEGY_CHINA -> listOf(GDPR_URL_CHINA, Constants.GDPR_URL)
                DATA_RESIDENCY_EU -> listOf(GDPR_URL_EU)
                DATA_RESIDENCY_TR -> listOf(GDPR_URL_TR)
                DATA_RESIDENCY_US -> listOf(GDPR_URL_US)
                else -> listOf(Constants.GDPR_URL, GDPR_URL_INDIA, GDPR_URL_CHINA)
            }
        }

        private fun subscriptionUrlChoices(urlStrategy: String?): List<String> {
            return when (urlStrategy) {
                URL_STRATEGY_INDIA -> listOf(SUBSCRIPTION_URL_INDIA, Constants.SUBSCRIPTION_URL)
                URL_STRATEGY_CHINA -> listOf(SUBSCRIPTION_URL_CHINA, Constants.SUBSCRIPTION_URL)
                DATA_RESIDENCY_EU -> listOf(SUBSCRIPTION_URL_EU)
                DATA_RESIDENCY_TR -> listOf(SUBSCRIPTION_URL_TR)
                DATA_RESIDENCY_US -> listOf(SUBSCRIPTION_URL_US)
                else -> listOf(
                    Constants.SUBSCRIPTION_URL,
                    SUBSCRIPTION_URL_INDIA,
                    SUBSCRIPTION_URL_CHINA
                )
            }
        }
    }

    private var baseUrlChoicesList = baseUrlChoices(motrackUrlStrategy)
    private var gdprUrlChoicesList = gdprUrlChoices(motrackUrlStrategy)
    private var subscriptionUrlChoicesList = subscriptionUrlChoices(motrackUrlStrategy)
    var wasLastAttemptSuccess = false
    private var choiceIndex = 0
    private var startingChoiceIndex = 0
    private var wasLastAttemptWithOverwrittenUrl = false

    fun resetAfterSuccess() {
        startingChoiceIndex = choiceIndex
        wasLastAttemptSuccess = true
    }

    fun shouldRetryAfterFailure(activityKind: ActivityKind): Boolean {
        wasLastAttemptSuccess = false

        // does not need to "rotate" choice index
        //  since it will use the same overwritten url
        //  might as well stop retrying in the same sending "session"
        //  and let the backoff strategy pick it up
        if (wasLastAttemptWithOverwrittenUrl) {
            return false
        }
        val choiceListSize: Int = when (activityKind) {
            ActivityKind.GDPR -> gdprUrlChoicesList.size
            ActivityKind.SUBSCRIPTION -> subscriptionUrlChoicesList.size
            else -> baseUrlChoicesList.size
        }

        val nextChoiceIndex = (choiceIndex + 1) % choiceListSize
        choiceIndex = nextChoiceIndex
        return choiceIndex != startingChoiceIndex
    }

    fun targetUrlByActivityKind(activityKind: ActivityKind): String {
        return if (activityKind === ActivityKind.GDPR) {
            if (gdprUrlOverwrite != null) {
                wasLastAttemptWithOverwrittenUrl = true
                gdprUrlOverwrite
            } else {
                wasLastAttemptWithOverwrittenUrl = false
                gdprUrlChoicesList[choiceIndex]
            }
        } else if (activityKind === ActivityKind.SUBSCRIPTION) {
            if (subscriptionUrlOverwrite != null) {
                wasLastAttemptWithOverwrittenUrl = true
                subscriptionUrlOverwrite
            } else {
                wasLastAttemptWithOverwrittenUrl = false
                subscriptionUrlChoicesList[choiceIndex]
            }
        } else {
            if (baseUrlOverwrite != null) {
                wasLastAttemptWithOverwrittenUrl = true
                baseUrlOverwrite
            } else {
                wasLastAttemptWithOverwrittenUrl = false
                baseUrlChoicesList[choiceIndex]
            }
        }
    }

}