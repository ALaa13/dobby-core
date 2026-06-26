package com.example.dobby.dto.discord

import kotlinx.serialization.Serializable

@Serializable
data class DiscordGuild(
    val id: String,
    val name: String,
    val icon: String?,
    val owner: Boolean,
    val permissions: String
)
