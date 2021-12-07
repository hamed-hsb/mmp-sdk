package com.motrack.testapp

import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object OkHttpProvider {

    // Timeout for the network requests
    private const val REQUEST_TIMEOUT = 3L

    private var okHttpClient: OkHttpClient? = null


    fun getOkHttpClient(): OkHttpClient {
        return if (okHttpClient == null) {
            val okHttpClient = OkHttpClient.Builder()
                .readTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
                .build()
            this.okHttpClient = okHttpClient
            okHttpClient
        } else {
            okHttpClient!!
        }
    }
}