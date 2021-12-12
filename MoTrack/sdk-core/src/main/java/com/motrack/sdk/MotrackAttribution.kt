package com.motrack.sdk

import org.json.JSONObject
import java.io.*

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

class MotrackAttribution : Serializable {

    lateinit var trackerToken: String
    lateinit var trackerName: String
    lateinit var network: String
    lateinit var campaign: String
    lateinit var adgroup: String
    lateinit var creative: String
    lateinit var clickLabel: String
    lateinit var adid: String
    lateinit var costType: String
    var costAmount: Double = 0.0
    lateinit var costCurrency: String


    @Throws(IOException::class)
    private fun writeObject(stream: ObjectOutputStream) {
        stream.defaultWriteObject()
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(stream: ObjectInputStream) {
        stream.defaultReadObject()
    }

    companion object {
        const val serialVersionUID: Long = 1L
        private val serialPersistentFields = arrayOf(
            ObjectStreamField("trackerToken", String::class.java),
            ObjectStreamField("trackerName", String::class.java),
            ObjectStreamField("network", String::class.java),
            ObjectStreamField("campaign", String::class.java),
            ObjectStreamField("adgroup", String::class.java),
            ObjectStreamField("creative", String::class.java),
            ObjectStreamField("clickLabel", String::class.java),
            ObjectStreamField("adid", String::class.java),
            ObjectStreamField("costType", String::class.java),
            ObjectStreamField("costAmount", Double::class.java),
            ObjectStreamField("costCurrency", String::class.java)
        )

        public fun fromJson(
            jsonObject: JSONObject?,
            adid: String?,
            sdkPlatform: String?
        ): MotrackAttribution? {
            if (jsonObject == null) return null
            val attribution = MotrackAttribution()
            if ("unity" == sdkPlatform) {
                // Unity Platform
                attribution.trackerToken = jsonObject.optString("tracker_token", "")
                attribution.trackerName = jsonObject.optString("tracker_name", "")
                attribution.network = jsonObject.optString("network", "")
                attribution.campaign = jsonObject.optString("campaign", "")
                attribution.adgroup = jsonObject.optString("adgroup", "")
                attribution.creative = jsonObject.optString("creative", "")
                attribution.clickLabel = jsonObject.optString("click_label", "")
                attribution.adid = adid ?: ""
                attribution.costType = jsonObject.optString("cost_type", "")
                attribution.costAmount = jsonObject.optDouble("cost_amount", 0.0)
                attribution.costCurrency = jsonObject.optString("cost_currency", "")
            } else {
                // Rest of all platforms.
                attribution.trackerToken = jsonObject.optString("tracker_token")
                attribution.trackerName = jsonObject.optString("tracker_name")
                attribution.network = jsonObject.optString("network")
                attribution.campaign = jsonObject.optString("campaign")
                attribution.adgroup = jsonObject.optString("adgroup")
                attribution.creative = jsonObject.optString("creative")
                attribution.clickLabel = jsonObject.optString("click_label")
                attribution.adid = adid!!
                attribution.costType = jsonObject.optString("cost_type")
                attribution.costAmount = jsonObject.optDouble("cost_amount")
                attribution.costCurrency = jsonObject.optString("cost_currency")
            }
            return attribution
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MotrackAttribution

        if (trackerToken != other.trackerToken) return false
        if (trackerName != other.trackerName) return false
        if (network != other.network) return false
        if (campaign != other.campaign) return false
        if (adgroup != other.adgroup) return false
        if (creative != other.creative) return false
        if (clickLabel != other.clickLabel) return false
        if (adid != other.adid) return false
        if (costType != other.costType) return false
        if (costAmount != other.costAmount) return false
        if (costCurrency != other.costCurrency) return false

        return true
    }

    override fun hashCode(): Int {
        var result = trackerToken.hashCode()
        result = 31 * result + trackerName.hashCode()
        result = 31 * result + network.hashCode()
        result = 31 * result + campaign.hashCode()
        result = 31 * result + adgroup.hashCode()
        result = 31 * result + creative.hashCode()
        result = 31 * result + clickLabel.hashCode()
        result = 31 * result + adid.hashCode()
        result = 31 * result + costType.hashCode()
        result = 31 * result + costAmount.hashCode()
        result = 31 * result + costCurrency.hashCode()
        return result
    }

    override fun toString(): String {
        return "MotrackAttribution(trackerToken='$trackerToken', trackerName='$trackerName', network='$network', campaign='$campaign', adgroup='$adgroup', creative='$creative', clickLabel='$clickLabel', adid='$adid', costType='$costType', costMount=$costAmount, costCurrency='$costCurrency')"
    }
}