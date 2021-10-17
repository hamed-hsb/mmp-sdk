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
     * Weak reference to ActivityHandler instance.
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
                val installReferrer = cursor.getString(0)
                val clickTime = cursor.getString(1)
                val installTime = cursor.getString(2)
                logger!!.debug("InstallReferrerHuawei reads referrer[$installReferrer] clickTime[$clickTime] installTime[$installTime]")
                val referrerClickTimestampSeconds = clickTime.toLong()
                val installBeginTimestampSeconds = installTime.toLong()
                val referrerDetails = ReferrerDetails(
                    installReferrer,
                    referrerClickTimestampSeconds, installBeginTimestampSeconds
                )
                referrerCallback!!.onInstallReferrerRead(referrerDetails)
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

    companion object {
        /**
         * Huawei install referrer provider content authority.
         */
        private const val REFERRER_PROVIDER_AUTHORITY = "com.huawei.appmarket.commondata"

        /**
         * Huawei install referrer provider content uri.
         */
        private const val REFERRER_PROVIDER_URI = "content://$REFERRER_PROVIDER_AUTHORITY/item/5"

    }
}
