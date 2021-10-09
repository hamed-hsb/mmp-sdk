package com.motrack.sdk

import android.annotation.SuppressLint
import android.content.Context
import com.motrack.sdk.Constants.Companion.ONE_SECOND
import com.motrack.sdk.scheduler.SingleThreadCachedScheduler
import com.motrack.sdk.scheduler.ThreadExecutor
import com.motrack.sdk.scheduler.TimerOnce
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean

class InstallReferrer
    (
    private val context: Context,
    /**
     * Weak reference to ActivityHandler instance.
     */
    private val referrerCallback: InstallReferrerReadListener
) : InvocationHandler {

    companion object {
        /**
         * Android install referrer library package name.
         */
        private const val PACKAGE_BASE_NAME = "com.android.installreferrer."

        /**
         * Play Store service is not connected now - potentially transient state.
         */
        private const val STATUS_SERVICE_DISCONNECTED = -1

        /**
         * Play Store service connection success.
         */
        private const val STATUS_OK = 0

        /**
         * Could not initiate connection to the install referrer service.
         */
        private const val STATUS_SERVICE_UNAVAILABLE = 1

        /**
         * Install Referrer API not supported by the installed Play Store app.
         */
        private const val STATUS_FEATURE_NOT_SUPPORTED = 2

        /**
         * General errors caused by incorrect usage.
         */
        private const val STATUS_DEVELOPER_ERROR = 3
    }

    /**
     * Retry time interval.
     */
    private val retryWaitTime: Int = (ONE_SECOND * 3).toInt()

    /**
     * Number of retries attempted to connect to service.
     */
    private var retries = 0

    /**
     * Boolean indicating whether service should be tried to read.
     * Either because it has not yet tried,
     * or it did and it was successful
     * or it did, was not successful, but it should not retry
     */
    private var shouldTryToRead: AtomicBoolean? = null

    /**
     * Adjust logger instance.
     */
    private var logger: ILogger? = null

    /**
     * InstallReferrer class instance.
     */
    private var referrerClient: Any? = null

    /**
     * Timer which fires retry attempts.
     */
    private var retryTimer: TimerOnce? = null

    private var playInstallReferrer: Any? = null

    private var executor: ThreadExecutor? = null

    init {
        logger = MotrackFactory.getLogger()
        playInstallReferrer = createInstallReferrer(context, referrerCallback, logger!!)
        shouldTryToRead = AtomicBoolean(true)
        retries = 0
        retryTimer = TimerOnce({ startConnection() }, "InstallReferrer")
        executor = SingleThreadCachedScheduler("InstallReferrer")
    }

    private fun createInstallReferrer(
        context: Context,
        referrerCallback: InstallReferrerReadListener,
        logger: ILogger
    ): Any? {
        return Reflection.createInstance(
            "com.motrack.sdk.play.InstallReferrer", arrayOf(
                Context::class.java,
                InstallReferrerReadListener::class.java,
                ILogger::class.java
            ),
            context, referrerCallback, logger
        )
    }

    /**
     * Start connection with install referrer service.
     */
    fun startConnection() {
        if (playInstallReferrer != null) {
            try {
                Reflection.invokeInstanceMethod(playInstallReferrer!!, "startConnection", null)
                return
            } catch (e: Exception) {
                logger!!.error("Call to Play startConnection error: ${e.message!!}")
            }
        }
        if (!MotrackFactory.getTryInstallReferrer()) {
            return
        }
        closeReferrerClient()
        if (!shouldTryToRead!!.get()) {
            logger!!.debug("Should not try to read Install referrer")
            return
        }
        referrerClient = createInstallReferrerClient(context)
        if (referrerClient == null) {
            return
        }
        val listenerClass = getInstallReferrerStateListenerClass() ?: return
        val listenerProxy: Any = createProxyInstallReferrerStateListener(listenerClass) ?: return
        startConnection(listenerClass, listenerProxy)
    }

    /**
     * Get object instance for given class (InstallReferrerStateListener in this case).
     *
     * @param installReferrerStateListenerClass Class object
     * @return Instance of Class type object.
     */
    private fun createProxyInstallReferrerStateListener(installReferrerStateListenerClass: Class<*>): Any? {
        var proxyInstance: Any? = null
        try {
            proxyInstance = Proxy.newProxyInstance(
                installReferrerStateListenerClass.classLoader,
                arrayOf(installReferrerStateListenerClass),
                this
            )
        } catch (ex: IllegalArgumentException) {
            logger!!.error("InstallReferrer proxy violating parameter restrictions")
        } catch (ex: NullPointerException) {
            logger!!.error("Null argument passed to InstallReferrer proxy")
        }
        return proxyInstance
    }

    /**
     * Initialise connection with install referrer service.
     *
     * @param listenerClass Callback listener class type
     * @param listenerProxy Callback listener object instance
     */
    private fun startConnection(listenerClass: Class<*>, listenerProxy: Any) {
        try {
            Reflection.invokeInstanceMethod(
                referrerClient!!,
                "startConnection",
                arrayOf(listenerClass),
                listenerProxy
            )
        } catch (ex: InvocationTargetException) {
            // Check for an underlying root cause in the stack trace
            if (Util.hasRootCause(ex)) {
                Util.getRootCause(ex)?.let {
                    logger!!.error("InstallReferrer encountered an InvocationTargetException $it")
                }
            }
        } catch (ex: Exception) {
            logger!!.error("startConnection error (${ex.message!!}) thrown by (${ex.javaClass.canonicalName})")
        }
    }


    /**
     * Create InstallReferrerClient object instance.
     *
     * @param context App context
     * @return Instance of InstallReferrerClient. Defaults to null if failed to create one.
     */
    private fun createInstallReferrerClient(context: Context): Any? {
        try {
            val builder: Any? = Reflection.invokeStaticMethod(
                PACKAGE_BASE_NAME + "api.InstallReferrerClient",
                "newBuilder",
                arrayOf(Context::class.java), context
            )
            return builder?.let { Reflection.invokeInstanceMethod(it, "build", null) }
        } catch (ex: ClassNotFoundException) {
            logger!!.warn("InstallReferrer not integrated in project (${ex.message!!}) thrown by (${ex.javaClass.canonicalName})")
        } catch (ex: Exception) {
            logger!!.error("createInstallReferrerClient error (${ex.message!!}) from (${ex.javaClass.canonicalName})")
        }
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Throws(Throwable::class)
    override fun invoke(proxy: Any?, method: Method?, args: Array<Any?>?): Any? {
        executor!!.submit {
            try {
                invokeI(proxy!!, method, args!!)
            } catch (throwable: Throwable) {
                logger!!.error("invoke error (${throwable.message!!}) thrown by (${throwable.javaClass.canonicalName})")
            }
        }
        return null
    }

    @Throws(Throwable::class)
    private fun invokeI(proxy: Any, method: Method?, args: Array<Any?>): Any? {
        var args: Array<Any?>? = args
        if (method == null) {
            logger!!.error("InstallReferrer invoke method null")
            return null
        }
        val methodName = method.name
        if (methodName.isNullOrEmpty()) {
            logger!!.error("InstallReferrer invoke method name null")
            return null
        }
        // Prints the method being invoked
        logger!!.debug("InstallReferrer invoke method name: $methodName")
        if (args == null) {
            logger!!.warn("InstallReferrer invoke args null")
            args = arrayOfNulls(0)
        }
        for (arg in args) {
            logger!!.debug("InstallReferrer invoke arg: $arg!!")
        }

        // if the method name equals some method's name then call your method
        if (methodName == "onInstallReferrerSetupFinished") {
            if (args.size != 1) {
                logger!!.error("InstallReferrer invoke onInstallReferrerSetupFinished args length not 1: ${args.size}")
                return null
            }
            val arg = args[0]
            if (arg !is Int) {
                logger!!.error("InstallReferrer invoke onInstallReferrerSetupFinished arg not int")
                return null
            }
            val responseCode = arg as Int?
            if (responseCode == null) {
                logger!!.error("InstallReferrer invoke onInstallReferrerSetupFinished responseCode arg is null")
                return null
            }
            onInstallReferrerSetupFinishedIntI(responseCode)
        } else if (methodName == "onInstallReferrerServiceDisconnected") {
            logger!!.debug("Connection to install referrer service was lost. Retrying ...")
            retryI()
        }
        return null
    }

    /**
     * Get InstallReferrerStateListener class object.
     *
     * @return Class object for InstallReferrerStateListener class.
     */
    @SuppressLint("PrivateApi")
    private fun getInstallReferrerStateListenerClass(): Class<*>? {
        try {
            return Class.forName(PACKAGE_BASE_NAME + "api.InstallReferrerStateListener")
        } catch (ex: Exception) {
            logger!!.error("getInstallReferrerStateListenerClass error (${ex.message!!}) from (${ex.javaClass.canonicalName})")
        }
        return null
    }

    /**
     * Check and process response from install referrer service.
     *
     * @param responseCode Response code from install referrer service
     */
    private fun onInstallReferrerSetupFinishedIntI(responseCode: Int) {
        var retryAtEnd = false
        when (responseCode) {
            STATUS_OK ->
                // Connection established
                try {
                    // Extract referrer
                    val referrerDetails = getInstallReferrer()
                    val installReferrer = getStringInstallReferrer(referrerDetails)
                    val clickTime = getReferrerClickTimestampSeconds(referrerDetails)
                    val installBegin = getInstallBeginTimestampSeconds(referrerDetails)
                    logger!!.debug("installReferrer: ${installReferrer!!}, clickTime: $clickTime, installBeginTime: $installBegin")

                    val installVersion = getStringInstallVersion(referrerDetails)
                    val clickTimeServer = getReferrerClickTimestampServerSeconds(referrerDetails)
                    val installBeginServer = getInstallBeginTimestampServerSeconds(referrerDetails)
                    val googlePlayInstant = getBooleanGooglePlayInstantParam(referrerDetails)
                    logger!!.debug(
                        "installVersion: ${installVersion!!}, clickTimeServer: ${clickTimeServer}, " +
                                "installBeginServer: ${installBeginServer}, googlePlayInstant: ${googlePlayInstant!!}"
                    )
                    logger!!.debug("Install Referrer read successfully. Closing connection")
                    val installReferrerDetails = ReferrerDetails(
                        installReferrer,
                        clickTime, installBegin, clickTimeServer, installBeginServer,
                        installVersion, googlePlayInstant
                    )

                    // Stuff successfully read, try to send it.
                    referrerCallback.onInstallReferrerRead(installReferrerDetails)
                } catch (e: java.lang.Exception) {
                    logger!!.warn(
                        "Couldn't get install referrer from client (%s). Retrying...",
                        e.message!!
                    )
                    retryAtEnd = true
                }
            STATUS_FEATURE_NOT_SUPPORTED ->
                // API not available on the current Play Store app
                logger!!.debug("Install Referrer API not supported by the installed Play Store app. Closing connection")

            STATUS_SERVICE_UNAVAILABLE -> {
                // Connection could not be established
                logger!!.debug("Could not initiate connection to the Install Referrer service. Retrying...")
                retryAtEnd = true
            }

            STATUS_SERVICE_DISCONNECTED -> {
                // Play Store service is not connected now - potentially transient state
                logger!!.debug("Play Store service is not connected now. Retrying...")
                retryAtEnd = true
            }

            STATUS_DEVELOPER_ERROR -> {
                logger!!.debug("Install Referrer API general errors caused by incorrect usage. Retrying...")
                retryAtEnd = true
            }

            else -> logger!!.debug("Unexpected response code of install referrer response: $responseCode. Closing connection")
        }
        if (retryAtEnd) {
            retryI()
        } else {
            shouldTryToRead!!.set(false)
            closeReferrerClient()
        }
    }

    /**
     * Get ReferrerDetails object (response).
     *
     * @return ReferrerDetails object
     */
    private fun getInstallReferrer(): Any? {
        if (referrerClient == null) {
            return null
        }
        try {
            return Reflection.invokeInstanceMethod(
                referrerClient!!, "getInstallReferrer", null
            )
        } catch (e: java.lang.Exception) {
            logger!!.error("getInstallReferrer error (${e.message!!}) thrown by (${e.javaClass.canonicalName})")
        }
        return null
    }

    /**
     * Get install referrer string value.
     *
     * @param referrerDetails ReferrerDetails object
     * @return Install referrer string value.
     */
    private fun getStringInstallReferrer(referrerDetails: Any?): String? {
        if (referrerDetails == null) {
            return null
        }
        try {
            return Reflection.invokeInstanceMethod(
                referrerDetails, "getInstallReferrer", null
            ) as String?
        } catch (e: java.lang.Exception) {
            logger!!.error("getStringInstallReferrer error (${e.message!!}) thrown by (${e.javaClass.canonicalName})")
        }
        return null
    }

    /**
     * Get redirect URL click timestamp.
     *
     * @param referrerDetails ReferrerDetails object
     * @return Redirect URL click timestamp.
     */
    private fun getReferrerClickTimestampSeconds(referrerDetails: Any?): Long {
        if (referrerDetails == null) {
            return -1
        }
        try {
            return Reflection.invokeInstanceMethod(
                referrerDetails, "getReferrerClickTimestampSeconds", null
            ) as Long
        } catch (e: java.lang.Exception) {
            logger!!.error("getReferrerClickTimestampSeconds error (${e.message!!}) thrown by (${e.javaClass.canonicalName})")
        }
        return -1
    }

    /**
     * Get Play Store app INSTALL button click timestamp.
     *
     * @param referrerDetails ReferrerDetails object
     * @return Play Store app INSTALL button click timestamp.
     */
    private fun getInstallBeginTimestampSeconds(referrerDetails: Any?): Long {
        if (referrerDetails == null) {
            return -1
        }
        try {
            return Reflection.invokeInstanceMethod(
                referrerDetails, "getInstallBeginTimestampSeconds", null
            ) as Long
        } catch (e: java.lang.Exception) {
            logger!!.error("getInstallBeginTimestampSeconds error (%${e.message!!}) thrown by (${e.javaClass.canonicalName})")
        }
        return -1
    }

    /**
     * Get install version string value.
     *
     * @param referrerDetails ReferrerDetails object
     * @return Install version string value.
     */
    private fun getStringInstallVersion(referrerDetails: Any?): String? {
        if (referrerDetails == null) {
            return null
        }
        try {
            return Reflection.invokeInstanceMethod(
                referrerDetails, "getInstallVersion", null
            ) as String?
        } catch (e: java.lang.Exception) {
            // not logging the error as this is expected to happen below v2.0
        }
        return null
    }

    /**
     * Get redirect URL click timestamp server.
     *
     * @param referrerDetails ReferrerDetails object
     * @return Redirect URL click timestamp server.
     */
    private fun getReferrerClickTimestampServerSeconds(referrerDetails: Any?): Long {
        if (referrerDetails == null) {
            return -1
        }
        try {
            return Reflection.invokeInstanceMethod(
                referrerDetails, "getReferrerClickTimestampServerSeconds", null
            ) as Long
        } catch (e: java.lang.Exception) {
            // not logging the error as this is expected to happen below v2.0
        }
        return -1
    }

    /**
     * Get Play Store app INSTALL button click timestamp server.
     *
     * @param referrerDetails ReferrerDetails object
     * @return Play Store app INSTALL button click timestamp server.
     */
    private fun getInstallBeginTimestampServerSeconds(referrerDetails: Any?): Long {
        if (referrerDetails == null) {
            return -1
        }
        try {
            return Reflection.invokeInstanceMethod(
                referrerDetails, "getInstallBeginTimestampServerSeconds", null
            ) as Long
        } catch (e: java.lang.Exception) {
            // not logging the error as this is expected to happen below v2.0
        }
        return -1
    }

    /**
     * Get google play instant boolean value.
     *
     * @param referrerDetails ReferrerDetails object
     * @return Google play instant boolean value.
     */
    private fun getBooleanGooglePlayInstantParam(referrerDetails: Any?): Boolean? {
        if (referrerDetails == null) {
            return null
        }

        try {
            return Reflection.invokeInstanceMethod(
                referrerDetails, "getGooglePlayInstantParam", null
            ) as Boolean
        } catch (e: java.lang.Exception) {
            // not logging the error as this is expected to happen below v2.0
        }
        return null
    }

    /**
     * Retry connection to install referrer service.
     */
    private fun retryI() {
        if (!shouldTryToRead!!.get()) {
            logger!!.debug("Should not try to read Install referrer")
            closeReferrerClient()
            return
        }
        // Check increase retry counter
        if (retries + 1 > Constants.MAX_INSTALL_REFERRER_RETRIES) {
            logger!!.debug("Limit number of retry of ${Constants.MAX_INSTALL_REFERRER_RETRIES} for install referrer surpassed")
            return
        }
        val firingIn = retryTimer!!.getFireIn()
        if (firingIn > 0) {
            logger!!.debug("Already waiting to retry to read install referrer in $firingIn milliseconds")
            return
        }
        retries++
        logger!!.debug("Retry number $retries to connect to install referrer API")
        retryTimer!!.startIn(retryWaitTime.toLong())
    }

    /**
     * Terminate connection to install referrer service.
     */
    private fun closeReferrerClient() {
        if (referrerClient == null) {
            return
        }
        try {
            Reflection.invokeInstanceMethod(referrerClient!!, "endConnection", null)
            logger!!.debug("Install Referrer API connection closed")
        } catch (e: java.lang.Exception) {
            logger!!.error("closeReferrerClient error (${e.message!!}) thrown by (${e.javaClass.canonicalName})")
        }
        referrerClient = null
    }

}
