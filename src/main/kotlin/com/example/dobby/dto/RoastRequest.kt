package com.example.dobby.dto

import kotlinx.serialization.Serializable

@Serializable
data class RoastRequest(
    val channelId: String,
    val messages: List<DiscordChatMessage>
)
