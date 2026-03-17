package com.example.kmpwebsocketapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform