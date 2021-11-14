package com.motrack.test.websocket

import com.google.gson.Gson
import com.motrack.test.TestLibrary
import com.motrack.test.Util.Companion.debug
import com.motrack.test.Util.Companion.error
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

/**
 * @author yaya (@yahyalmh)
 * @since 14th November 2021
 */

class ControlWebSocketClient(private val testLibrary: TestLibrary, serverUri: String?) :
    WebSocketClient(URI(serverUri)) {
    private val gson = Gson()

    override fun onOpen(handshakedata: ServerHandshake) {
        debug("[WebSocket] connection opened with the server")
    }

    override fun onMessage(message: String) {
        //debug(String.format("[WebSocket] onMessage, message [%s]", message));
        val incomingSignal: ControlSignal = this.parseControlSignal(message)
        this.handleIncomingSignal(incomingSignal)
    }

    private fun parseControlSignal(message: String?): ControlSignal {
        val incomingSignal: ControlSignal = try {
            gson.fromJson(message, ControlSignal::class.java)
        } catch (ex: Exception) {
            error("[WebSocket] onMessage Error! Cannot parse message [${message ?: "null"}]. Details: [${ex.message}]")
            ex.printStackTrace()
            ControlSignal(SignalType.UNKNOWN)
        }
        return incomingSignal
    }

    private fun handleIncomingSignal(incomingSignal: ControlSignal) {
        when (incomingSignal.getType()) {
            SignalType.INFO -> debug("[WebSocket] info from the server: ${incomingSignal.value}")
            SignalType.END_WAIT -> {
                debug("[WebSocket] end wait signal received, reason:${incomingSignal.value}")
                testLibrary.signalEndWait(incomingSignal.value)
            }
            SignalType.CANCEL_CURRENT_TEST -> {
                debug("[WebSocket] cancel test received, reason: " + incomingSignal.value)
                testLibrary.cancelTestAndGetNext()
            }
            else -> debug("[WebSocket] unknown signal received by the server. Value: ${incomingSignal.value}")

        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        debug("[WebSocket] onClose, code [$code], reason [$reason]")
    }

    override fun onError(ex: Exception?) {
        debug(String.format("[WebSocket] onError [${ex!!.message}]"))
    }

    fun sendInitTestSessionSignal(testSessionId: String) {
        val initSignal = ControlSignal(SignalType.INIT_TEST_SESSION, testSessionId)
        send(gson.toJson(initSignal))
    }

}