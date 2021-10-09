package com.motrack.sdk

/**
 * @author yaya (@yahyalmh)
 * @since 06th October 2021
 */

class MotrackAdRevenue {
    private lateinit var source: String
    private var revenue: Double = 0.0
    var currency: String? = null
    private var adImpressionsCount: Int = 0
    private var adRevenueNetwork: String? = null
    private var adRevenueUnit: String? = null
    private var adRevenuePlacement: String? = null

    private var callbackParameters: HashMap<String, String>? = null
    private var partnerParameters: HashMap<String, String>? = null

    companion object {
        val logger = MotrackFactory.getLogger()
    }

    constructor(source: String?) {
        if (!isValidSource(source)) return
        this.source = source!!
    }

    public fun addCallbackParameter(key: String, value: String) {
        if (!Util.isValidParameter(key, "key", "Callback")) return
        if (!Util.isValidParameter(value, "value", "Callback")) return

        if (callbackParameters == null) {
            callbackParameters = LinkedHashMap()
        }

        val previousValue = callbackParameters!!.put(key, value)

        if (!previousValue.isNullOrEmpty()) {
            logger.warn("key $key was overwritten")
        }
    }

    fun addPartnerParameter(key: String?, value: String?) {
        if (!Util.isValidParameter(key, "key", "Partner")) return
        if (!Util.isValidParameter(value, "value", "Partner")) return

        if (partnerParameters == null) {
            partnerParameters = java.util.LinkedHashMap()
        }

        val previousValue = partnerParameters!!.put(key!!, value!!)

        if (previousValue != null) {
            logger.warn("key $key was overwritten")
        }
    }

    public fun isValid(): Boolean {
        return isValidSource(this.source)
    }

    private fun isValidSource(param: String?): Boolean {
        if (param == null) {
            logger.error("Missing source")
            return false
        }

        if (param.isEmpty()) {
            logger.error("Source can not be null")
            return false
        }
        return true
    }

    fun setRevenue(revenue: Double, currency: String) {
        this.revenue = revenue
        this.currency = currency
    }

    fun setAdImpressionsCount(adImpressionsCount: Int) {
        this.adImpressionsCount = adImpressionsCount
    }

    fun setAdRevenueNetwork(adRevenueNetwork: String) {
        this.adRevenueNetwork = adRevenueNetwork
    }

    fun setAdRevenueUnit(adRevenueUnit: String) {
        this.adRevenueUnit = adRevenueUnit
    }

    fun setAdRevenuePlacement(adRevenuePlacement: String) {
        this.adRevenuePlacement = adRevenuePlacement
    }
}