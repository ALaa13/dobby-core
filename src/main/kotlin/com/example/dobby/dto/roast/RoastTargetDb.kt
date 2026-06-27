package com.example.dobby.dto.roast

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoastTargetDb(
    @SerialName("roast_id")
    val roastId: String,
    @SerialName("discord_user_id")
    val discordUserId: String,
    @SerialName("guild_id")
    val guildId: String,
    @SerialName("damage_reason")
    val reason: String,
)

@Serializable
data class RoastTargetDbResponseDto(
    @SerialName("roast_id")
    val roastId: String,
    @SerialName("discord_user_id")
    val discordUserId: String,
    @SerialName("guild_id")
    val guildId: String,
    @SerialName("damage_reason")
    val reason: String,
    @SerialName("user_profiles")
    val userProfile: NestedUserProfileDto? = null,
)

@Serializable
data class NestedUserProfileDto(
    @SerialName("display_name")
    val displayName: String?,
    @SerialName("avatar_hash")
    val avatarHash: String?,
)
