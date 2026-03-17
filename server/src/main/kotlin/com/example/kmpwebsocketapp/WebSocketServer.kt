package com.example.kmpwebsocketapp

import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import java.util.UUID

class WebSocketServer(
    private val port: Int,
    private val onMessageReceived: (Message) -> Unit,
    private val onClientConnected: (String) -> Unit,
    private val onClientDisconnected: (String) -> Unit
) {
    private var server: EmbeddedServer<*, *>? = null
    private val connectedClients = mutableMapOf<String, WebSocketSession>()

    suspend fun start() {
        server = embeddedServer(Netty, port = port) {
            install(WebSockets)
            routing {
                webSocket("/") {
                    val clientId = generateClientId()
                    connectedClients[clientId] = this
                    onClientConnected(clientId)

                    try {
                        for (frame in incoming) {
                            frame as? Frame.Text ?: continue
                            val text = frame.readText()
                            val message = Json.decodeFromString<Message>(text)
                            onMessageReceived(message.copy(timestamp = System.currentTimeMillis())) // сохраняем время получения
                        }
                    } catch (e: Exception) {
                        println("Error: ${e.message}")
                    } finally {
                        connectedClients.remove(clientId)
                        onClientDisconnected(clientId)
                    }
                }
            }
        }
        server?.start(wait = false)
    }

    suspend fun stop() {
        server?.stop(1000, 5000)
    }

    private fun generateClientId() = UUID.randomUUID().toString()
}