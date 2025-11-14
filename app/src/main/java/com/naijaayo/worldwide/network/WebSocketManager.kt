package com.naijaayo.worldwide.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class WebSocketManager {

    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    private var session: DefaultClientWebSocketSession? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _messageFlow = MutableSharedFlow<com.naijaayo.worldwide.Message>()
    val messageFlow = _messageFlow.asSharedFlow()

    private val _connectionStateFlow = MutableSharedFlow<Boolean>()
    val connectionStateFlow = _connectionStateFlow.asSharedFlow()

    private val connectionChannel = Channel<Boolean>(Channel.CONFLATED)

    fun connect(userId: String) {
        coroutineScope.launch {
            try {
                client.webSocket(
                    method = HttpMethod.Get,
                    host = "ayo.sampidia.com",
                    port = 443,
                    path = "/ws/messages/$userId"
                ) {
                    session = this
                    connectionChannel.send(true)
                    _connectionStateFlow.emit(true)
                    listenForMessages()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                connectionChannel.send(false)
                _connectionStateFlow.emit(false)
            }
        }
    }

    private suspend fun listenForMessages() {
        try {
            session?.incoming?.let { incoming ->
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        try {
                            val message = Json.decodeFromString<com.naijaayo.worldwide.Message>(frame.readText())
                            _messageFlow.emit(message)
                        } catch (e: Exception) {
                            // Handle parsing error
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        coroutineScope.launch {
            session?.close()
            session = null
            _connectionStateFlow.emit(false)
        }
    }

    suspend fun awaitConnection(): Boolean = connectionChannel.receive()

    fun isConnected(): Boolean = session != null
}
