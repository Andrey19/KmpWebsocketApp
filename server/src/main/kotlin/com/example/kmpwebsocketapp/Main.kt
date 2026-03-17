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
        title = "WebSocket Server"
    ) {
        ServerApp()
    }
}

@Composable
fun ServerApp(viewModel: ServerViewModel = viewModel<ServerViewModel> { ServerViewModel() }) {
    val messages by viewModel.messages.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val log by viewModel.log.collectAsState()

    var port by remember { mutableStateOf("8080") }

    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            // Панель управления
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    enabled = !isRunning,
                    modifier = Modifier.width(100.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (isRunning) {
                            viewModel.stopServer()
                        } else {
                            viewModel.startServer(port.toIntOrNull() ?: 8080)
                        }
                    },
                    colors = ButtonColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(if (isRunning) "Stop Server" else "Start Server")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Connected clients: ${clients.size}")
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

            Spacer(modifier = Modifier.height(16.dp))

            // Полученные сообщения
            Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    reverseLayout = true
                ) {
                    items(messages.reversed()) { msg ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Client: ${msg.clientId.take(8)}", style = MaterialTheme.typography.labelSmall)
                                val formattedTime = java.time.Instant.ofEpochMilli(msg.timestamp)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .format(java.time.format.DateTimeFormatter.ofPattern(" HH:mm:ss dd.MM.yyyy "))
                                Text("Time: $formattedTime", style = MaterialTheme.typography.labelSmall)
                                Text(msg.message, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}