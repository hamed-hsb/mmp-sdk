package com.motrack.sdk

import java.io.*
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 06th October 2021
 */

class ActivityPackage(private var activityKind: ActivityKind) : Serializable {
    // data
    private var path: String? = null
    private var parameters: Map<String, String>? = null
    private var clientSdk: String? = null

    @Transient // it will not be serialized
    private val hashCode = 0
    private var suffix: String? = null

    // delay
    private var callbackParameters: Map<String, String>? = null
    private var partnerParameters: Map<String, String>? = null

    private var retries = 0
    private var clickTimeInMilliseconds: Long = 0
    private var clickTimeInSeconds: Long = 0
    private var installBeginTimeInSeconds: Long = 0
    private var clickTimeServerInSeconds: Long = 0
    private var installBeginTimeServerInSeconds: Long = 0
    private var installVersion: String? = null
    private var googlePlayInstant: Boolean? = null

    private val logger = MotrackFactory.getLogger()


    companion object {
        private const val serialVersionUID = -35935556512024097L
        private val serialPersistentFields = arrayOf(
            ObjectStreamField("path", String::class.java),
            ObjectStreamField("clientSdk", String::class.java),
            ObjectStreamField(
                "parameters",
                Map::class.java as Class<Map<String?, String?>?>
            ),
            ObjectStreamField("activityKind", ActivityKind::class.java),
            ObjectStreamField("suffix", String::class.java),
            ObjectStreamField(
                "callbackParameters",
                Map::class.java as Class<Map<String?, String?>?>
            ),
            ObjectStreamField(
                "partnerParameters",
                Map::class.java as Class<Map<String?, String?>?>
            )
        )

    }

    public fun getExtendedString(): String {
        val builder = StringBuilder()
        builder.append("Path:       $path\n")
        builder.append("ClientSdk:  $clientSdk\n")

        if (!parameters.isNullOrEmpty()) {
            builder.append("Parameters: ")
            val sortedParameters = TreeMap(parameters!!)
            val stringsToExclude = listOf("app_secret", "secret_id", "event_callback_id")
            for ((key, value) in sortedParameters.entries) {
                if (stringsToExclude.contains(key)) {
                    continue
                }
                builder.append("\n\t$key $value")
            }
        }
        return builder.toString()
    }

    @Throws(ClassNotFoundException::class, IOException::class)
    private fun writeObject(stream: ObjectOutputStream) {
        stream.defaultWriteObject()

    }

    @Throws(ClassNotFoundException::class, IOException::class)
    private fun readObject(stream: ObjectInputStream) {
        val fields = stream.readFields()
        path = readField(fields, "path", null) as String?
        clientSdk = readField(fields, "clientSdk", null) as String?
        activityKind = readField(fields, "activityKind", ActivityKind.UNKNOWN) as ActivityKind
        suffix = readField(fields, "suffix", null) as String?
        parameters = readField(fields, "parameters", null) as Map<String, String>?
        callbackParameters = readField(fields, "callbackParameters", null) as Map<String, String>?
        partnerParameters = readField(fields, "partnerParameters", null) as Map<String, String>?
    }

    private fun readField(
        fields: ObjectInputStream.GetField,
        name: String?,
        defaultValue: Any?
    ): Any? {
        return try {
            fields[name, defaultValue]
        } catch (e: Exception) {
            logger.debug("Unable to read '$name' field in migration device with message (${e.message})")
            defaultValue
        }
    }

    fun increaseRetries(): Int {
        retries++
        return retries
    }

    public fun getPath(): String? {
        return path
    }

    public fun setPath(path: String) {
        this.path = path
    }


    public fun setClientSdk(clientSdk: String) {
        this.clientSdk = clientSdk
    }

    public fun getClientSdk(): String? {
        return clientSdk
    }

    public fun getParameters(): Map<String, String>? {
        return parameters
    }

    fun setParameters(parameters: Map<String, String>) {
        this.parameters = parameters
    }

    fun setCallbackParameters(callbackParameters: Map<String, String>) {
        this.callbackParameters = callbackParameters
    }


    fun setPartnerParameters(partnerParameters: Map<String, String>) {
        this.partnerParameters = partnerParameters
    }

    @JvmName("getPackageActivityKind")
    fun getActivityKind(): ActivityKind {
        return activityKind
    }

    fun getFailureMessage(): String {
        return "Failed to track $activityKind$suffix"
    }

    fun getSuffix(): String? {
        return suffix
    }

    fun setSuffix(suffix: String?) {
        this.suffix = suffix
    }

    fun getRetries(): Int {
        return retries
    }

    fun getClickTimeInMilliseconds(): Long {
        return clickTimeInMilliseconds
    }

    fun setClickTimeInMilliseconds(clickTimeInMilliseconds: Long) {
        this.clickTimeInMilliseconds = clickTimeInMilliseconds
    }

    fun getClickTimeInSeconds(): Long {
        return clickTimeInSeconds
    }

    fun setClickTimeInSeconds(clickTimeInSeconds: Long) {
        this.clickTimeInSeconds = clickTimeInSeconds
    }

    fun getInstallBeginTimeInSeconds(): Long {
        return installBeginTimeInSeconds
    }

    fun setInstallBeginTimeInSeconds(installBeginTimeInSeconds: Long) {
        this.installBeginTimeInSeconds = installBeginTimeInSeconds
    }

    fun getClickTimeServerInSeconds(): Long {
        return clickTimeServerInSeconds
    }

    fun setClickTimeServerInSeconds(clickTimeServerInSeconds: Long) {
        this.clickTimeServerInSeconds = clickTimeServerInSeconds
    }

    fun getInstallBeginTimeServerInSeconds(): Long {
        return installBeginTimeServerInSeconds
    }

    fun setInstallBeginTimeServerInSeconds(installBeginTimeServerInSeconds: Long) {
        this.installBeginTimeServerInSeconds = installBeginTimeServerInSeconds
    }

    fun getInstallVersion(): String? {
        return installVersion
    }

    fun setInstallVersion(installVersion: String?) {
        this.installVersion = installVersion
    }

    fun getGooglePlayInstant(): Boolean? {
        return googlePlayInstant
    }

    fun setGooglePlayInstant(googlePlayInstant: Boolean?) {
        this.googlePlayInstant = googlePlayInstant
    }

    fun getCallbackParameters(): Map<String, String>? {
        return callbackParameters
    }

    fun getPartnerParameters(): Map<String, String>? {
        return partnerParameters
    }

    public override fun toString(): String {
        return "$activityKind$suffix"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ActivityPackage

        if (activityKind != other.activityKind) return false
        if (path != other.path) return false
        if (parameters != other.parameters) return false
        if (clientSdk != other.clientSdk) return false
        if (hashCode != other.hashCode) return false
        if (suffix != other.suffix) return false
        if (callbackParameters != other.callbackParameters) return false
        if (partnerParameters != other.partnerParameters) return false
        if (retries != other.retries) return false
        if (clickTimeInMilliseconds != other.clickTimeInMilliseconds) return false
        if (clickTimeInSeconds != other.clickTimeInSeconds) return false
        if (installBeginTimeInSeconds != other.installBeginTimeInSeconds) return false
        if (clickTimeServerInSeconds != other.clickTimeServerInSeconds) return false
        if (installBeginTimeServerInSeconds != other.installBeginTimeServerInSeconds) return false
        if (installVersion != other.installVersion) return false
        if (googlePlayInstant != other.googlePlayInstant) return false

        return true
    }

    override fun hashCode(): Int {
        var result = activityKind.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + clientSdk.hashCode()
        result = 31 * result + hashCode
        result = 31 * result + (suffix?.hashCode() ?: 0)
        result = 31 * result + callbackParameters.hashCode()
        result = 31 * result + partnerParameters.hashCode()
        result = 31 * result + retries
        result = 31 * result + clickTimeInMilliseconds.hashCode()
        result = 31 * result + clickTimeInSeconds.hashCode()
        result = 31 * result + installBeginTimeInSeconds.hashCode()
        result = 31 * result + clickTimeServerInSeconds.hashCode()
        result = 31 * result + installBeginTimeServerInSeconds.hashCode()
        result = 31 * result + (installVersion?.hashCode() ?: 0)
        result = 31 * result + (googlePlayInstant?.hashCode() ?: 0)
        return result
    }
}