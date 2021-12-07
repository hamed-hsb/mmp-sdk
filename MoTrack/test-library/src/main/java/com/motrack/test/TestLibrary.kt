package com.motrack.test

/**
 * @author yaya (@yahyalmh)
 * @since 14th November 2021
 */
import android.os.SystemClock
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.motrack.test.Util.Companion.debug
import com.motrack.test.Util.Companion.error
import com.motrack.test.websocket.ControlWebSocketClient
import java.net.URISyntaxException
import java.util.*
import java.util.concurrent.*
import kotlin.collections.HashMap
import kotlin.system.exitProcess

class TestLibrary {
    private val gson: Gson = Gson()
    private var waitControlQueue: BlockingQueue<String>? = null
    lateinit var controlClient: ControlWebSocketClient
    private var executor: ExecutorService? = null
    private var infoToServer: HashMap<String, String>? = null
    private lateinit var testSessionId: String
    private val currentTestNames = StringBuilder()
    private var commandListener: ICommandListener? = null
    private var commandJsonListener: ICommandJsonListener? = null
    private var commandRawJsonListener: ICommandRawJsonListener? = null
    private lateinit var currentTestName: String
    private lateinit var currentBasePath: String
    private var onExitListener: IOnExitListener? = null
    private var exitAfterEnd = true


    companion object {
        lateinit var baseUrl: String
        lateinit var controlUrl: String
    }

    constructor(
        baseUrl: String,
        controlUrl: String,
        commandRawJsonListener: ICommandRawJsonListener?
    ) : this(baseUrl, controlUrl) {
        this.commandRawJsonListener = commandRawJsonListener
    }

    constructor(
        baseUrl: String,
        controlUrl: String,
        commandJsonListener: ICommandJsonListener?
    ) : this(baseUrl, controlUrl) {
        this.commandJsonListener = commandJsonListener
    }

    constructor(
        baseUrl: String,
        controlUrl: String,
        commandListener: ICommandListener?
    ) : this(baseUrl, controlUrl) {
        this.commandListener = commandListener
    }

    private constructor(baseUrl: String, controlUrl: String) {
        TestLibrary.baseUrl = baseUrl
        TestLibrary.controlUrl = controlUrl
        debug("> base url: \t%s", baseUrl)
        debug("> control url: \t%s", controlUrl)
        this.initializeWebSocket(controlUrl)
    }

    private fun initializeWebSocket(controlUrl: String) {
        try {
            this.controlClient = ControlWebSocketClient(this, controlUrl)
            this.controlClient.connect()
            debug(" ---> control web socket client, connection state: ${this.controlClient.readyState}")
        } catch (e: URISyntaxException) {
            debug("Error, cannot create/connect with server web socket: [${e.message}]")
            e.printStackTrace()
        }
    }

    // resets test library to initial state
    private fun resetTestLibrary() {
        teardown(true)
        executor = Executors.newCachedThreadPool()
        waitControlQueue = LinkedBlockingQueue()
    }

    // clears test library
    private fun teardown(shutdownNow: Boolean) {
        executor?.let {
            if (shutdownNow) {
                debug("test library executor shutdownNow")
                it.shutdownNow()
            } else {
                debug("test library executor shutdown")
                it.shutdown()
            }
        }
        executor = null
        infoToServer = null
        waitControlQueue?.clear()
        waitControlQueue = null
    }

    fun startTestSession(clientSdk: String) {
        resetTestLibrary()

        // reconnect web socket client if disconnected
        if (!controlClient.isOpen) {
            debug("reconnecting web socket client ...")
            initializeWebSocket(controlUrl)
            // wait for WS to reconnect
            SystemClock.sleep(Constants.ONE_SECOND.toLong())
        }
        executor!!.submit { startTestSessionI(clientSdk) }
    }


    private fun startTestSessionI(clientSdk: String) {
        val httpResponse: NetworkUtils.Companion.HttpResponse? =
            NetworkUtils.sendPostI("/init_session", clientSdk, currentTestNames.toString())
        httpResponse?.let {
            this.testSessionId =
                httpResponse.headerFields[Constants.TEST_SESSION_ID_HEADER]?.get(0).toString()
        }

        // set test session ID on the web socket object in Test Server, so it can be uniquely identified
        controlClient.sendInitTestSessionSignal(this.testSessionId)
        debug("starting new test session with ID: ${this.testSessionId}")
        readResponseI(httpResponse)
    }

    fun setOnExitListener(onExitListener: IOnExitListener?) {
        this.onExitListener = onExitListener
    }

    fun signalEndWait(reason: String?) {
        waitControlQueue!!.offer(reason)
    }

    fun cancelTestAndGetNext() {
        resetTestLibrary()
        executor!!.submit {
            val httpResponse: NetworkUtils.Companion.HttpResponse? =
                NetworkUtils.sendPostI(Util.appendBasePath(currentBasePath, "/end_test_read_next"))
            readResponseI(httpResponse)
        }
    }

    fun addInfoToSend(key: String, value: String) {
        if (infoToServer == null) {
            infoToServer = HashMap()
        }
        infoToServer!![key] = value
    }

    fun sendInfoToServer(basePath: String) {
        executor!!.submit { sendInfoToServerI(basePath) }
    }


    private fun sendInfoToServerI(basePath: String) {
        val httpResponse: NetworkUtils.Companion.HttpResponse? =
            NetworkUtils.sendPostI(Util.appendBasePath(basePath, "/test_info"), null, infoToServer)
        infoToServer = null
        readResponseI(httpResponse)
    }


    fun readResponseI(httpResponse: NetworkUtils.Companion.HttpResponse?) {
        if (httpResponse == null) {
            debug("httpResponse is null")
            return
        }

        val testCommands: List<TestCommand> =
            gson.fromJson(
                httpResponse.response,
                Array<TestCommand>::class.java
            ).toList()


        try {
            execTestCommandsI(testCommands)
        } catch (e: InterruptedException) {
            debug("InterruptedException thrown %s", e.message)
        }
    }

    fun doNotExitAfterEnd() {
        exitAfterEnd = false
    }

    fun addTestDirectory(testDir: String) {
        currentTestNames.append(testDir)
        if (!testDir.endsWith("/") || !testDir.endsWith("/;")) {
            currentTestNames.append("/")
        }
        if (!testDir.endsWith(";")) {
            currentTestNames.append(";")
        }
    }

    fun addTest(testName: String) {
        currentTestNames.append(testName)
        if (!testName.endsWith(";")) {
            currentTestNames.append(";")
        }
    }

    @Throws(InterruptedException::class)
    private fun execTestCommandsI(testCommands: List<TestCommand>) {
        debug("testCommands: %s", testCommands)

        val gson = GsonBuilder().disableHtmlEscaping().create()
        for (testCommand in testCommands) {
            if (Thread.interrupted()) {
                error("Thread interrupted")
                return
            }
            debug("ClassName: %s", testCommand.className)
            debug("FunctionName: %s", testCommand.functionName)
            debug("Params:")

            testCommand.params?.let { params ->
                if (params.isNotEmpty()) {
                    for ((key, value) in params.entries) {
                        debug("\t%s: %s", key, value)
                    }
                }
            }

            val timeBefore = System.nanoTime()
            debug(
                "time before %s %s: %d",
                testCommand.className,
                testCommand.functionName,
                timeBefore
            )

            // execute TestLibrary command
            if (Constants.TEST_LIBRARY_CLASSNAME == testCommand.className) {
                executeTestLibraryCommandI(testCommand)
                val timeAfter = System.nanoTime()
                val timeElapsedMillis = TimeUnit.NANOSECONDS.toMillis(timeAfter - timeBefore)
                debug(
                    "time after %s %s: %d",
                    testCommand.className,
                    testCommand.functionName,
                    timeAfter
                )
                debug(
                    "time elapsed %s %s in milli seconds: %d",
                    testCommand.className,
                    testCommand.functionName,
                    timeElapsedMillis
                )
                continue
            }

            // execute Motrack command

            when {
                commandListener != null -> {
                    testCommand.params?.let {
                        commandListener!!.executeCommand(
                            testCommand.className,
                            testCommand.functionName,
                            it
                        )
                    }
                }
                commandJsonListener != null -> {
                    val toJsonParams = gson.toJson(testCommand.params)
                    debug("commandJsonListener test command params toJson: %s", toJsonParams)
                    commandJsonListener!!.executeCommand(
                        testCommand.className,
                        testCommand.functionName,
                        toJsonParams
                    )
                }
                commandRawJsonListener != null -> {
                    val toJsonCommand = gson.toJson(testCommand)
                    debug("commandRawJsonListener test command toJson: %s", toJsonCommand)
                    commandRawJsonListener!!.executeCommand(toJsonCommand)
                }
            }

            val timeAfter = System.nanoTime()
            val timeElapsedMillis = TimeUnit.NANOSECONDS.toMillis(timeAfter - timeBefore)
            debug(
                "time after %s.%s: %d",
                testCommand.className,
                testCommand.functionName,
                timeAfter
            )
            debug(
                "time elapsed %s.%s in milli seconds: %d",
                testCommand.className,
                testCommand.functionName,
                timeElapsedMillis
            )
        }
    }

    @Throws(InterruptedException::class)
    private fun executeTestLibraryCommandI(testCommand: TestCommand) {
        when (testCommand.functionName) {
            "resetTest" -> resetTestI(testCommand.params)
            "endTestReadNext" -> endTestReadNextI()
            "endTestSession" -> endTestSessionI()
            "wait" -> waitI(testCommand.params)
            "exit" -> exit()
        }
    }

    private fun resetTestI(params: Map<String, List<String>>?) {
        params?.let {
            if (params.containsKey("basePath")) {
                currentBasePath = params["basePath"]!![0]
                debug("current base path %s", currentBasePath)
            }
            if (params.containsKey("testName")) {
                currentTestName = params["testName"]!![0]
                debug("current test name %s", currentTestName)
            }
        }

        if (waitControlQueue != null) {
            waitControlQueue!!.clear()
        }
        infoToServer = null
        waitControlQueue = LinkedBlockingQueue()
    }

    private fun endTestReadNextI() {
        val httpResponse: NetworkUtils.Companion.HttpResponse? =
            NetworkUtils.sendPostI(Util.appendBasePath(currentBasePath, "/end_test_read_next"))
        readResponseI(httpResponse)
    }

    private fun endTestSessionI() {
        debug(" ---> test session ended!")
        teardown(false)
        if (exitAfterEnd) {
            exit()
        }
    }

    @Throws(InterruptedException::class)
    private fun waitI(params: Map<String, List<String>>?) {
        params?.let {
            if (params.containsKey(Constants.WAIT_FOR_CONTROL)) {
                val waitExpectedReason = params[Constants.WAIT_FOR_CONTROL]!![0]
                debug("wait for %s", waitExpectedReason)
                val endReason = waitControlQueue!!.take()
                debug("wait ended due to %s", endReason)
            }
            if (params.containsKey(Constants.WAIT_FOR_SLEEP)) {
                val millisToSleep = params[Constants.WAIT_FOR_SLEEP]!![0].toLong()
                debug("sleep for %s", millisToSleep)
                SystemClock.sleep(millisToSleep)
                debug("sleep ended")
            }
        }
    }

    private fun exit() {
        onExitListener?.onExit()
        exitProcess(0)
    }

}