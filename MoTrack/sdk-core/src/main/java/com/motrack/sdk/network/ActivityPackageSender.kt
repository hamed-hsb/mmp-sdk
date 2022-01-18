package com.motrack.sdk.network

import android.net.Uri
import com.motrack.sdk.*
import com.motrack.sdk.scheduler.SingleThreadCachedScheduler
import com.motrack.sdk.scheduler.ThreadExecutor
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.*
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLHandshakeException

/**
 * @author yaya (@yahyalmh)
 * @since 11th October 2021
 */

class ActivityPackageSender(
    motrackUrlStrategy: String?,
    private val basePath: String?,
    private val gdprPath: String?,
    private val subscriptionPath: String?,
    private val clientSdk: String?
) : IActivityPackageSender {

    private var logger: ILogger = MotrackFactory.getLogger()
    private var executor: ThreadExecutor? = null
    private var urlStrategy: UrlStrategy? = null
    private var httpsURLConnectionProvider: NetworkUtil.IHttpsURLConnectionProvider? = null
    private var connectionOptions: NetworkUtil.IConnectionOptions? = null

    init {
        executor = SingleThreadCachedScheduler("ActivityPackageSender")
        urlStrategy = UrlStrategy(
            MotrackFactory.baseUrl,
            MotrackFactory.gdprUrl,
            MotrackFactory.subscriptionUrl,
            motrackUrlStrategy
        )
        httpsURLConnectionProvider = MotrackFactory.httpsURLConnectionProvider
        connectionOptions = MotrackFactory.connectionOptions
    }

    override fun sendActivityPackage(
        activityPackage: ActivityPackage,
        sendingParameters: Map<String, String>,
        responseCallback: IActivityPackageSender.ResponseDataCallbackSubscriber
    ) {
        executor!!.submit {
            responseCallback.onResponseDataCallback(
                sendActivityPackageSync(activityPackage, sendingParameters)
            )
        }
    }

    override fun sendActivityPackageSync(
        activityPackage: ActivityPackage,
        sendingParameters: Map<String, String>
    ): ResponseData {
        var retryToSend: Boolean
        var responseData: ResponseData
        do {
            responseData = ResponseData.buildResponseData(activityPackage, sendingParameters)
            tryToGetResponse(responseData)
            retryToSend = shouldRetryToSend(responseData)
        } while (retryToSend)
        return responseData
    }

    private fun shouldRetryToSend(responseData: ResponseData): Boolean {
        if (!responseData.willRetry) {
            logger.debug("Will not retry with current url strategy")
            urlStrategy!!.resetAfterSuccess()
            return false
        }
        return if (urlStrategy!!.shouldRetryAfterFailure(responseData.activityKind!!)) {
            logger.error("Failed with current url strategy, but it will retry with new")
            true
        } else {
            logger.error("Failed with current url strategy and it will not retry")
            //  Stop retrying with different type and return to caller
            false
        }
    }

    private fun tryToGetResponse(responseData: ResponseData) {
        var dataOutputStream: DataOutputStream? = null
        try {
            val activityPackage = responseData.activityPackage!!
            val activityPackageParameters: HashMap<String, String> =
                activityPackage.parameters as HashMap<String, String>
            val sendingParameters = responseData.sendingParameters
            val authorizationHeader: String? = buildAndExtractAuthorizationHeader(
                activityPackageParameters,
                activityPackage.activityKind
            )
            val shouldUseGET =
                responseData.activityPackage!!.activityKind === ActivityKind.ATTRIBUTION
            val urlString: String = if (shouldUseGET) {
                extractEventCallbackId(activityPackageParameters)
                generateUrlStringForGET(
                    activityPackage.activityKind,
                    activityPackage.path!!,
                    activityPackageParameters,
                    sendingParameters
                )
            } else {
                generateUrlStringForPOST(
                    activityPackage.activityKind,
                    activityPackage.path!!
                )
            }
            val url = URL(urlString)
            val connection = httpsURLConnectionProvider!!.generateHttpsURLConnection(url)

            // get and apply connection options (default or for tests)
            connectionOptions!!.applyConnectionOptions(connection, activityPackage.clientSdk!!)
            if (authorizationHeader != null) {
                connection.setRequestProperty("Authorization", authorizationHeader)
            }
            dataOutputStream = if (shouldUseGET) {
                configConnectionForGET(connection)
            } else {
                extractEventCallbackId(activityPackageParameters)
                configConnectionForPOST(
                    connection,
                    activityPackageParameters,
                    sendingParameters!!
                )
            }

            // read connection response
            val responseCode: Int? = readConnectionResponse(connection, responseData)
            responseData.success =
                responseData.jsonResponse != null && responseData.retryIn == null && responseCode != null && responseCode == HttpsURLConnection.HTTP_OK
            // it is only processed by the server if it contains
            //  a JSON response *AND* does not contain a retry_in
            responseData.willRetry =
                responseData.jsonResponse == null || responseData.retryIn != null
        } catch (exception: UnsupportedEncodingException) {
            localError(exception, "Failed to encode parameters", responseData)
        } catch (exception: MalformedURLException) {
            localError(exception, "Malformed URL", responseData)
        } catch (exception: ProtocolException) {
            localError(exception, "Protocol Error", responseData)
        } catch (exception: SocketTimeoutException) {

            // timeout is remote/network related -> did not fail locally
            remoteError(exception, "Request timed out", responseData)
        } catch (exception: SSLHandshakeException) {

            // failed due certificate from the server -> did not fail locally
            remoteError(exception, "Certificate failed", responseData)
        } catch (exception: IOException) {

            // IO is the network -> did not fail locally
            remoteError(exception, "Request failed", responseData)
        } catch (t: Throwable) {

            // not sure if error is local or not -> assume it is local
            localError(t, "Sending SDK package", responseData)
        } finally {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.flush()
                    dataOutputStream.close()
                }
            } catch (ioException: IOException) {
                val errorMessage: String = errorMessage(
                    ioException,
                    "Flushing and closing connection output stream",
                    responseData.activityPackage!!
                )
                logger.error(errorMessage)
            }
        }
    }

    private fun localError(throwable: Throwable, description: String, responseData: ResponseData) {
        val finalMessage = errorMessage(throwable, description, responseData.activityPackage!!)
        logger.error(finalMessage)
        responseData.message = finalMessage
        responseData.willRetry = false
    }

    private fun remoteError(throwable: Throwable, description: String, responseData: ResponseData) {
        val finalMessage = (errorMessage(throwable, description, responseData.activityPackage!!)
                + " Will retry later")
        logger.error(finalMessage)
        responseData.message = finalMessage
        responseData.willRetry = true
    }

    private fun errorMessage(
        throwable: Throwable,
        description: String,
        activityPackage: ActivityPackage
    ): String {
        val failureMessage = activityPackage.getFailureMessage()
        val reasonString: String = Util.getReasonString(description, throwable)
        return "$failureMessage. ($reasonString)"
    }

    @Throws(MalformedURLException::class)
    private fun generateUrlStringForGET(
        activityKind: ActivityKind,
        activityPackagePath: String,
        activityPackageParameters: Map<String, String>,
        sendingParameters: Map<String, String>?
    ): String {
        val targetUrl = urlStrategy!!.targetUrlByActivityKind(activityKind)

        // extra path, if present, has the format '/X/Y'
        val urlWithPath: String = urlWithExtraPathByActivityKind(activityKind, targetUrl)
        val urlObject = URL(urlWithPath)
        val uriBuilder = Uri.Builder()
        uriBuilder.scheme(urlObject.protocol)
        uriBuilder.encodedAuthority(urlObject.authority)
        uriBuilder.path(urlObject.path)
        uriBuilder.appendPath(activityPackagePath)
        logger.debug("Making request to url: $uriBuilder")
        for ((key, value) in activityPackageParameters) {
            uriBuilder.appendQueryParameter(key, value)
        }
        if (sendingParameters != null) {
            for ((key, value) in sendingParameters) {
                uriBuilder.appendQueryParameter(key, value)
            }
        }
        return uriBuilder.build().toString()
    }

    private fun generateUrlStringForPOST(
        activityKind: ActivityKind,
        activityPackagePath: String
    ): String {
        val targetUrl = urlStrategy!!.targetUrlByActivityKind(activityKind)

        // extra path, if present, has the format '/X/Y'
        val urlWithPath: String = urlWithExtraPathByActivityKind(activityKind, targetUrl)


        // 'targetUrl' does not end with '/', but activity package paths that are sent by POST
        //  do start with '/', so it's not added om between
        val urlString = "$urlWithPath$activityPackagePath"
        logger.debug("Making request to url : $urlString")
        return urlString
    }

    private fun urlWithExtraPathByActivityKind(
        activityKind: ActivityKind,
        targetUrl: String
    ): String {
        return when (activityKind) {
            ActivityKind.GDPR -> if (gdprPath != null) targetUrl + gdprPath else targetUrl
            ActivityKind.SUBSCRIPTION -> if (subscriptionPath != null) targetUrl + subscriptionPath else targetUrl
            else -> if (basePath != null) targetUrl + basePath else targetUrl
        }
    }

    @Throws(ProtocolException::class)
    private fun configConnectionForGET(connection: HttpsURLConnection): DataOutputStream? {
        // set default GET configuration options
        connection.requestMethod = "GET"
        return null
    }

    @Throws(ProtocolException::class, UnsupportedEncodingException::class, IOException::class)
    private fun configConnectionForPOST(
        connection: HttpsURLConnection,
        activityPackageParameters: Map<String, String>,
        sendingParameters: Map<String, String>
    ): DataOutputStream? {
        // set default POST configuration options
        connection.requestMethod = "POST"
        // don't allow caching, that is controlled by retrying mechanisms
        connection.useCaches = false
        // necessary to read the response
        connection.doInput = true
        // necessary to pass the body to the connection
        connection.doOutput = true

        // build POST body
        val postBodyString: String = generatePOSTBodyString(
            activityPackageParameters,
            sendingParameters
        ) ?: return null

        // write POST body to connection
        val dataOutputStream = DataOutputStream(connection.outputStream)
        dataOutputStream.writeBytes(postBodyString)
        return dataOutputStream
    }

    @Throws(UnsupportedEncodingException::class)
    private fun generatePOSTBodyString(
        parameters: Map<String, String>,
        sendingParameters: Map<String, String>
    ): String? {
        if (parameters.isEmpty()) {
            return null
        }
        val postStringBuilder = StringBuilder()
        injectParametersToPOSTStringBuilder(parameters, postStringBuilder)
        injectParametersToPOSTStringBuilder(sendingParameters, postStringBuilder)

        // delete last added &
        if (postStringBuilder.isNotEmpty()
            && postStringBuilder[postStringBuilder.length - 1] == '&'
        ) {
            postStringBuilder.deleteCharAt(postStringBuilder.length - 1)
        }
        return postStringBuilder.toString()
    }

    @Throws(UnsupportedEncodingException::class)
    private fun injectParametersToPOSTStringBuilder(
        parametersToInject: Map<String, String>?,
        postStringBuilder: StringBuilder
    ) {
        if (parametersToInject == null || parametersToInject.isEmpty()) {
            return
        }

        for ((key, value) in parametersToInject) {
            val encodedName = URLEncoder.encode(key, Constants.ENCODING)
            val encodedValue =
                if (value != null) URLEncoder.encode(value, Constants.ENCODING) else ""
            postStringBuilder.append(encodedName)
            postStringBuilder.append("=")
            postStringBuilder.append(encodedValue)
            postStringBuilder.append("&")
        }
    }

    private fun readConnectionResponse(
        connection: HttpsURLConnection?,
        responseData: ResponseData
    ): Int? {
        val responseStringBuilder = StringBuilder()
        var responseCode: Int? = null

        // connect and read response to string builder
        try {
            connection!!.connect()
            responseCode = connection.responseCode
            val inputStream: InputStream =
                if (responseCode.toInt() >= Constants.MINIMAL_ERROR_STATUS_CODE) {
                    connection.errorStream
                } else {
                    connection.inputStream
                }
            val inputStreamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                responseStringBuilder.append(line)
            }
        } catch (ioException: IOException) {
            val errorMessage = errorMessage(
                ioException,
                "Connecting and reading response",
                responseData.activityPackage!!
            )
            logger.error(errorMessage)
        } finally {
            connection?.disconnect()
        }
        if (responseStringBuilder.isEmpty()) {
            logger.error("Empty response string buffer")
            return responseCode
        }
        if (responseCode == 429) {
            logger.error("Too frequent requests to the endpoint (429)")
            return responseCode
        }

        // extract response string from string builder
        val responseString = responseStringBuilder.toString()
        logger.debug("Response string: $responseString")
        parseResponse(responseData, responseString)
        val responseMessage = responseData.message ?: return responseCode

        // log response message
        if (responseCode != null && responseCode.toInt() == HttpsURLConnection.HTTP_OK) {
            logger.info("Response message: $responseMessage")
        } else {
            logger.error("Response message: $responseMessage")
        }
        return responseCode
    }

    private fun parseResponse(responseData: ResponseData, responseString: String) {
        if (responseString.isEmpty()) {
            logger.error("Empty response string")
            return
        }
        var jsonResponse: JSONObject? = null
        // convert string response to JSON object
        try {
            jsonResponse = JSONObject(responseString)
        } catch (jsonException: JSONException) {
            val errorMessage = errorMessage(
                jsonException,
                "Failed to parse JSON response",
                responseData.activityPackage!!
            )
            logger.error(errorMessage)
        }
        if (jsonResponse == null) {
            return
        }
        responseData.jsonResponse = jsonResponse
        responseData.message = NetworkUtil.extractJsonString(jsonResponse, "message")
        responseData.adid = NetworkUtil.extractJsonString(jsonResponse, "adid")
        responseData.timestamp = NetworkUtil.extractJsonString(jsonResponse, "timestamp")
        val trackingState: String? = NetworkUtil.extractJsonString(jsonResponse, "tracking_state")
        if (trackingState != null) {
            if (trackingState == "opted_out") {
                responseData.trackingState = TrackingState.OPTED_OUT
            }
        }
        responseData.askIn = NetworkUtil.extractJsonLong(jsonResponse, "ask_in")
        responseData.retryIn = NetworkUtil.extractJsonLong(jsonResponse, "retry_in")
        responseData.continueIn = NetworkUtil.extractJsonLong(jsonResponse, "continue_in")
        val attributionJson = jsonResponse.optJSONObject("attribution")
        responseData.attribution = MotrackAttribution.fromJson(
            attributionJson,
            responseData.adid,
            Util.getSdkPrefixPlatform(clientSdk!!)
        )
    }

    private fun buildAndExtractAuthorizationHeader(
        parameters: MutableMap<String, String>,
        activityKind: ActivityKind
    ): String? {
        val activityKindString = activityKind.toString()
        val secretId: String? = extractSecretId(parameters)
        val headersId: String? = extractHeadersId(parameters)
        val signature: String? = extractSignature(parameters)
        val algorithm: String? = extractAlgorithm(parameters)
        val nativeVersion: String? = extractNativeVersion(parameters)
        val authorizationHeader: String? = buildAuthorizationHeaderV2(
            signature, secretId,
            headersId, algorithm, nativeVersion
        )
        if (authorizationHeader != null) {
            return authorizationHeader
        }
        val appSecret: String? = extractAppSecret(parameters)
        return buildAuthorizationHeaderV1(parameters, appSecret, secretId, activityKindString)
    }

    private fun buildAuthorizationHeaderV1(
        parameters: Map<String, String>,
        appSecret: String?,
        secretId: String?,
        activityKindString: String
    ): String? {
        // check if the secret exists and it's not empty
        if (appSecret == null || appSecret.isEmpty()) {
            return null
        }
        val appSecretName = "app_secret"
        val signatureDetails: Map<String, String?> =
            getSignature(parameters, activityKindString, appSecret)
        val algorithm = "sha256"
        val signature = Util.sha256(signatureDetails["clear_signature"]!!)
        val fields = signatureDetails["fields"]
        val secretIdHeader = "secret_id=\"$secretId\""
        val signatureHeader = "signature=\"$signature\""
        val algorithmHeader = "algorithm=\"$algorithm\""
        val fieldsHeader = "headers=\"$fields\""
        val authorizationHeader =
            "Signature $secretIdHeader,$signatureHeader,$algorithmHeader,$fieldsHeader"

        logger.verbose("authorizationHeader: $authorizationHeader")
        return authorizationHeader
    }

    private fun buildAuthorizationHeaderV2(
        signature: String?,
        secretId: String?,
        headersId: String?,
        algorithm: String?,
        nativeVersion: String?
    ): String? {
        if (secretId == null || signature == null || headersId == null) {
            return null
        }
        val signatureHeader = "signature=\"$signature\""
        val secretIdHeader = "secret_id=\"$secretId\""
        val idHeader = "headers_id=\"$headersId\""
        val algorithmHeader = "algorithm=\"${algorithm ?: "adj1"}\""
        val nativeVersionHeader = "native_version=\"${nativeVersion ?: ""}\""
        val authorizationHeader =
            "Signature $signatureHeader,$secretIdHeader,$algorithmHeader,$idHeader,$nativeVersionHeader"
        logger.verbose("authorizationHeader: $authorizationHeader")
        return authorizationHeader
    }

    private fun getSignature(
        parameters: Map<String, String>,
        activityKindString: String,
        appSecret: String
    ): Map<String, String?> {
        val activityKindName = "activity_kind"
        val createdAtName = "created_at"
        val createdAt = parameters[createdAtName]
        val deviceIdentifierName: String? = getValidIdentifier(parameters)
        val deviceIdentifier = parameters[deviceIdentifierName]
        val sourceName = "source"
        val sourceValue = parameters[sourceName]
        val payloadName = "payload"
        val payloadValue = parameters[payloadName]
        val signatureParams: MutableMap<String, String?> =
            HashMap()
        signatureParams["app_secret"] = appSecret
        signatureParams[createdAtName] = createdAt
        signatureParams[activityKindName] = activityKindString
        signatureParams[deviceIdentifierName!!] = deviceIdentifier
        if (sourceValue != null) {
            signatureParams[sourceName] = sourceValue
        }
        if (payloadValue != null) {
            signatureParams[payloadName] = payloadValue
        }
        var fields = ""
        var clearSignature = ""
        for ((key, value) in signatureParams) {
            if (value != null) {
                fields += "$key "
                clearSignature += value
            }
        }

        // Remove last empty space.
        fields = fields.substring(0, fields.length - 1)
        val signature = HashMap<String, String?>()
        signature["clear_signature"] = clearSignature
        signature["fields"] = fields
        return signature
    }

    private fun getValidIdentifier(parameters: Map<String, String>): String? {
        val googleAdIdName = "gps_adid"
        val fireAdIdName = "fire_adid"
        val androidIdName = "android_id"
        val androidUUIDName = "android_uuid"
        if (parameters[googleAdIdName] != null) {
            return googleAdIdName
        }
        if (parameters[fireAdIdName] != null) {
            return fireAdIdName
        }
        if (parameters[androidIdName] != null) {
            return androidIdName
        }

        return if (parameters[androidUUIDName] != null) {
            androidUUIDName
        } else null
    }

    private fun extractAppSecret(parameters: MutableMap<String, String>): String? {
        return parameters.remove("app_secret")
    }

    private fun extractSecretId(parameters: MutableMap<String, String>): String? {
        return parameters.remove("secret_id")
    }

    private fun extractSignature(parameters: MutableMap<String, String>): String? {
        return parameters.remove("signature")
    }

    private fun extractAlgorithm(parameters: MutableMap<String, String>): String? {
        return parameters.remove("algorithm")
    }

    private fun extractNativeVersion(parameters: MutableMap<String, String>): String? {
        return parameters.remove("native_version")
    }

    private fun extractHeadersId(parameters: MutableMap<String, String>): String? {
        return parameters.remove("headers_id")
    }

    private fun extractEventCallbackId(parameters: MutableMap<String, String>) {
        parameters.remove("event_callback_id")
    }

}