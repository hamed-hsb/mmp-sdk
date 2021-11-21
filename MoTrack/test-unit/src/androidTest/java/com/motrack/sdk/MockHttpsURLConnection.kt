package com.motrack.sdk

import android.os.SystemClock
import com.motrack.sdk.network.NetworkUtil
import java.io.*
import java.net.ProtocolException
import java.net.URL
import java.nio.charset.Charset
import java.security.Permission
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * @author yaya (@yahyalmh)
 * @since 20th November 2021
 */

class MockHttpsURLConnection(url: URL?) : HttpsURLConnection(url),
    NetworkUtil.IHttpsURLConnectionProvider {

    lateinit var testLogger: MockLogger
    private val prefix = "MockHttpsURLConnection "
    private var outputStream: ByteArrayOutputStream? = null
    var responseType: ResponseType? = null
    var timeout = false
    var waitingTime: Long? = null

    constructor(url: URL?, mockLogger: MockLogger) : this(url) {
        testLogger = mockLogger
    }

    @Throws(IOException::class)
    override fun getInputStream(): InputStream? {
        testLogger.test(prefix + "getInputStream, responseType: " + responseType)
        if (timeout) {
            SystemClock.sleep(10000)
        }
        if (waitingTime != null) {
            SystemClock.sleep(waitingTime!!)
        }
        return when (responseType) {
            ResponseType.CLIENT_PROTOCOL_EXCEPTION -> throw IOException("testResponseError")
            ResponseType.WRONG_JSON -> getMockResponse("not a json response")
            ResponseType.EMPTY_JSON -> getMockResponse("{ }")
            ResponseType.MESSAGE -> getMockResponse("{ \"message\" : \"response OK\"}")
            else -> null
        }
    }

    override fun getErrorStream(): InputStream? {
        testLogger.test(prefix + "getErrorStream, responseType: " + responseType)
        try {
            if (responseType === ResponseType.INTERNAL_SERVER_ERROR) {
                return getMockResponse("{ \"message\": \"testResponseError\"}")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    @Throws(IOException::class)
    private fun getMockResponse(response: String): InputStream {
        return ByteArrayInputStream(response.toByteArray(Charset.forName("UTF-8")))
    }


    fun readRequest(): String? {
        var out: String? = null
        try {
            out = String(outputStream!!.toByteArray(), Charset.forName("UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            testLogger.test("readRequest, UnsupportedEncodingException " + e.message)
        }
        return out
    }

    override fun disconnect() {
        testLogger.test(prefix + "disconnect")
    }

    override fun usingProxy(): Boolean {
        testLogger.test(prefix + "usingProxy")
        return false
    }

    @Throws(IOException::class)
    override fun connect() {
        testLogger.test(prefix + "connect")
    }

    @Throws(IOException::class)
    override fun getPermission(): Permission? {
        testLogger.test(prefix + "getPermission")
        return null
    }

    override fun getRequestMethod(): String? {
        testLogger.test(prefix + "getRequestMethod")
        return null
    }

    @Throws(IOException::class)
    override fun getResponseCode(): Int {
        testLogger.test(prefix + "getResponseCode")
        return if (responseType === ResponseType.INTERNAL_SERVER_ERROR) {
            HTTP_INTERNAL_ERROR
        } else {
            HTTP_OK
        }
    }

    @Throws(IOException::class)
    override fun getResponseMessage(): String? {
        testLogger.test(prefix + "getResponseMessage")
        return null
    }

    @Throws(ProtocolException::class)
    override fun setRequestMethod(method: String) {
        testLogger.test(prefix + "setRequestMethod, method " + method)
        super.setRequestMethod(method)
    }

    override fun getContentEncoding(): String? {
        testLogger.test(prefix + "getContentEncoding")
        return null
    }

    override fun getInstanceFollowRedirects(): Boolean {
        testLogger.test(prefix + "getInstanceFollowRedirects")
        return false
    }

    override fun setInstanceFollowRedirects(followRedirects: Boolean) {
        testLogger.test(prefix + "setInstanceFollowRedirects, followRedirects " + followRedirects)
    }

    override fun getHeaderFieldDate(field: String, defaultValue: Long): Long {
        testLogger.test(prefix + "getHeaderFieldDate, field " + field + ", defaultValue " + defaultValue)
        return 0
    }

    override fun setFixedLengthStreamingMode(contentLength: Long) {
        testLogger.test(prefix + "setFixedLengthStreamingMode, contentLength " + contentLength)
    }

    override fun setFixedLengthStreamingMode(contentLength: Int) {
        testLogger.test(prefix + "setFixedLengthStreamingMode, contentLength " + contentLength)
    }

    override fun setChunkedStreamingMode(chunkLength: Int) {
        testLogger.test(prefix + "setChunkedStreamingMode, chunkLength " + chunkLength)
    }

    override fun getAllowUserInteraction(): Boolean {
        testLogger.test(prefix + "getAllowUserInteraction")
        return false
    }

    @Throws(IOException::class)
    override fun getContent(): Any? {
        testLogger.test(prefix + "getReferrer")
        return null
    }

    @Throws(IOException::class)
    override fun getContent(types: Array<Class<*>?>): Any? {
        testLogger.test(prefix + "getReferrer, types " + types)
        return null
    }

    override fun getContentLength(): Int {
        testLogger.test(prefix + "getContentLength")
        return 0
    }

    override fun getContentType(): String? {
        testLogger.test(prefix + "getContentLength")
        return null
    }

    override fun getDate(): Long {
        testLogger.test(prefix + "getDate")
        return 0
    }

    override fun getDefaultUseCaches(): Boolean {
        testLogger.test(prefix + "getDefaultUseCaches")
        return false
    }

    override fun getDoInput(): Boolean {
        testLogger.test(prefix + "getDoInput")
        return false
    }

    override fun getDoOutput(): Boolean {
        testLogger.test(prefix + "getDoOutput")
        return false
    }

    override fun getExpiration(): Long {
        testLogger.test(prefix + "getExpiration")
        return 0
    }

    override fun getHeaderField(pos: Int): String? {
        testLogger.test(prefix + "getHeaderField, pos " + pos)
        return null
    }

    override fun getHeaderFields(): Map<String?, List<String?>?>? {
        testLogger.test(prefix + "getHeaderFields")
        return null
    }

    override fun getRequestProperties(): Map<String?, List<String?>?>? {
        testLogger.test(prefix + "getRequestProperties")
        return null
    }

    override fun addRequestProperty(field: String, newValue: String) {
        testLogger.test(prefix + "addRequestProperty, field " + field + ", newValue " + newValue)
    }

    override fun getHeaderField(key: String): String? {
        testLogger.test(prefix + "getHeaderField, key " + key)
        return null
    }

    override fun getHeaderFieldInt(field: String, defaultValue: Int): Int {
        testLogger.test(prefix + "getHeaderFieldInt, field " + field + ", defaultValue " + defaultValue)
        return 0
    }

    override fun getHeaderFieldKey(posn: Int): String? {
        testLogger.test(prefix + "getHeaderFieldKey, " + posn)
        return null
    }

    override fun getIfModifiedSince(): Long {
        testLogger.test(prefix + "getIfModifiedSince")
        return 0
    }

    override fun getLastModified(): Long {
        testLogger.test(prefix + "getLastModified")
        return 0
    }

    @Throws(IOException::class)
    override fun getOutputStream(): OutputStream? {
        testLogger.test(prefix + "getOutputStream")
        outputStream = ByteArrayOutputStream()
        return outputStream
    }

    override fun getRequestProperty(field: String): String? {
        testLogger.test(prefix + "getRequestProperty, field " + field)
        return null
    }

    override fun getURL(): URL? {
        testLogger.test(prefix + "getURL")
        return this.url
    }

    fun setURL(url: URL) {
        testLogger.test(prefix + "setURL, " + url)
    }

    override fun getUseCaches(): Boolean {
        testLogger.test(prefix + "getUseCaches")
        return false
    }

    override fun setAllowUserInteraction(newValue: Boolean) {
        testLogger.test(prefix + "setAllowUserInteraction, newValue " + newValue)
    }

    override fun setDefaultUseCaches(newValue: Boolean) {
        testLogger.test(prefix + "setDefaultUseCaches, newValue " + newValue)
    }

    override fun setDoInput(newValue: Boolean) {
        testLogger.test(prefix + "setDoInput, newValue " + newValue)
        super.setDoInput(newValue)
    }

    override fun setDoOutput(newValue: Boolean) {
        testLogger.test(prefix + "setDoOutput, newValue " + newValue)
        super.setDoOutput(newValue)
    }

    override fun setIfModifiedSince(newValue: Long) {
        testLogger.test(prefix + "setIfModifiedSince, newValue " + newValue)
    }

    override fun setRequestProperty(field: String, newValue: String) {
        testLogger.test(prefix + "setRequestProperty, field " + field + ", newValue " + newValue)
        super.setRequestProperty(field, newValue)
    }

    override fun setUseCaches(newValue: Boolean) {
        testLogger.test(prefix + "setUseCaches, newValue " + newValue)
        super.setUseCaches(newValue)
    }

    override fun setConnectTimeout(timeoutMillis: Int) {
        testLogger.test(prefix + "setConnectTimeout, timeoutMillis " + timeoutMillis)
        super.setConnectTimeout(timeoutMillis)
    }

    override fun getConnectTimeout(): Int {
        testLogger.test(prefix + "getConnectTimeout")
        return 0
    }

    override fun setReadTimeout(timeoutMillis: Int) {
        testLogger.test(prefix + "setReadTimeout, timeoutMillis " + timeoutMillis)
    }

    override fun getReadTimeout(): Int {
        testLogger.test(prefix + "getReadTimeout")
        return 0
    }

    override fun generateHttpsURLConnection(url: URL): HttpsURLConnection {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        testLogger.test(prefix + "toString")
        return "null"
    }

    override fun getCipherSuite(): String? {
        testLogger.test(prefix + "getCipherSuite")
        return null
    }

    override fun getLocalCertificates(): Array<Certificate?> {
        testLogger.test(prefix + "getLocalCertificates")
        return arrayOfNulls(0)
    }

    @Throws(SSLPeerUnverifiedException::class)
    override fun getServerCertificates(): Array<Certificate?> {
        testLogger.test(prefix + "getServerCertificates")
        return arrayOfNulls(0)
    }

}