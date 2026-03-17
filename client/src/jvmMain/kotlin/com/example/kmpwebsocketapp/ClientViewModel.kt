package com.example.kmpwebsocketapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class ClientViewModel : ViewModel() {
    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log

    private var client: WebSocketClient? = null
    private val clientId = UUID.randomUUID().toString()

    sealed class ConnectionStatus {
        object Connected : ConnectionStatus()
        object Connecting : ConnectionStatus()
        object Disconnected : ConnectionStatus()
        data class Reconnecting(val attempt: Int, val maxAttempts: Int) : ConnectionStatus()
    }

    fun connect(host: String, port: Int) {
        viewModelScope.launch {
            // Отключаем предыдущего клиента, если он есть
            client?.disconnect()

            _connectionStatus.value = ConnectionStatus.Connecting
            addToLog("Connecting to $host:$port...")

            // Создаём нового клиента с переданным scope
            val newClient = WebSocketClient(
                host = host,
                port = port,
                onConnected = {
                    _connectionStatus.value = ConnectionStatus.Connected
                    addToLog("Connected to $host:$port")
                },
                onDisconnected = { error ->
                    _connectionStatus.value = ConnectionStatus.Disconnected
                    addToLog("Disconnected: ${error?.message ?: "normal closure"}")
                },
                onMessageReceived = { response ->
                    addToLog("Received: $response")
                },
                onReconnecting = { attempt, max ->
                    _connectionStatus.value = ConnectionStatus.Reconnecting(attempt, max)
                    addToLog("Reconnecting attempt $attempt/$max...")
                },
                scope = viewModelScope // передаём scope
            )
            client = newClient
            newClient.connect()
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            if (_connectionStatus.value !is ConnectionStatus.Connected) {
                addToLog("Not connected")
                return@launch
            }
            val message = Message(
                clientId = clientId,
                timestamp = System.currentTimeMillis(),
                message = text
            )
            try {
                client?.sendMessage(message)
                addToLog("Sent: $text")
            } catch (e: Exception) {
                addToLog("Send error: ${e.message}")
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            client?.disconnect()
            _connectionStatus.value = ConnectionStatus.Disconnected
            addToLog("Disconnected manually")
        }
    }

    private fun addToLog(msg: String) {
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        _log.update { it + "[$time] $msg" }
    }
}