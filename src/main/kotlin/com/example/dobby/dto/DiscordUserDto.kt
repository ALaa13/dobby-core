package com.example.dobby.dto

import kotlinx.serialization.Serializable

@Serializable
data class DiscordUserDto(
    val id: String,
    val username: String,
    val avatar: String?
)
