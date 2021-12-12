package com.motrack.sdk

import android.content.Context
import android.database.Cursor
import android.net.Uri
import java.util.concurrent.atomic.AtomicBoolean

class InstallReferrerHuawei
/**
 * Default constructor.
 *
 * @param context         Application context
 * @param referrerCallback Callback for referrer information
 */(
    /**
     * Application context.
     */
    private var context: Context?,
    /**
     * Huawei Referrer callback.
     */
    private val referrerCallback: InstallReferrerReadListener?
) {

    private var logger: ILogger? = null

    /**
     * Boolean indicating whether service should be tried to read.
     * Either because it has not yet tried,
     * or it did and it was successful
     * or it did, was not successful, but it should not retry
     */
    private var shouldTryToRead: AtomicBoolean? = null

    init {
        logger = MotrackFactory.getLogger()
        shouldTryToRead = AtomicBoolean(true)
    }

    fun readReferrer() {
        if (!shouldTryToRead!!.get()) {
            logger!!.debug("Should not try to read Install referrer Huawei")
            return
        }
        if (!Util.resolveContentProvider(context!!, REFERRER_PROVIDER_AUTHORITY)) {
            return
        }
        var cursor: Cursor? = null
        val uri = Uri.parse(REFERRER_PROVIDER_URI)
        val contentResolver = context!!.contentResolver


        val packageName = arrayOf(context!!.packageName)
        try {
            cursor = contentResolver.query(uri, null, null, packageName, null)

            if (cursor != null && cursor.moveToFirst()) {
                val referrerHuaweiAds = cursor.getString(COLUMN_INDEX_REFERRER)
                val referrerHuaweiAppGallery = cursor.getString(COLUMN_INDEX_TRACK_ID)
                logger!!.debug("InstallReferrerHuawei reads index_referrer $referrerHuaweiAds index_track_id $referrerHuaweiAppGallery")
                val clickTime = cursor.getString(COLUMN_INDEX_CLICK_TIME)
                val installTime = cursor.getString(COLUMN_INDEX_INSTALL_TIME)
                logger!!.debug("InstallReferrerHuawei reads clickTime[$clickTime] installTime[$installTime]")

                val referrerClickTimestampSeconds = clickTime.toLong()
                val installBeginTimestampSeconds = installTime.toLong()
                if (isValidReferrerHuaweiAds(referrerHuaweiAds)) {
                    val referrerDetails = ReferrerDetails(
                        referrerHuaweiAds,
                        referrerClickTimestampSeconds,
                        installBeginTimestampSeconds
                    )
                    referrerCallback!!.onInstallReferrerRead(
                        referrerDetails,
                        Constants.REFERRER_API_HUAWEI_ADS
                    )
                }
                if (isValidReferrerHuaweiAppGallery(referrerHuaweiAppGallery)) {
                    val referrerDetails = ReferrerDetails(
                        referrerHuaweiAppGallery,
                        referrerClickTimestampSeconds,
                        installBeginTimestampSeconds
                    )
                    referrerCallback!!.onInstallReferrerRead(
                        referrerDetails,
                        Constants.REFERRER_API_HUAWEI_APP_GALLERY
                    )
                }
            } else {
                logger!!.debug("InstallReferrerHuawei fail to read referrer for package [${context!!.packageName}] and content uri [$uri]")
            }
        } catch (e: Exception) {
            logger!!.debug("InstallReferrerHuawei error [${e.message!!}]")
        } finally {
            cursor?.close()
        }
        shouldTryToRead!!.set(false)
    }

    private fun isValidReferrerHuaweiAds(referrerHuaweiAds: String?): Boolean {
        if (referrerHuaweiAds == null) {
            return false
        }
        return referrerHuaweiAds.isNotEmpty()
    }

    private fun isValidReferrerHuaweiAppGallery(referrerHuaweiAppGallery: String?): Boolean {
        if (referrerHuaweiAppGallery == null) {
            return false
        }
        return referrerHuaweiAppGallery.isNotEmpty()
    }


    companion object {
        /**
         * Huawei install referrer provider content authority.
         */
        private const val REFERRER_PROVIDER_AUTHORITY = "com.huawei.appmarket.commondata"

        /**
         * Huawei install referrer provider content uri.
         */
        private const val REFERRER_PROVIDER_URI = "content://$REFERRER_PROVIDER_AUTHORITY/item/5"


        /**
         * Huawei install referrer provider column index referrer.
         */
        private const val COLUMN_INDEX_REFERRER = 0

        /**
         * Huawei install referrer provider column index click time.
         */
        private const val COLUMN_INDEX_CLICK_TIME = 1

        /**
         * Huawei install referrer provider column index install time.
         */
        private const val COLUMN_INDEX_INSTALL_TIME = 2

        /**
         * Huawei install referrer provider column index track ID.
         */
        private const val COLUMN_INDEX_TRACK_ID = 4

    }
}
