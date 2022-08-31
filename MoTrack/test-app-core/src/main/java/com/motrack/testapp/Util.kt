package com.motrack.testapp

import android.util.Log
import com.motrack.sdk.network.NetworkUtil
import java.net.HttpURLConnection
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.*

/**
 * @author yaya (@yahyalmh)
 * @since 21th November 2021
 */

class Util {
    companion object {

        fun strictParseStringToBoolean(value: String?): Boolean? {
            if (value == null) {
                return null
            }
            if (value.equals("true", ignoreCase = true)) {
                return true
            }
            return if (value.equals("false", ignoreCase = true)) {
                false
            } else null
        }

        fun testConnectionOptions(): NetworkUtil.IConnectionOptions {
            return object : NetworkUtil.IConnectionOptions {
                override fun applyConnectionOptions(
                    connection: HttpURLConnection,
                    clientSdk: String
                ) {
                    val defaultConnectionOption: NetworkUtil.IConnectionOptions =
                        NetworkUtil.createDefaultConnectionOptions()
                    defaultConnectionOption.applyConnectionOptions(connection, clientSdk)
                    try {
                        val sc = SSLContext.getInstance("TLS")
                        sc.init(null, arrayOf<TrustManager>(
                            object : X509TrustManager {
                                override fun getAcceptedIssuers(): Array<X509Certificate>? {
                                    Log.d("TestApp", "getAcceptedIssuers")
                                    return null
                                }

                                override fun checkClientTrusted(
                                    certs: Array<X509Certificate>, authType: String
                                ) {
                                    Log.d("TestApp", "checkClientTrusted ")
                                }

                                @Throws(CertificateException::class)
                                override fun checkServerTrusted(
                                    certs: Array<X509Certificate>, authType: String
                                ) {
                                    Log.d("TestApp", "checkServerTrusted ")
                                    val serverThumbprint =
                                        "7BCFF44099A35BC093BB48C5A6B9A516CDFDA0D1"
                                    val certificate = certs[0]
                                    var md: MessageDigest? = null
                                    try {
                                        md = MessageDigest.getInstance("SHA1")
                                        val publicKey = md.digest(certificate.encoded)
                                        val hexString = byte2HexFormatted(publicKey)
                                        if (!hexString.equals(
                                                serverThumbprint,
                                                ignoreCase = true
                                            )
                                        ) {
                                            throw CertificateException()
                                        }
                                    } catch (e: NoSuchAlgorithmException) {
                                        Log.e("TestApp", "testingMode error " + e.message)
                                    } catch (e: CertificateEncodingException) {
                                        Log.e("TestApp", "testingMode error " + e.message)
                                    }
                                }
                            }
                        ), SecureRandom())
//                        connection.sslSocketFactory = sc.socketFactory
//                        connection.hostnameVerifier = HostnameVerifier { hostname, session ->
//                            Log.d("TestApp", "verify hostname ")
//                            true
//                        }
                    } catch (e: Exception) {
                        Log.e("TestApp", "testingMode error " + e.message)
                    }
                }
            }
        }

        private fun byte2HexFormatted(arr: ByteArray): String {
            val str = StringBuilder(arr.size * 2)
            for (i in arr.indices) {
                var h = Integer.toHexString(arr[i].toInt())
                val l = h.length
                if (l == 1) {
                    h = "0$h"
                }
                if (l > 2) {
                    h = h.substring(l - 2, l)
                }
                str.append(h.uppercase(Locale.getDefault()))

                // if (i < (arr.length - 1)) str.append(':');
            }
            return str.toString()
        }
    }
}