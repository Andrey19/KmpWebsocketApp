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
    private val onMessageReceived: (String) -> Unit,
    private val onReconnecting: (Int, Int) -> Unit = { _, _ -> },
    private val maxRetries: Int = 5,
    private val retryDelay: Long = 1000,
    private val scope: CoroutineScope // добавляем внешний scope
) {
    private var session: WebSocketSession? = null
    private val client = HttpClient(CIO) { install(WebSockets) }
    private var connectJob: Job? = null
    private var isActive = true

    fun connect() {
        if (connectJob?.isActive == true) return
        connectJob = scope.launch {
            withContext(Dispatchers.IO) {
                var retryCount = 0
                while (isActive) {
                    try {
                        client.webSocket(method = HttpMethod.Get, host = host, port = port, path = "/") {
                            session = this
                            retryCount = 0
                            onConnected()
                            for (frame in incoming) {
                                if (!isActive) break
                                frame as? Frame.Text ?: continue
                                onMessageReceived(frame.readText())
                            }
                        }
                        if (!isActive) break
                        retryCount++
                        if (retryCount > maxRetries) {
                            onDisconnected(null)
                            break
                        }
                        onReconnecting(retryCount, maxRetries)
                        delay(retryDelay * (1 shl (retryCount - 1)))
                    } catch (e: Exception) {
                        if (!isActive) break
                        retryCount++
                        if (retryCount > maxRetries) {
                            onDisconnected(e)
                            break
                        }
                        onReconnecting(retryCount, maxRetries)
                        delay(retryDelay * (1 shl (retryCount - 1)))
                    }
                }
                if (!isActive) {
                    onDisconnected(null)
                }
            }
        }
    }

    suspend fun sendMessage(message: Message) {
        val json = Json.encodeToString(Message.serializer(), message)
        session?.send(Frame.Text(json))
    }

    suspend fun disconnect() {
        isActive = false
        connectJob?.cancel()
        session?.close()
        client.close()
    }
}
