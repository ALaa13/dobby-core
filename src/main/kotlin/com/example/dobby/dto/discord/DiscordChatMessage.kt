package com.example.dobby.dto.discord

import kotlinx.serialization.Serializable

@Serializable
data class DiscordChatMessage(
    val displayName: String,
    val discordUserId: String,
    val avatarHash: String,
    val content: String,
    val timestamp: String,
)
