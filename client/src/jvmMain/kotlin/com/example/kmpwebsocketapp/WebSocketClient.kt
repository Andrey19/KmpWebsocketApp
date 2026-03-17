package com.example.kmpwebsocketapp

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

class WebSocketClient(
    private val host: String,
    private val port: Int,
    private val onConnected: () -> Unit,
    private val onDisconnected: (Throwable?) -> Unit,
    private val onMessageReceived: (String) -> Unit
) {
    private var session: WebSocketSession? = null
    private val client = HttpClient(CIO) { install(WebSockets) }
    private var isActive = true

    suspend fun connect() {
        withContext(Dispatchers.IO) {
            try {
                client.webSocket(method = HttpMethod.Get, host = host, port = port, path = "/") {
                    session = this
                    onConnected()
                    for (frame in incoming) {
                        frame as? Frame.Text ?: continue
                        onMessageReceived(frame.readText())
                    }
                }
            } catch (e: Exception) {
                if (isActive) onDisconnected(e)
            } finally {
                if (isActive) onDisconnected(null)
            }
        }
    }

    suspend fun sendMessage(message: Message) {
        val json = Json.encodeToString(Message.serializer(), message)
        session?.send(Frame.Text(json))
    }

    suspend fun disconnect() {
        isActive = false
        session?.close()
        client.close()
    }
}