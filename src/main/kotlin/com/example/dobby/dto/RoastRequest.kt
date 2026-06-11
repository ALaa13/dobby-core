package com.example.dobby.dto

import kotlinx.serialization.Serializable

@Serializable
data class RoastRequest(
    val channelId: String,
    val guildId: String,
    val messages: List<DiscordChatMessage>,
    val persona: String?
)


fun RoastRequest.toResult(content: String, success: Boolean) =
    RoastResult(channelId, content, success)