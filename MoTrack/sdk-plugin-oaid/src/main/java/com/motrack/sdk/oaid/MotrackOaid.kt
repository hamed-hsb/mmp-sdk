package com.motrack.sdk.oaid

import android.content.Context
import android.util.Log
import com.bun.miitmdid.core.MdidSdkHelper

/**
 * @author yaya (@yahyalmh)
 * @since 12th December 2021
 */

class MotrackOaid {
    companion object {
        var isOaidToBeRead = false
        var isMsaSdkAvailable = false
    }

    fun readOaid() {
        isOaidToBeRead = true
    }

    fun readOaid(context: Context) {
        readOaid()
        try {
            System.loadLibrary("nllvm1623827671")
            val certificate: String? = Util.readCertFromAssetFile(context)
            isMsaSdkAvailable = MdidSdkHelper.InitCert(context, certificate)
        } catch (t: Throwable) {
            isMsaSdkAvailable = false
            Log.d("Adjust", "Error during msa sdk initialization " + t.message)
        }
    }

    fun doNotReadOaid() {
        isOaidToBeRead = false
    }
}