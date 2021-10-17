package com.motrack.sdk

/**
 * @author yaya (@yahyalmh)
 * @since 04th October 2021
 */

class MotrackEvent {
    lateinit var eventToken: String
    var revenue: Double? = null
    var currency: String? = null
    var callbackParameters: LinkedHashMap<String, String>? = null
    var partnerParameters: LinkedHashMap<String, String>? = null
    var orderId: String? = null
    var callbackId: String? = null
    private val logger: ILogger = MotrackFactory.getLogger()


    constructor(eventToken: String) {
        if (!checkEventToken(eventToken)) return
        this.eventToken = eventToken
    }

    public fun setRevenue(revenue: Double, currency: String) {
        if (!checkRevenue(revenue, currency)) return

        this.revenue = revenue
        this.currency = currency
    }

    public fun addCallbackParameter(key: String?, value: String?) {
        if (!Util.isValidParameter(key, "key", "Callback")) return
        if (!Util.isValidParameter(value, "value", "Callback")) return

        if (callbackParameters == null) {
            callbackParameters = LinkedHashMap()
        }

        val previousValue: String? = callbackParameters!!.put(key!!, value!!)

        if (previousValue != null) {
            logger.warn("$key was overwritten")
        }
    }

    public fun addPartnerParameter(key: String?, value: String?) {
        if (!Util.isValidParameter(key, "key", "Partner")) return
        if (!Util.isValidParameter(value, "value", "Partner")) return

        if (partnerParameters == null) {
            partnerParameters = LinkedHashMap()
        }

        val previousValue = partnerParameters!!.put(key!!, value!!)
        if (previousValue != null) {
            logger.warn("$key was overwritten")
        }
    }

    @JvmName("setEventOrderId")
    public fun setOrderId(orderId: String) {
        this.orderId = orderId
    }

    @JvmName("setEventCallbackId")
    public fun setCallbackId(callbackId: String) {
        this.callbackId = callbackId
    }

    private fun checkRevenue(revenue: Double?, currency: String?): Boolean {
        if (revenue != null) {
            if (revenue < 0.0) {
                logger.error("Invalid revenue amount: $revenue")
                return false
            }

            if (currency == null) {
                logger.error("Currency must set with revenue")
                return false
            }

            if (currency.isEmpty() || currency.isBlank()) {
                logger.error("Currency is empty")
                return false
            }
        } else if (currency != null) {
            logger.error("Revenue must be set with currency")
            return false
        }

        return true
    }

    private fun checkEventToken(eventToken: String?): Boolean {
        if (eventToken == null) {
            logger.error("Missing Event Token")
            return false
        }
        if (eventToken.isBlank() || eventToken.isEmpty()) {
            logger.error("Event Token can not be empty")
            return false
        }
        return true
    }

    fun isValid(): Boolean {
        return eventToken != null
    }
}