package com.motrack.sdk.criteo

import android.net.Uri
import com.motrack.sdk.ILogger
import com.motrack.sdk.MotrackEvent
import com.motrack.sdk.MotrackFactory
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 07th December 2021
 */

class MotrackCriteo {
    private val logger: ILogger = MotrackFactory.getLogger()
    private val MAX_VIEW_LISTING_PRODUCTS = 3
    private var hashEmailInternal: String? = null
    private var checkInDateInternal: String? = null
    private var checkOutDateInternal: String? = null
    private var partnerIdInternal: String? = null
    private var userSegmentInternal: String? = null
    private var customerIdInternal: String? = null

    fun injectViewListingIntoEvent(event: MotrackEvent, productIds: List<String>) {
        val jsonProducts: String? = createCriteoVLFromProducts(productIds)
        event.addPartnerParameter("criteo_p", jsonProducts)
        injectOptionalParams(event)
    }

    fun injectViewProductIntoEvent(event: MotrackEvent, productId: String?) {
        event.addPartnerParameter("criteo_p", productId)
        injectOptionalParams(event)
    }

    fun injectCartIntoEvent(event: MotrackEvent, products: List<CriteoProduct>) {
        val jsonProducts: String? = createCriteoVBFromProducts(products)
        event.addPartnerParameter("criteo_p", jsonProducts)
        injectOptionalParams(event)
    }

    fun injectTransactionConfirmedIntoEvent(
        event: MotrackEvent,
        products: List<CriteoProduct>,
        transactionId: String?,
        newCustomer: String?
    ) {
        val jsonProducts: String? = createCriteoVBFromProducts(products)
        event.addPartnerParameter("transaction_id", transactionId)
        event.addPartnerParameter("criteo_p", jsonProducts)
        event.addPartnerParameter("new_customer", newCustomer)
        injectOptionalParams(event)
    }

    fun injectUserLevelIntoEvent(event: MotrackEvent, uiLevel: Long) {
        event.addPartnerParameter("ui_level", uiLevel.toString())
        injectOptionalParams(event)
    }

    fun injectUserStatusIntoEvent(event: MotrackEvent, uiStatus: String?) {
        event.addPartnerParameter("ui_status", uiStatus)
        injectOptionalParams(event)
    }

    fun injectAchievementUnlockedIntoEvent(event: MotrackEvent, uiAchievement: String?) {
        event.addPartnerParameter("ui_achievmnt", uiAchievement)
        injectOptionalParams(event)
    }

    fun injectCustomEventIntoEvent(event: MotrackEvent, uiData: String?) {
        event.addPartnerParameter("ui_data", uiData)
        injectOptionalParams(event)
    }

    fun injectCustomEvent2IntoEvent(event: MotrackEvent, uiData2: String?, uiData3: Long) {
        event.addPartnerParameter("ui_data2", uiData2)
        event.addPartnerParameter("ui_data3", uiData3.toString())
        injectOptionalParams(event)
    }

    fun injectHashedEmailIntoCriteoEvents(hashEmail: String) {
        hashEmailInternal = hashEmail
    }

    fun injectViewSearchDatesIntoCriteoEvents(checkInDate: String, checkOutDate: String) {
        checkInDateInternal = checkInDate
        checkOutDateInternal = checkOutDate
    }

    fun injectPartnerIdIntoCriteoEvents(partnerId: String) {
        partnerIdInternal = partnerId
    }

    fun injectUserSegmentIntoCriteoEvents(userSegment: String) {
        userSegmentInternal = userSegment
    }

    fun injectCustomerIdIntoCriteoEvents(customerId: String) {
        customerIdInternal = customerId
    }

    fun injectDeeplinkIntoEvent(event: MotrackEvent, url: Uri?) {
        if (url == null) {
            return
        }
        event.addPartnerParameter("criteo_deeplink", url.toString())
        injectOptionalParams(event)
    }

    private fun injectOptionalParams(event: MotrackEvent) {
        injectHashEmail(event)
        injectSearchDates(event)
        injectPartnerId(event)
        injectUserSegment(event)
        injectCustomerId(event)
    }

    private fun injectHashEmail(event: MotrackEvent) {
        if (hashEmailInternal == null || hashEmailInternal.isNullOrEmpty()) {
            return
        }
        event.addPartnerParameter(
            "criteo_email_hash",
            hashEmailInternal
        )
    }

    private fun injectSearchDates(event: MotrackEvent) {
        if (checkInDateInternal == null || checkInDateInternal.isNullOrEmpty() || checkOutDateInternal == null || checkOutDateInternal.isNullOrEmpty()) {
            return
        }
        event.addPartnerParameter("din", checkInDateInternal)
        event.addPartnerParameter("dout", checkOutDateInternal)
    }

    private fun injectPartnerId(event: MotrackEvent) {
        if (partnerIdInternal == null || partnerIdInternal.isNullOrEmpty()) {
            return
        }
        event.addPartnerParameter(
            "criteo_partner_id",
            partnerIdInternal
        )
    }

    private fun injectUserSegment(event: MotrackEvent) {
        if (userSegmentInternal == null || userSegmentInternal.isNullOrEmpty()) {
            return
        }
        event.addPartnerParameter(
            "user_segment",
            userSegmentInternal
        )
    }

    private fun injectCustomerId(event: MotrackEvent) {
        if (customerIdInternal == null || customerIdInternal.isNullOrEmpty()) {
            return
        }
        event.addPartnerParameter(
            "customer_id",
            customerIdInternal
        )
    }

    private fun createCriteoVLFromProducts(productIds: List<String>): String? {
        var productIds: List<String>? = productIds
        if (productIds == null) {
            logger.warn("Criteo View Listing product ids list is null. It will sent as empty.")
            productIds = ArrayList()
        }
        val criteoVLValue = StringBuffer("[")
        val productIdsSize = productIds.size
        if (productIdsSize > MAX_VIEW_LISTING_PRODUCTS) {
            logger.warn("Criteo View Listing should only have at most 3 product ids. The rest will be discarded.")
        }
        var i = 0
        while (i < productIdsSize) {
            val productID = productIds[i]
            val productString = String.format(Locale.US, "\"%s\"", productID)
            criteoVLValue.append(productString)
            i++
            if (i == productIdsSize || i >= MAX_VIEW_LISTING_PRODUCTS) {
                break
            }
            criteoVLValue.append(",")
        }
        criteoVLValue.append("]")
        var result: String? = null
        try {
            result = URLEncoder.encode(criteoVLValue.toString(), "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            e.message?.let {
                logger.error(
                    "error converting criteo product ids (%s)",
                    it
                )
            }
        }
        return result
    }

    private fun createCriteoVBFromProducts(products: List<CriteoProduct>): String? {
        var products: List<CriteoProduct>? = products
        if (products == null) {
            logger.warn("Criteo Event product list is empty. It will sent as empty.")
            products = ArrayList()
        }
        val criteoVBValue = StringBuffer("[")
        val productsSize = products.size
        var i = 0
        while (i < productsSize) {
            val criteoProduct = products[i]
            val productString = java.lang.String.format(
                Locale.US, "{\"i\":\"%s\",\"pr\":%f,\"q\":%d}",
                criteoProduct.productID,
                criteoProduct.price,
                criteoProduct.quantity
            )
            criteoVBValue.append(productString)
            i++
            if (i == productsSize) {
                break
            }
            criteoVBValue.append(",")
        }
        criteoVBValue.append("]")
        var result: String? = null
        try {
            result = URLEncoder.encode(criteoVBValue.toString(), "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            e.message?.let {
                logger.error("error converting criteo products $it")
            }
        }
        return result
    }
}