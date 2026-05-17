package com.example.dobby.dto

import kotlinx.serialization.Serializable

@Serializable
data class DiscordChatMessage(
    val author: String,
    val content: String,
    val timestamp: String
)
