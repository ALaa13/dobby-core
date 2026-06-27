package com.example.dobby.dto.discord

import kotlinx.serialization.Serializable

@Serializable
data class DiscordFactRequest(
    val fact: String,
    val discordUserId: String,
    val guildId: String,
    val displayName: String?,
    val avatarHash: String?,
)
