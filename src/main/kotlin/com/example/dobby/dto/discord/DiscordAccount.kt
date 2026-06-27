package com.example.dobby.dto.discord

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscordAccount(
    @SerialName("discord_user_id")
    val discordUserId: String,
    @SerialName("encrypted_token")
    val encryptedToken: String,
)
