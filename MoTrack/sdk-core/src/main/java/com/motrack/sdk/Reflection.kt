package com.motrack.sdk

import android.content.Context

/**
 * @author yaya (@yahyalmh)
 * @since 09th October 2021
 */

class Reflection {
    companion object {
        @Throws(Exception::class)
        fun getAdvertisingInfoObject(context: Context?): Any? {
            return invokeStaticMethod(
                "com.google.android.gms.ads.identifier.AdvertisingIdClient",
                "getAdvertisingIdInfo",
                arrayOf(
                    Context::class.java
                ),
                context
            )
        }

        fun getImeiParameters(context: Context?, logger: ILogger?): Map<String, String>? {
            try {
                val nonPlayParameters = invokeStaticMethod(
                    "com.motrack.sdk.imei.Util", "getImeiParameters", arrayOf(
                        Context::class.java,
                        ILogger::class.java
                    ), context, logger
                )
                val stringStringMapClass = MutableMap::class.java as Class<Map<String, String>>
                if (nonPlayParameters != null && stringStringMapClass.isInstance(nonPlayParameters)) {
                    return nonPlayParameters as Map<String, String>?
                }
            } catch (e: Exception) {
            }
            return null
        }

        fun getOaidParameters(context: Context?, logger: ILogger?): Map<String, String>? {
            try {
                val oaidParameters = invokeStaticMethod(
                    "com.motrack.sdk.oaid.Util", "getOaidParameters", arrayOf(
                        Context::class.java,
                        ILogger::class.java
                    ), context, logger
                )
                val stringStringMapClass = MutableMap::class.java as Class<Map<String, String>>
                if (oaidParameters != null && stringStringMapClass.isInstance(oaidParameters)) {
                    return oaidParameters as Map<String, String>
                }
            } catch (e: java.lang.Exception) {
            }
            return null
        }

        fun getPlayAdId(context: Context, AdvertisingInfoObject: Any): String? {
            return try {
                val playAdid = invokeInstanceMethod(
                    AdvertisingInfoObject,
                    "getId",
                    null
                )
                playAdid as String
            } catch (t: Throwable) {
                null
            }
        }

        fun isPlayTrackingEnabled(context: Context?, AdvertisingInfoObject: Any): Boolean? {
            return try {
                val isLimitedTrackingEnabled =
                    invokeInstanceMethod(
                        AdvertisingInfoObject,
                        "isLimitAdTrackingEnabled",
                        null
                    ) as Boolean
                return if (isLimitedTrackingEnabled == null) null else !isLimitedTrackingEnabled
            } catch (t: Throwable) {
                null
            }
        }

        fun forName(className: String): Class<*>? {
            return try {
                Class.forName(className)
            } catch (t: Throwable) {
                null
            }
        }

        fun createDefaultInstance(className: String): Any? {
            val classObject: Class<*> = forName(className)
                ?: return null
            return createDefaultInstance(classObject)
        }

        fun createDefaultInstance(classObject: Class<*>): Any? {
            return try {
                classObject.newInstance()
            } catch (t: Throwable) {
                null
            }
        }

        fun createInstance(className: String, cArgs: Array<Class<*>?>, vararg args: Any?): Any? {
            return try {
                val classObject = Class.forName(className)
                val constructor =
                    classObject.getConstructor(*cArgs)
                constructor.newInstance(*args)
            } catch (t: Throwable) {
                null
            }
        }


        @Throws(Exception::class)
        fun invokeStaticMethod(
            className: String,
            methodName: String,
            cArgs: Array<Class<*>>,
            vararg args: Any?
        ): Any? {
            val classObject = Class.forName(className)
            return invokeMethod(classObject, methodName, null, cArgs, *args)
        }

        @Throws(Exception::class)
        fun invokeInstanceMethod(
            instance: Any,
            methodName: String,
            cArgs: Array<Class<*>>?,
            vararg args: Any?
        ): Any? {
            val classObject: Class<*> = instance.javaClass
            return invokeMethod(classObject, methodName, instance, cArgs, *args)
        }

        @Throws(Exception::class)
        fun invokeMethod(
            classObject: Class<*>,
            methodName: String,
            instance: Any?,
            cArgs: Array<Class<*>>?,
            vararg args: Any?
        ): Any? {

            val methodObject = if (cArgs != null) {
                classObject.getMethod(methodName, *cArgs)
            } else {
                classObject.getMethod(methodName)
            }

            return methodObject.invoke(instance, *args)
        }

        @Throws(Exception::class)
        fun readField(className: String, fieldName: String): Any? {
            return readField(className, fieldName, null)
        }

        @Throws(Exception::class)
        fun readField(className: String, fieldName: String, instance: Any?): Any? {
            val classObject: Class<*> = forName(className) ?: return null
            val fieldObject = classObject.getField(fieldName) ?: return null
            return fieldObject[instance]
        }
    }
}