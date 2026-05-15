package com.example.dobby.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatDiscordMessage(
    val author: String,
    val content: String,
    val timestamp: String
)
