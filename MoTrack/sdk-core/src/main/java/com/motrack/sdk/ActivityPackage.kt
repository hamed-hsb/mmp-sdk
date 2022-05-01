package com.motrack.sdk

import java.io.*
import java.util.*
import kotlin.collections.HashMap

/**
 * @author yaya (@yahyalmh)
 * @since 06th October 2021
 */

class ActivityPackage(var activityKind: ActivityKind = ActivityKind.UNKNOWN) :
    Serializable {
    // data
    var path: String? = null
    var parameters: HashMap<String, String>? = null
    var clientSdk: String? = null
    var suffix: String? = null
        get() = field

    @Transient // it will not be serialized
    var hashCode = 0

    // delay
    var callbackParameters: HashMap<String, String>? = null
    var partnerParameters: HashMap<String, String>? = null

    var retries = 0

    var clickTimeInMilliseconds: Long = 0

    var clickTimeInSeconds: Long = 0

    var installBeginTimeInSeconds: Long = 0

    var clickTimeServerInSeconds: Long = 0

    var installBeginTimeServerInSeconds: Long = 0

    var installVersion: String? = null

    var googlePlayInstant: Boolean? = null

    val logger = MotrackFactory.getLogger()


    companion object {
        private const val serialVersionUID = -35935556512024097L
        private val serialPersistentFields = arrayOf(
            ObjectStreamField("path", String::class.java),
            ObjectStreamField("clientSdk", String::class.java),
            ObjectStreamField(
                "parameters",
                HashMap::class.java as Class<HashMap<String, String>?>
            ),
            ObjectStreamField("activityKind", ActivityKind::class.java),
            ObjectStreamField("suffix", String::class.java),
            ObjectStreamField(
                "callbackParameters",
                HashMap::class.java as Class<HashMap<String, String>?>
            ),
            ObjectStreamField(
                "partnerParameters",
                HashMap::class.java as Class<HashMap<String, String>?>
            ),
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
        try {
            stream.defaultWriteObject()
        }catch (e: Exception){
            logger.error("Error when serializing ActivityPackage: ${e.message}")
        }
    }

    @Throws(ClassNotFoundException::class, IOException::class)
    private fun readObject(stream: ObjectInputStream) {
        val fields = stream.readFields()
        path = readField(fields, "path", null) as String?
        clientSdk = readField(fields, "clientSdk", null) as String?
        activityKind = readField(fields, "activityKind", ActivityKind.UNKNOWN) as ActivityKind
        suffix = readField(fields, "suffix", null) as String?
        parameters = readField(fields, "parameters", null) as HashMap<String, String>?
        callbackParameters =
            readField(fields, "callbackParameters", null) as HashMap<String, String>?
        partnerParameters = readField(fields, "partnerParameters", null) as HashMap<String, String>?
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

    fun getFailureMessage(): String {
        return "Failed to track $activityKind$suffix"
    }

    override fun toString(): String {
        return "$activityKind$suffix"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ActivityPackage

        if (path != other.path) return false
        if (clientSdk != other.clientSdk) return false
        if (parameters != other.parameters) return false
        if (activityKind != other.activityKind) return false
        if (suffix != other.suffix) return false
        if (callbackParameters != other.callbackParameters) return false
        if (partnerParameters != other.partnerParameters) return false

        return true
    }

    override fun hashCode(): Int {
        if (hashCode == 0) {
            hashCode = 17
            hashCode = 31 * hashCode + path.hashCode()
            hashCode = 31 * hashCode + clientSdk.hashCode()
            hashCode = 31 * hashCode + parameters.hashCode()
            hashCode = 31 * hashCode + activityKind.hashCode()
            hashCode = 31 * hashCode + (suffix?.hashCode() ?: 0)
            hashCode = 31 * hashCode + callbackParameters.hashCode()
            hashCode = 31 * hashCode + partnerParameters.hashCode()
        }

        return hashCode
    }
}