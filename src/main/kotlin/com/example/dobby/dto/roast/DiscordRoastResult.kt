package com.example.dobby.dto.roast

import kotlinx.serialization.Serializable

@Serializable
data class DiscordRoastResult(
    val channelId: String,
    val content: String,
    val success: Boolean
)
