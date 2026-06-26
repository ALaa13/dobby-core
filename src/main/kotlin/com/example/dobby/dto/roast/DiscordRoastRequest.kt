package com.example.dobby.dto.roast

import com.example.dobby.dto.discord.DiscordChatMessage
import kotlinx.serialization.Serializable

@Serializable
data class RoastRequest(
    val channelId: String,
    val guildId: String,
    val messages: List<DiscordChatMessage>,
    val persona: String?
)


fun RoastRequest.toResult(content: String, success: Boolean) =
    DiscordRoastResult(channelId, content, success)