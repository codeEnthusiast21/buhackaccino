package com.example.buhackaccino

data class ChatMessage(
    val text: String,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)