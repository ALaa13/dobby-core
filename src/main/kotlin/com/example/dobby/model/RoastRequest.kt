package com.example.dobby.model

import kotlinx.serialization.Serializable

@Serializable
data class RoastRequest(
    val channelId: String,
    val messageId: String,
    val messages: List<ChatDiscordMessage>
)
