package com.motrack.sdk.network

import com.motrack.sdk.Constants
import com.motrack.sdk.ILogger
import com.motrack.sdk.MotrackFactory
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * @author yaya (@yahyalmh)
 * @since 11th October 2021
 */

class NetworkUtil {

    interface IConnectionOptions {
        fun applyConnectionOptions(connection: HttpURLConnection, clientSdk: String)
    }

    interface IHttpsURLConnectionProvider {
        @Throws(IOException::class)
        fun generateHttpsURLConnection(url: URL): HttpURLConnection
    }

    companion object {
        private var userAgent: String? = null

        private fun getLogger(): ILogger {
            return MotrackFactory.getLogger()
        }

        fun setUserAgent(userAgent: String) {
            NetworkUtil.userAgent = userAgent
        }

        fun createDefaultConnectionOptions(): IConnectionOptions {
            return object : IConnectionOptions {
                override fun applyConnectionOptions(
                    connection: HttpURLConnection,
                    clientSdk: String
                ) {
                    connection.setRequestProperty("Client-SDK", clientSdk)
                    connection.connectTimeout = Constants.ONE_MINUTE.toInt()
                    connection.readTimeout = Constants.ONE_MINUTE.toInt()
                    if (userAgent != null) {
                        connection.setRequestProperty(
                            "User-Agent",
                            userAgent
                        )
                    }
                }
            }
        }

        fun createDefaultHttpsURLConnectionProvider(): IHttpsURLConnectionProvider {
            return object : IHttpsURLConnectionProvider {
                @Throws(IOException::class)
                override fun generateHttpsURLConnection(url: URL): HttpURLConnection {
                    return url.openConnection() as HttpURLConnection
                }
            }
        }

        fun extractJsonString(jsonObject: JSONObject, name: String?): String? {
            // taken from JSONObject.optString(...) to add null fallback
            val result = jsonObject.opt(name)
            return if (result is String) {
                result
            } else result?.toString()
        }

        fun extractJsonLong(jsonObject: JSONObject, name: String): Long? {
            // taken from JSONObject.optLong(...) to add null fallback
            val anObject = jsonObject.opt(name)
            if (anObject is Long) {
                return anObject
            }

            if (anObject is Number) {
                return anObject.toLong()
            }

            if (anObject is String) {
                try {
                    return anObject.toDouble().toLong()
                } catch (ignored: NumberFormatException) {
                }
            }

            return null
        }
    }
}