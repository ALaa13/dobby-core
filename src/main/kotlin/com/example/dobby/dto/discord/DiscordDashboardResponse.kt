package com.example.dobby.dto.discord

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscordDashboardResponse(
    @SerialName("discord_user_id") val discordUserId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String,
    @SerialName("managed_guilds") val managedGuilds: List<DiscordGuild>,
)
