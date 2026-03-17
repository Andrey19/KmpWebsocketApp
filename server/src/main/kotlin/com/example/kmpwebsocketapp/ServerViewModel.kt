package com.example.kmpwebsocketapp


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

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

    // Файл для хранения истории сообщений (JSON Lines)
    private val historyFile = File("messages.jsonl")
    private val json = Json { prettyPrint = false } // компактный JSON

    init {
        viewModelScope.launch {
            loadHistory()
        }
    }

    private suspend fun loadHistory() = withContext(Dispatchers.IO) {
        if (!historyFile.exists()) return@withContext
        try {
            val history = historyFile.readLines()
                .mapNotNull { line ->
                    try {
                        json.decodeFromString<Message>(line)
                    } catch (e: Exception) {
                        addToLog("Ошибка чтения строки истории: ${e.message}")
                        null
                    }
                }
            _messages.update { history }
            addToLog("Загружено ${history.size} сообщений из истории")
        } catch (e: Exception) {
            addToLog("Ошибка загрузки истории: ${e.message}")
        }
    }

    private suspend fun saveMessage(message: Message) = withContext(Dispatchers.IO) {
        try {
            val line = json.encodeToString(Message.serializer(), message)
            historyFile.appendText(line + "\n")
        } catch (e: Exception) {
            addToLog("Ошибка сохранения сообщения: ${e.message}")
        }
    }

    fun startServer(port: Int) {
        viewModelScope.launch {
            loadHistory()

            server = WebSocketServer(
                port = port,
                onMessageReceived = { message ->
                    _messages.update { it + message }
                    // Сохраняем каждое новое сообщение в фоне
                    viewModelScope.launch {
                        saveMessage(message)
                    }
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
            server?.start()
            _isRunning.value = true
            addToLog("Server started on port $port")
        }
    }

    fun stopServer() {
        viewModelScope.launch {
            server?.stop()
            _isRunning.value = false
            addToLog("Server stopped")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                if (historyFile.exists()) {
                    historyFile.delete()
                    addToLog("History file deleted")
                }
                _messages.update { emptyList() }
                addToLog("Message list cleared")
            } catch (e: Exception) {
                addToLog("Error clearing history: ${e.message}")
            }
        }
    }

    private fun addToLog(text: String) {
        _log.update { it + "[${java.time.LocalTime.now()}] $text" }
    }
}