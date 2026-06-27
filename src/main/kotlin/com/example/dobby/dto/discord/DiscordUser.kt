package com.example.dobby.dto.discord

import kotlinx.serialization.Serializable

@Serializable
data class DiscordUser(
    val id: String,
    val username: String,
    val avatar: String?,
)
