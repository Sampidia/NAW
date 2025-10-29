package com.naijaayo.worldwide.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

class SocketManager {

    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    private var session: DefaultClientWebSocketSession? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _eventFlow = MutableSharedFlow<Pair<String, Any>>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val connectionChannel = Channel<Boolean>(Channel.CONFLATED)

    fun connect() {
        coroutineScope.launch {
            try {
                client.webSocket(method = HttpMethod.Get, host = "ayo.sampidia.com", port = 443, path = "/ws") {
                    session = this
                    connectionChannel.send(true)
                    listen() // Start listening for incoming messages
                }
            } catch (e: Exception) {
                e.printStackTrace()
                connectionChannel.send(false)
            }
        }
    }

    private suspend fun listen() {
        try {
            session?.let {
                for (frame in it.incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        val json = JSONObject(text)
                        val eventName = json.getString("type")
                        val data = json.opt("data")
                        _eventFlow.emit(eventName to (data ?: text))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun on(eventName: String, listener: (Any) -> Unit) {
        coroutineScope.launch {
            eventFlow.collect { (name, data) ->
                if (name == eventName) {
                    listener(data)
                }
            }
        }
    }

    fun emit(eventName: String, data: JSONObject) {
        coroutineScope.launch {
            val message = data.put("type", eventName).toString()
            session?.send(message)
        }
    }

    suspend fun awaitConnection() {
        connectionChannel.receive()
    }

    fun disconnect() {
        coroutineScope.launch {
            session?.close()
            session = null
        }
    }
}
