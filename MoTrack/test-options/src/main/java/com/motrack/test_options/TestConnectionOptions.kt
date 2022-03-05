package com.motrack.test_options

import android.util.Log
import com.motrack.sdk.MotrackFactory
import com.motrack.sdk.network.NetworkUtil
import java.net.HttpURLConnection
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * @author yaya (@yahyalmh)
 * @since 21th November 2021
 */

class TestConnectionOptions {
    companion object {

        fun setTestConnectionOptions() {
            MotrackFactory.connectionOptions = object : NetworkUtil.IConnectionOptions {
                override fun applyConnectionOptions(
                    connection: HttpURLConnection,
                    clientSdk: String
                ) {
                    val defaultConnectionOption: NetworkUtil.IConnectionOptions =
                        NetworkUtil.createDefaultConnectionOptions()
                    defaultConnectionOption.applyConnectionOptions(connection, clientSdk)
                    try {
                        val tlsSocketFactory = TLSSocketFactory(
                            generateTrustAllCerts(),
                            SecureRandom()
                        )
//                        connection.sslSocketFactory = tlsSocketFactory
//                        connection.hostnameVerifier = generateHostnameVerifier()
                    } catch (e: Exception) {
                        Log.e("TestOptions", "connectionOptions error " + e.message)
                    }
                }
            }
        }

        private fun generateTrustAllCerts(): Array<TrustManager> {
            return arrayOf(
                object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate>? {
                        Log.d("TestApp", "getAcceptedIssuers")
                        return null
                    }

                    override fun checkClientTrusted(
                        certs: Array<X509Certificate>,
                        authType: String
                    ) {
                        Log.d("TestApp", "checkClientTrusted")
                    }

                    override fun checkServerTrusted(
                        certs: Array<X509Certificate>,
                        authType: String
                    ) {
                        Log.d("TestApp", "checkServerTrusted")
                    }
                }
            )
        }

        private fun generateHostnameVerifier(): HostnameVerifier {
            return HostnameVerifier { _, _ ->
                Log.d("TestApp", "HostnameVerifier verify")
                true
            }
        }
    }
}