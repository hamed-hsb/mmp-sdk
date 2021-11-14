package com.motrack.test

/**
 * @author yaya (@yahyalmh)
 * @since 14th November 2021
 */
import com.google.gson.Gson
import com.motrack.test.Util.Companion.debug
import java.net.URISyntaxException
import java.util.concurrent.BlockingQueue

class TestLibrary {
    private val gson: Gson = Gson()
    private val waitControlQueue: BlockingQueue<String>? = null
    private var controlClient: ControlWebSocketClient? = null

    companion object {
        var baseUrl: String? = null
        var controlUrl: String? = null
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
            debug(" ---> control web socket client, connection state: ${this.controlClient.getReadyState()}")
        } catch (e: URISyntaxException) {
            debug("Error, cannot create/connect with server web socket: [${e.message}]")
            e.printStackTrace()
        }
    }
}