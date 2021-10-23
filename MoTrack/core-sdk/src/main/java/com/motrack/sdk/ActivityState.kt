package com.motrack.sdk

import java.io.*
import java.util.*

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

class ActivityState : Serializable, Cloneable {
    companion object {
        const val serialVersionUID = 9039439291143138148L
        const val ORDER_ID_MAXCOUNT = 10

        private val serialPersistentFields = arrayOf(
            ObjectStreamField("uuid", String::class.java),
            ObjectStreamField("enabled", Boolean::class.javaPrimitiveType),
            ObjectStreamField("isGdprForgotten", Boolean::class.javaPrimitiveType),
            ObjectStreamField("isThirdPartySharingDisabled", Boolean::class.javaPrimitiveType),
            ObjectStreamField("askingAttribution", Boolean::class.javaPrimitiveType),
            ObjectStreamField("eventCount", Int::class.javaPrimitiveType),
            ObjectStreamField("sessionCount", Int::class.javaPrimitiveType),
            ObjectStreamField("subsessionCount", Int::class.javaPrimitiveType),
            ObjectStreamField("sessionLength", Long::class.javaPrimitiveType),
            ObjectStreamField("timeSpent", Long::class.javaPrimitiveType),
            ObjectStreamField("lastActivity", Long::class.javaPrimitiveType),
            ObjectStreamField("lastInterval", Long::class.javaPrimitiveType),
            ObjectStreamField("updatePackages", Boolean::class.javaPrimitiveType),
            ObjectStreamField(
                "orderIds",
                LinkedList::class.java as Class<LinkedList<String?>?>
            ),
            ObjectStreamField("pushToken", String::class.java),
            ObjectStreamField("adid", String::class.java),
            ObjectStreamField("clickTime", Long::class.javaPrimitiveType),
            ObjectStreamField("installBegin", Long::class.javaPrimitiveType),
            ObjectStreamField("installReferrer", String::class.java),
            ObjectStreamField("googlePlayInstant", Boolean::class.java),
            ObjectStreamField("clickTimeServer", Long::class.javaPrimitiveType),
            ObjectStreamField("installBeginServer", Long::class.javaPrimitiveType),
            ObjectStreamField("installVersion", String::class.java),
            ObjectStreamField("clickTimeHuawei", Long::class.javaPrimitiveType),
            ObjectStreamField("installBeginHuawei", Long::class.javaPrimitiveType),
            ObjectStreamField("installReferrerHuawei", String::class.java),
            ObjectStreamField("installReferrerHuaweiAppGallery", String::class.java)
        )
    }

    @Transient
    val logger: ILogger = MotrackFactory.getLogger()

    // persistent data
    var uuid: String? = null
    var enabled = true
    var isGdprForgotten = false
    var isThirdPartySharingDisabled = false
    var askingAttribution = false

    // global counters
    var eventCount = 0
    var sessionCount = 0

    // session attributes
    var subsessionCount = -1
    var sessionLength: Long = -1 // all durations in milliseconds
    var timeSpent: Long = -1
    var lastActivity: Long = -1 // all times in milliseconds since 1970


    var lastInterval: Long = -1

    var updatePackages = false

    var orderIds: LinkedList<String>? = null

    var pushToken: String? = null
    var adid: String? = null

    var clickTime: Long = 0
    var installBegin: Long = 0
    var installReferrer: String? = null
    var googlePlayInstant: Boolean? = null
    var clickTimeServer: Long = 0
    var installBeginServer: Long = 0
    var installVersion: String? = null

    var clickTimeHuawei: Long = 0
    var installBeginHuawei: Long = 0
    var installReferrerHuawei: String? = null
    var installReferrerHuaweiAppGallery: String? = null


    init {
        uuid = Util.createUuid()

    }

    fun addOrderId(orderId: String?) {
        if (orderIds == null) {
            orderIds = LinkedList()
        }
        if (orderIds!!.size >= ORDER_ID_MAXCOUNT) {
            orderIds!!.removeLast()
        }
        orderIds!!.addFirst(orderId)
    }

    fun findOrderId(orderId: String?): Boolean {
        return if (orderIds == null) {
            false
        } else orderIds!!.contains(orderId)
    }


    fun resetSessionAttributes(now: Long) {
        subsessionCount = 1 // first subsession
        sessionLength = 0 // no session length yet
        timeSpent = 0 // no time spent yet
        lastActivity = now
        lastInterval = -1
    }

    @Throws(IOException::class)
    private fun writeObject(stream: ObjectOutputStream) {
        stream.defaultWriteObject()
    }

    private fun stamp(dateMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateMillis
        return "${Calendar.HOUR_OF_DAY}:${Calendar.MINUTE}:${Calendar.SECOND}"
    }

    override fun toString(): String {
        return "eventCount:$eventCount sessionCount:$sessionCount subsessionCount:${subsessionCount} " +
                "sessionLength:${sessionLength / 1000.0} timeSpent:${timeSpent / 1000.0} " +
                "lastActivity:${stamp(lastActivity)} uuid:$uuid"
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(stream: ObjectInputStream) {
        val fields = stream.readFields()
        eventCount = readField(fields, "eventCount", 0) as Int
        sessionCount = readField(fields, "sessionCount", 0) as Int
        subsessionCount = readField(fields, "subsessionCount", -1) as Int
        sessionLength = readField(fields, "sessionLength", -1L) as Long
        timeSpent = readField(fields, "timeSpent", -1L) as Long
        lastActivity = readField(fields, "lastActivity", -1L) as Long
        lastInterval = readField(fields, "lastInterval", -1L) as Long

        // new fields
        uuid = readField(fields, "uuid", null) as String?
        enabled = readField(fields, "enabled", true) as Boolean
        isGdprForgotten = readField(fields, "isGdprForgotten", false) as Boolean
        isThirdPartySharingDisabled =
            readField(fields, "isThirdPartySharingDisabled", false) as Boolean
        askingAttribution = readField(fields, "askingAttribution", false) as Boolean
        updatePackages = readField(fields, "updatePackages", false) as Boolean
        orderIds = readField(fields, "orderIds", null) as LinkedList<String>?
        pushToken = readField(fields, "pushToken", null) as String?
        adid = readField(fields, "adid", null) as String?
        clickTime = readField(fields, "clickTime", -1L) as Long
        installBegin = readField(fields, "installBegin", -1L) as Long
        installReferrer = readField(fields, "installReferrer", null) as String?
        googlePlayInstant = readField(fields, "googlePlayInstant", null) as Boolean?
        clickTimeServer = readField(fields, "clickTimeServer", -1L) as Long
        installBeginServer = readField(fields, "installBeginServer", -1L) as Long
        installVersion = readField(fields, "installVersion", null) as String?
        clickTimeHuawei = readField(fields, "clickTimeHuawei", -1L) as Long
        installBeginHuawei = readField(fields, "installBeginHuawei", -1L) as Long
        installReferrerHuawei = readField(fields, "installReferrerHuawei", null) as String?
        installReferrerHuaweiAppGallery =
            readField(fields, "installReferrerHuaweiAppGallery", null) as String?

        // create UUID for migrating devices
        if (uuid == null) {
            uuid = Util.createUuid()
        }
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


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ActivityState

        if (logger != other.logger) return false
        if (uuid != other.uuid) return false
        if (enabled != other.enabled) return false
        if (isGdprForgotten != other.isGdprForgotten) return false
        if (isThirdPartySharingDisabled != other.isThirdPartySharingDisabled) return false
        if (askingAttribution != other.askingAttribution) return false
        if (eventCount != other.eventCount) return false
        if (sessionCount != other.sessionCount) return false
        if (subsessionCount != other.subsessionCount) return false
        if (sessionLength != other.sessionLength) return false
        if (timeSpent != other.timeSpent) return false
        if (lastActivity != other.lastActivity) return false
        if (lastInterval != other.lastInterval) return false
        if (updatePackages != other.updatePackages) return false
        if (orderIds != other.orderIds) return false
        if (pushToken != other.pushToken) return false
        if (adid != other.adid) return false
        if (clickTime != other.clickTime) return false
        if (installBegin != other.installBegin) return false
        if (installReferrer != other.installReferrer) return false
        if (googlePlayInstant != other.googlePlayInstant) return false
        if (clickTimeServer != other.clickTimeServer) return false
        if (installBeginServer != other.installBeginServer) return false
        if (installVersion != other.installVersion) return false
        if (clickTimeHuawei != other.clickTimeHuawei) return false
        if (installBeginHuawei != other.installBeginHuawei) return false
        if (installReferrerHuawei != other.installReferrerHuawei) return false
        if (installReferrerHuaweiAppGallery != other.installReferrerHuaweiAppGallery) return false

        return true
    }

    override fun hashCode(): Int {
        var result = logger.hashCode()
        result = 31 * result + (uuid?.hashCode() ?: 0)
        result = 31 * result + enabled.hashCode()
        result = 31 * result + isGdprForgotten.hashCode()
        result = 31 * result + isThirdPartySharingDisabled.hashCode()
        result = 31 * result + askingAttribution.hashCode()
        result = 31 * result + eventCount
        result = 31 * result + sessionCount
        result = 31 * result + subsessionCount
        result = 31 * result + sessionLength.hashCode()
        result = 31 * result + timeSpent.hashCode()
        result = 31 * result + lastActivity.hashCode()
        result = 31 * result + lastInterval.hashCode()
        result = 31 * result + updatePackages.hashCode()
        result = 31 * result + (orderIds?.hashCode() ?: 0)
        result = 31 * result + (pushToken?.hashCode() ?: 0)
        result = 31 * result + (adid?.hashCode() ?: 0)
        result = 31 * result + clickTime.hashCode()
        result = 31 * result + installBegin.hashCode()
        result = 31 * result + (installReferrer?.hashCode() ?: 0)
        result = 31 * result + (googlePlayInstant?.hashCode() ?: 0)
        result = 31 * result + clickTimeServer.hashCode()
        result = 31 * result + installBeginServer.hashCode()
        result = 31 * result + (installVersion?.hashCode() ?: 0)
        result = 31 * result + clickTimeHuawei.hashCode()
        result = 31 * result + installBeginHuawei.hashCode()
        result = 31 * result + (installReferrerHuawei?.hashCode() ?: 0)
        result = 31 * result + (installReferrerHuaweiAppGallery?.hashCode() ?: 0)
        return result
    }


}