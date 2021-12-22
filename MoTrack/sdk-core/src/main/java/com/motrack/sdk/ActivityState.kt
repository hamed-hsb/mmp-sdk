package com.motrack.sdk

import java.io.*
import java.util.*

class ActivityState : Serializable, Cloneable {

    @Transient
    var logger: ILogger = MotrackFactory.getLogger()

    constructor() {
        // create UUID for new devices
        uuid = Util.createUuid()
        enabled = true
        isGdprForgotten = false
        isThirdPartySharingDisabled = false
        askingAttribution = false
        eventCount = 0 // no events yet
        sessionCount = 0 // the first session just started
        subsessionCount = -1 // we don't know how many subsessions this first  session will have
        sessionLength = -1 // same for session length and time spent
        timeSpent = -1 // this information will be collected and attached to the next session
        lastActivity = -1
        lastInterval = -1
        updatePackages = false
        orderIds = null
        pushToken = null
        adid = null
        clickTime = 0
        installBegin = 0
        installReferrer = null
        googlePlayInstant = false
        clickTimeServer = 0
        installBeginServer = 0
        installVersion = null
        clickTimeHuawei = 0
        installBeginHuawei = 0
        installReferrerHuawei = null
        installReferrerHuaweiAppGallery = null
    }




    // persistent data
    var uuid: String? = null
    var enabled: Boolean = true
    var isGdprForgotten: Boolean = false
    var isThirdPartySharingDisabled: Boolean = false
    var askingAttribution: Boolean = false

    // global counters
    var eventCount: Int = 0
    var sessionCount: Int = 0

    // session attributes
    var subsessionCount: Int = -1
    var sessionLength: Long = -1 // all durations in milliseconds
    var timeSpent: Long = -1
    var lastActivity: Long = -1 // all times in milliseconds since 1970


    var lastInterval: Long = -1

    var updatePackages: Boolean = false

    var orderIds: LinkedList<String>? = null

    var pushToken: String? = null
    var adid: String? = null

    var clickTime: Long = 0
    var installBegin: Long = 0
    var installReferrer: String? = null
    var googlePlayInstant: Boolean = false
    var clickTimeServer: Long = 0
    var installBeginServer: Long = 0
    var installVersion: String? = null

    var clickTimeHuawei: Long = 0
    var installBeginHuawei: Long = 0
    var installReferrerHuawei: String? = null
    var installReferrerHuaweiAppGallery: String? = null


    fun resetSessionAttributes(now: Long) {
        subsessionCount = 1 // first subsession
        sessionLength = 0 // no session length yet
        timeSpent = 0 // no time spent yet
        lastActivity = now
        lastInterval = -1
    }

    fun addOrderId(orderId: String) {
        if (orderIds == null) {
            orderIds = LinkedList()
        }
        if (orderIds!!.size >= ORDER_ID_MAXCOUNT) {
            orderIds!!.removeLast()
        }
        orderIds!!.addFirst(orderId)
    }

    fun findOrderId(orderId: String): Boolean {
        return if (orderIds == null) {
            false
        } else orderIds!!.contains(orderId)
    }

    override fun toString(): String {
        return Util.formatString(
            "ec:%d sc:%d ssc:%d sl:%.1f ts:%.1f la:%s uuid:%s",
            eventCount, sessionCount, subsessionCount,
            sessionLength / 1000.0, timeSpent / 1000.0,
            stamp(lastActivity), uuid
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        val otherActivityState = other as ActivityState
        if (!Util.equalString(uuid, otherActivityState.uuid)) return false
        if (!Util.equalBoolean(enabled, otherActivityState.enabled)) return false
        if (!Util.equalBoolean(isGdprForgotten, otherActivityState.isGdprForgotten)) return false
        if (!Util.equalBoolean(
                isThirdPartySharingDisabled,
                otherActivityState.isThirdPartySharingDisabled
            )
        ) return false
        if (!Util.equalBoolean(
                askingAttribution,
                otherActivityState.askingAttribution
            )
        ) return false
        if (!Util.equalInt(eventCount, otherActivityState.eventCount)) return false
        if (!Util.equalInt(sessionCount, otherActivityState.sessionCount)) return false
        if (!Util.equalInt(subsessionCount, otherActivityState.subsessionCount)) return false
        if (!Util.equalLong(sessionLength, otherActivityState.sessionLength)) return false
        if (!Util.equalLong(timeSpent, otherActivityState.timeSpent)) return false
        if (!Util.equalLong(lastInterval, otherActivityState.lastInterval)) return false
        if (!Util.equalBoolean(updatePackages, otherActivityState.updatePackages)) return false
        if (!Util.equalObject(orderIds, otherActivityState.orderIds)) return false
        if (!Util.equalString(pushToken, otherActivityState.pushToken)) return false
        if (!Util.equalString(adid, otherActivityState.adid)) return false
        if (!Util.equalLong(clickTime, otherActivityState.clickTime)) return false
        if (!Util.equalLong(installBegin, otherActivityState.installBegin)) return false
        if (!Util.equalString(installReferrer, otherActivityState.installReferrer)) return false
        if (!Util.equalBoolean(
                googlePlayInstant,
                otherActivityState.googlePlayInstant
            )
        ) return false
        if (!Util.equalLong(clickTimeServer, otherActivityState.clickTimeServer)) return false
        if (!Util.equalLong(installBeginServer, otherActivityState.installBeginServer)) return false
        if (!Util.equalString(installVersion, otherActivityState.installVersion)) return false
        if (!Util.equalLong(clickTimeHuawei, otherActivityState.clickTimeHuawei)) return false
        if (!Util.equalLong(installBeginHuawei, otherActivityState.installBeginHuawei)) return false
        if (!Util.equalString(
                installReferrerHuawei,
                otherActivityState.installReferrerHuawei
            )
        ) return false
        return Util.equalString(
            installReferrerHuaweiAppGallery,
            otherActivityState.installReferrerHuaweiAppGallery
        )
    }

    override fun hashCode(): Int {
        var hashCode = 17
        hashCode = 37 * hashCode + Util.hashString(uuid)
        hashCode = 37 * hashCode + Util.hashBoolean(enabled)
        hashCode = 37 * hashCode + Util.hashBoolean(isGdprForgotten)
        hashCode = 37 * hashCode + Util.hashBoolean(isThirdPartySharingDisabled)
        hashCode = 37 * hashCode + Util.hashBoolean(askingAttribution)
        hashCode = 37 * hashCode + eventCount
        hashCode = 37 * hashCode + sessionCount
        hashCode = 37 * hashCode + subsessionCount
        hashCode = 37 * hashCode + Util.hashLong(sessionLength)
        hashCode = 37 * hashCode + Util.hashLong(timeSpent)
        hashCode = 37 * hashCode + Util.hashLong(lastInterval)
        hashCode = 37 * hashCode + Util.hashBoolean(updatePackages)
        hashCode = 37 * hashCode + Util.hashObject(orderIds)
        hashCode = 37 * hashCode + Util.hashString(pushToken)
        hashCode = 37 * hashCode + Util.hashString(adid)
        hashCode = 37 * hashCode + Util.hashLong(clickTime)
        hashCode = 37 * hashCode + Util.hashLong(installBegin)
        hashCode = 37 * hashCode + Util.hashString(installReferrer)
        hashCode = 37 * hashCode + Util.hashBoolean(googlePlayInstant)
        hashCode = 37 * hashCode + Util.hashLong(clickTimeServer)
        hashCode = 37 * hashCode + Util.hashLong(installBeginServer)
        hashCode = 37 * hashCode + Util.hashString(installVersion)
        hashCode = 37 * hashCode + Util.hashLong(clickTimeHuawei)
        hashCode = 37 * hashCode + Util.hashLong(installBeginHuawei)
        hashCode = 37 * hashCode + Util.hashString(installReferrerHuawei)
        hashCode = 37 * hashCode + Util.hashString(installReferrerHuaweiAppGallery)
        return hashCode
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(stream: ObjectInputStream) {
        val fields = stream.readFields()
        eventCount = Util.readIntField(fields, "eventCount", 0)
        sessionCount = Util.readIntField(fields, "sessionCount", 0)
        subsessionCount = Util.readIntField(fields, "subsessionCount", -1)
        sessionLength = Util.readLongField(fields, "sessionLength", -1L)
        timeSpent = Util.readLongField(fields, "timeSpent", -1L)
        lastActivity = Util.readLongField(fields, "lastActivity", -1L)
        lastInterval = Util.readLongField(fields, "lastInterval", -1L)

        // new fields
        uuid = Util.readStringField(fields, "uuid", null)
        enabled = Util.readBooleanField(fields, "enabled", true)
        isGdprForgotten = Util.readBooleanField(fields, "isGdprForgotten", false)
        isThirdPartySharingDisabled =
            Util.readBooleanField(fields, "isThirdPartySharingDisabled", false)
        askingAttribution = Util.readBooleanField(fields, "askingAttribution", false)
        updatePackages = Util.readBooleanField(fields, "updatePackages", false)
        orderIds = Util.readObjectField(fields, "orderIds", null)
        pushToken = Util.readStringField(fields, "pushToken", null)
        adid = Util.readStringField(fields, "adid", null)
        clickTime = Util.readLongField(fields, "clickTime", -1L)
        installBegin = Util.readLongField(fields, "installBegin", -1L)
        installReferrer = Util.readStringField(fields, "installReferrer", null)
        googlePlayInstant = Util.readObjectField(fields, "googlePlayInstant", false)
        clickTimeServer = Util.readLongField(fields, "clickTimeServer", -1L)
        installBeginServer = Util.readLongField(fields, "installBeginServer", -1L)
        installVersion = Util.readStringField(fields, "installVersion", null)
        clickTimeHuawei = Util.readLongField(fields, "clickTimeHuawei", -1L)
        installBeginHuawei = Util.readLongField(fields, "installBeginHuawei", -1L)
        installReferrerHuawei = Util.readStringField(fields, "installReferrerHuawei", null)
        installReferrerHuaweiAppGallery =
            Util.readStringField(fields, "installReferrerHuaweiAppGallery", null)

        // create UUID for migrating devices
        if (uuid == null) {
            uuid = Util.createUuid()
        }
    }

    @Throws(IOException::class)
    private fun writeObject(stream: ObjectOutputStream) {
        try {
            stream.defaultWriteObject()
        } catch (e: Exception) {
           logger.error("Error when serializing ActivityState: ${e.message}")
        }
    }

    companion object {
        private const val serialVersionUID: Long = 9039439291143138148L
        private const val ORDER_ID_MAXCOUNT = 10
        private final val serialPersistentFields = arrayOf(
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
                LinkedList::class.java as Class<LinkedList<String>?>
            ),
            ObjectStreamField("pushToken", String::class.java),
            ObjectStreamField("adid", String::class.java),
            ObjectStreamField("clickTime", Long::class.javaPrimitiveType),
            ObjectStreamField("installBegin", Long::class.javaPrimitiveType),
            ObjectStreamField("installReferrer", String::class.java),
            ObjectStreamField("googlePlayInstant", Boolean::class.javaPrimitiveType),
            ObjectStreamField("clickTimeServer", Long::class.javaPrimitiveType),
            ObjectStreamField("installBeginServer", Long::class.javaPrimitiveType),
            ObjectStreamField("installVersion", String::class.java),
            ObjectStreamField("clickTimeHuawei", Long::class.javaPrimitiveType),
            ObjectStreamField("installBeginHuawei", Long::class.javaPrimitiveType),
            ObjectStreamField("installReferrerHuawei", String::class.java),
            ObjectStreamField("installReferrerHuaweiAppGallery", String::class.java),
        )

        private fun stamp(dateMillis: Long): String {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = dateMillis
            return Util.formatString(
                "%02d:%02d:%02d",
                Calendar.HOUR_OF_DAY,
                Calendar.MINUTE,
                Calendar.SECOND
            )
        }
    }
}