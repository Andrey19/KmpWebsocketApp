package com.example.kmpwebsocketapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.lifecycle.viewmodel.compose.viewModel

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "WebSocket Client"
    ) {
        ClientApp()
    }
}

@Composable
fun ClientApp(viewModel: ClientViewModel = viewModel { ClientViewModel() }) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val log by viewModel.log.collectAsState()

    var host by remember { mutableStateOf("localhost") }
    var port by remember { mutableStateOf("8080") }
    var messageText by remember { mutableStateOf("") }

    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            // Строка подключения
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host") },
                    enabled = connectionStatus == ClientViewModel.ConnectionStatus.Disconnected,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    enabled = connectionStatus == ClientViewModel.ConnectionStatus.Disconnected,
                    modifier = Modifier.width(100.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (connectionStatus == ClientViewModel.ConnectionStatus.Connected) {
                            viewModel.disconnect()
                        } else {
                            viewModel.connect(host, port.toIntOrNull() ?: 8080)
                        }
                    },
                    colors = ButtonColors(
                        containerColor = when (connectionStatus) {
                            ClientViewModel.ConnectionStatus.Connected -> MaterialTheme.colorScheme.error
                            ClientViewModel.ConnectionStatus.Connecting -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        when (val status = connectionStatus) {
                            ClientViewModel.ConnectionStatus.Connected -> "Disconnect"
                            ClientViewModel.ConnectionStatus.Connecting -> "Connecting..."
                            is ClientViewModel.ConnectionStatus.Reconnecting ->
                                "Reconnecting (${status.attempt}/${status.maxAttempts})..."
                            else -> "Connect"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Поле отправки сообщения
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Message") },
                    enabled = connectionStatus == ClientViewModel.ConnectionStatus.Connected,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        }
                    },
                    enabled = connectionStatus == ClientViewModel.ConnectionStatus.Connected && messageText.isNotBlank()
                ) {
                    Text("Send")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Лог событий
            Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    reverseLayout = true
                ) {
                    items(log.reversed()) { logEntry ->
                        Text(logEntry, modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
}