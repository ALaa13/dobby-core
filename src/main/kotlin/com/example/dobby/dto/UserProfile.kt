package com.example.dobby.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class UserProfileCreateRequest(
    @SerialName("discord_user_id")
    val discordUserId: String,
    @SerialName("guild_id")
    val guildId: String,
    @SerialName("display_name")
    val displayName: String?,
)


@Serializable
data class UserProfileResponse(
    @SerialName("id")
    val id: String,
    @SerialName("discord_user_id")
    val discordUserId: String,
    @SerialName("guild_id")
    val guildId: String,
    @SerialName("display_name")
    val displayName: String?,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String?
)

