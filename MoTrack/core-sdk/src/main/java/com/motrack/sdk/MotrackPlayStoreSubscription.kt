package com.motrack.sdk

/**
 * @author yaya (@yahyalmh)
 * @since 06th October 2021
 */

class MotrackPlayStoreSubscription(
    private val price: Long,                                // revenue
    private val currency: String,
    private val sku: String,                                // product_id
    private val orderId: String,                            // transaction_id
    private val signature: String,                          // receipt
    private val purchaseToken: String,
    private val billingStore: String = "GooglePlay",
    private var purchaseTime: Long = -1                     // transaction_date
) {
    private var callbackParameters: HashMap<String, String>? = null
    private var partnerParameters: HashMap<String, String>? = null

    public fun addCallbackParameter(key: String, value: String) {
        if (!Util.isValidParameter(key, "key", "Callback")) return
        if (!Util.isValidParameter(value, "value", "Callback")) return

        if (callbackParameters == null) {
            callbackParameters = LinkedHashMap()
        }

        val previousValue = callbackParameters!!.put(key, value)

        if (!previousValue.isNullOrEmpty()) {
            MotrackAdRevenue.logger.warn("key $key was overwritten")
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
            MotrackAdRevenue.logger.warn("key $key was overwritten")
        }
    }

    fun getPrice(): Long {
        return price
    }

    fun getPurchaseTime(): Long {
        return purchaseTime
    }

    fun getCurrency(): String {
        return currency
    }

    fun getSku(): String {
        return sku
    }

    fun getOrderId(): String {
        return orderId
    }

    fun getSignature(): String {
        return signature
    }

    fun getBillingStore(): String {
        return billingStore
    }

    fun getPurchaseToken(): String {
        return purchaseToken
    }

    fun getCallbackParameters(): HashMap<String, String>? {
        return callbackParameters
    }

    fun getPartnerParameters(): HashMap<String, String>? {
        return partnerParameters
    }

    fun setPurchaseTime(purchaseTime: Long) {
        this.purchaseTime = purchaseTime
    }
}