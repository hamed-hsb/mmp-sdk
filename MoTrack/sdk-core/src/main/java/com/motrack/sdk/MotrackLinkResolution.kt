package com.motrack.sdk

import android.net.Uri
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * @author yaya (@yahyalmh)
 * @since 19th October 2021
 */

class MotrackLinkResolution private constructor() {
    interface MotrackLinkResolutionCallback {
        fun resolvedLinkCallback(resolvedLink: Uri?)
    }

    companion object {
        // https://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
        @Volatile
        private var executor: ExecutorService? = null

        private const val maxRecursions = 10
        private val expectedUrlHostSuffixArray = arrayOf(
            "motrack.com",
            "go.link"
        )

        fun resolveLink(
            url: String?,
            resolveUrlSuffixArray: Array<String>?,
            motrackLinkResolutionCallback: MotrackLinkResolutionCallback?
        ) {
            if (motrackLinkResolutionCallback == null) {
                return
            }
            if (url == null) {
                motrackLinkResolutionCallback.resolvedLinkCallback(null)
                return
            }
            var originalURL: URL? = null
            try {
                originalURL = URL(url)
            } catch (ignored: MalformedURLException) {
            }
            if (originalURL == null) {
                motrackLinkResolutionCallback.resolvedLinkCallback(null)
                return
            }
            if (!urlMatchesSuffix(
                    originalURL.host,
                    resolveUrlSuffixArray
                )
            ) {
                motrackLinkResolutionCallback.resolvedLinkCallback(
                    convertToUri(originalURL)
                )
                return
            }
            if (executor == null) {
                synchronized(expectedUrlHostSuffixArray) {
                    if (executor == null) {
                        executor = Executors.newSingleThreadExecutor()
                    }
                }
            }
            val finalOriginalURL: URL = originalURL
            executor!!.execute(Runnable {
                requestAndResolve(
                    finalOriginalURL,
                    0,
                    motrackLinkResolutionCallback
                )
            })
        }

        private fun resolveLink(
            responseUrl: URL?,
            previousUrl: URL,
            recursionNumber: Int,
            motrackLinkResolutionCallback: MotrackLinkResolutionCallback
        ) {
            // return (possible null) previous url when the current one does not exist
            if (responseUrl == null) {
                motrackLinkResolutionCallback.resolvedLinkCallback(
                    convertToUri(previousUrl)
                )
                return
            }

            // return found url with expected host
            if (isTerminalUrl(responseUrl.host)) {
                motrackLinkResolutionCallback.resolvedLinkCallback(
                    convertToUri(responseUrl)
                )
                return
            }

            // return previous (non-null) url when it reached the max number of recursive tries
            if (recursionNumber > maxRecursions) {
                motrackLinkResolutionCallback.resolvedLinkCallback(
                    convertToUri(responseUrl)
                )
                return
            }
            requestAndResolve(
                responseUrl,
                recursionNumber,
                motrackLinkResolutionCallback
            )
        }

        private fun requestAndResolve(
            urlToRequest: URL,
            recursionNumber: Int,
            motrackLinkResolutionCallback: MotrackLinkResolutionCallback
        ) {
            val httpsUrl: URL? = convertToHttps(urlToRequest)
            var resolvedURL: URL? = null
            var ucon: HttpURLConnection? = null
            try {
                ucon = httpsUrl!!.openConnection() as HttpURLConnection
                ucon.instanceFollowRedirects = false
                ucon.connect()
                val headerLocationField = ucon.getHeaderField("Location")
                if (headerLocationField != null) {
                    resolvedURL = URL(headerLocationField)
                }
            } catch (ignored: Throwable) {
            } finally {
                ucon?.disconnect()
                resolveLink(
                    resolvedURL,
                    httpsUrl!!,
                    recursionNumber + 1,
                    motrackLinkResolutionCallback
                )
            }
        }

        private fun isTerminalUrl(urlHost: String): Boolean {
            return urlMatchesSuffix(
                urlHost,
                MotrackLinkResolution.expectedUrlHostSuffixArray
            )
        }

        private fun urlMatchesSuffix(urlHost: String?, suffixArray: Array<String>?): Boolean {
            if (urlHost == null) {
                return false
            }
            if (suffixArray == null) {
                return false
            }
            for (expectedUrlHostSuffix in suffixArray) {
                if (urlHost.endsWith(expectedUrlHostSuffix)) {
                    return true
                }
            }
            return false
        }

        private fun convertToHttps(urlToConvert: URL?): URL? {
            if (urlToConvert == null) {
                return urlToConvert
            }
            val stringUrlToConvert = urlToConvert.toExternalForm() ?: return urlToConvert
            if (!stringUrlToConvert.startsWith("http:")) {
                return urlToConvert
            }
            var convertedUrl = urlToConvert
            try {
                convertedUrl = URL("https:" + stringUrlToConvert.substring(5))
            } catch (ignored: MalformedURLException) {
            }
            return convertedUrl
        }

        private fun convertToUri(url: URL?): Uri? {
            return if (url == null) {
                null
            } else Uri.parse(url.toString())
        }

    }
}