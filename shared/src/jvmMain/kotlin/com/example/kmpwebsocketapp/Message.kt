package com.example.kmpwebsocketapp

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val clientId: String,
    val timestamp: Long,
    val message: String
)