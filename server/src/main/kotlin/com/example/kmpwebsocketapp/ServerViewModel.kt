package com.example.kmpwebsocketapp


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ServerViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _clients = MutableStateFlow<List<String>>(emptyList())
    val clients: StateFlow<List<String>> = _clients

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log

    private var server: WebSocketServer? = null

    fun startServer(port: Int) {
        viewModelScope.launch {
            server = WebSocketServer(
                port = port,
                onMessageReceived = { message ->
                    _messages.update { it + message }
                    addToLog("Received from ${message.clientId}: ${message.message}")
                },
                onClientConnected = { clientId ->
                    _clients.update { it + clientId }
                    addToLog("Client connected: $clientId")
                },
                onClientDisconnected = { clientId ->
                    _clients.update { it - clientId }
                    addToLog("Client disconnected: $clientId")
                }
            )
            server?.start() // это suspend-функция, вызывается внутри launch
            _isRunning.value = true
            addToLog("Server started on port $port")
        }
    }

    fun stopServer() {
        viewModelScope.launch {
            server?.stop() // suspend-функция
            _isRunning.value = false
            _messages.update { emptyList() }
            _clients.update { emptyList() }
            addToLog("Server stopped")
        }
    }

    private fun addToLog(text: String) {
        _log.update { it + "[${java.time.LocalTime.now()}] $text" }
    }
}