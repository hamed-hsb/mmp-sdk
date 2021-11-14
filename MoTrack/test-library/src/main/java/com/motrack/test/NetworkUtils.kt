package com.motrack.test

import com.motrack.test.Util.Companion.debug
import com.motrack.test.Util.Companion.error
import java.io.*
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL
import java.net.URLEncoder
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.*
import kotlin.properties.Delegates

/**
 * @author yaya (@yahyalmh)
 * @since 14th November 2021
 */

class NetworkUtils {
    companion object {
        var connectionOptions = ConnectionOptions()
        var hostnameVerifier = HostnameVerifier { _, _ -> true }

        var trustAllCerts: Array<TrustManager> = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate>? {
                    debug("getAcceptedIssuers")
                    return null
                }

                override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {
                    debug("checkClientTrusted")
                }

                override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {
                    debug("checkServerTrusted")
                }
            }
        )

        fun sendPostI(path: String): HttpResponse? {
            return sendPostI(path, null, null, null)
        }

        fun sendPostI(
            path: String,
            clientSdk: String?
        ): HttpResponse? {
            return sendPostI(path, clientSdk, null, null)
        }

        fun sendPostI(
            path: String,
            clientSdk: String?,
            testNames: String?
        ): HttpResponse? {
            return sendPostI(path, clientSdk, testNames, null)
        }

        fun sendPostI(
            path: String,
            clientSdk: String?,
            postBody: Map<String, String>?
        ): HttpResponse? {
            return sendPostI(path, clientSdk, null, postBody)
        }

        fun sendPostI(
            path: String,
            clientSdk: String?,
            testNames: String?,
            postBody: Map<String, String>?
        ): HttpResponse? {
            val targetURL: String = TestLibrary.baseUrl.toString() + path
            try {
                connectionOptions.clientSdk = clientSdk
                connectionOptions.testNames = testNames

                val connection: HttpsURLConnection =
                    createPOSTHttpsURLConnection(targetURL, postBody, connectionOptions)
                val httpResponse: HttpResponse = readHttpResponse(connection)
                debug("Response: %s", httpResponse.response)
                httpResponse.headerFields = connection.headerFields
                debug("Headers: %s", httpResponse.headerFields)
                return httpResponse

            } catch (e: IOException) {
                e.message?.let { error(it) }
            } catch (e: Exception) {
                e.message?.let { error(it) }
            }

            return null
        }

        @Throws(java.lang.Exception::class)
        fun readHttpResponse(connection: HttpsURLConnection): HttpResponse {
            val sb = StringBuffer()
            val httpResponse = HttpResponse()
            try {
                connection.connect()
                httpResponse.responseCode = connection.responseCode
                val inputStream: InputStream = if (httpResponse.responseCode >= 400) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
            } catch (e: java.lang.Exception) {
                error("Failed to read response. (%s)", e.message)
                throw e
            } finally {
                connection.disconnect()
            }
            httpResponse.response = sb.toString()
            return httpResponse
        }

        @Throws(IOException::class)
        fun createPOSTHttpsURLConnection(
            urlString: String?,
            postBody: Map<String, String>?,
            connectionOptions: IConnectionOptions
        ): HttpsURLConnection {
            var wr: DataOutputStream? = null
            val connection: HttpsURLConnection?
            return try {
                debug("POST request: %s", urlString)
                val url = URL(urlString)
                connection = url.openConnection() as HttpsURLConnection
                connectionOptions.applyConnectionOptions(connection)
                connection.requestMethod = "POST"
                connection.useCaches = false
                connection.doInput = true
                connection.doOutput = true
                postBody?.let {
                    if (it.isNotEmpty()) {
                        wr = DataOutputStream(connection.outputStream)
                        wr?.writeBytes(getPostDataString(it))
                    }
                }
                connection
            } catch (e: Exception) {
                throw e
            } finally {
                try {
                    wr?.let {
                        it.flush()
                        it.close()
                    }
                } catch (e: Exception) {
                }
            }
        }

        @Throws(UnsupportedEncodingException::class)
        private fun getPostDataString(body: Map<String, String>): String {
            val result = StringBuilder()
            for ((key, value) in body) {
                val encodedName = URLEncoder.encode(key, Constants.ENCODING)
                val encodedValue =
                    URLEncoder.encode(value, Constants.ENCODING)
                if (result.isNotEmpty()) {
                    result.append("&")
                }
                result.append(encodedName)
                result.append("=")
                result.append(encodedValue)
            }
            return result.toString()
        }

        class HttpResponse {
            var response: String? = null
            var responseCode by Delegates.notNull<Int>()
            var headerFields: Map<String, List<String>>? = null
        }

        interface IConnectionOptions {
            fun applyConnectionOptions(connection: HttpsURLConnection)
        }

        class ConnectionOptions : IConnectionOptions {
            var clientSdk: String? = null
            var testNames: String? = null

            override fun applyConnectionOptions(connection: HttpsURLConnection) {
                clientSdk?.let { connection.setRequestProperty("Client-SDK", clientSdk) }
                testNames?.let { connection.setRequestProperty("Test-Names", testNames) }

                //Inject local ip address for Jenkins script
                connection.setRequestProperty(
                    "Local-Ip",
                    getIPAddress(true)
                )

                connection.connectTimeout = Constants.ONE_MINUTE
                connection.readTimeout = Constants.ONE_MINUTE
                try {
                    val sc = SSLContext.getInstance("TLS")
                    sc.init(null, trustAllCerts, SecureRandom())
                    connection.sslSocketFactory = sc.socketFactory
                    connection.hostnameVerifier = hostnameVerifier
                    debug("applyConnectionOptions")
                } catch (e: java.lang.Exception) {
                    debug("applyConnectionOptions ${e.message}")
                }
            }
        }

        fun getIPAddress(useIPv4: Boolean): String {
            try {
                val interfaces: List<NetworkInterface> =
                    Collections.list(NetworkInterface.getNetworkInterfaces())
                for (networkInterface in interfaces) {
                    val addresses: List<InetAddress> =
                        Collections.list(networkInterface.inetAddresses)
                    for (address in addresses) {
                        if (!address.isLoopbackAddress) {
                            val hostAddress = address.hostAddress
                            val isIPv4 = hostAddress.indexOf(':') < 0
                            if (useIPv4) {
                                if (isIPv4) return hostAddress
                            } else {
                                if (!isIPv4) {
                                    val delimit = hostAddress.indexOf('%') // drop ip6 zone suffix
                                    return if (delimit < 0) hostAddress.uppercase(Locale.getDefault())
                                    else hostAddress.substring(0, delimit).uppercase(Locale.getDefault())
                                }
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                error("Failed to read ip address (${ex.message})")
            }
            return ""
        }
    }
}