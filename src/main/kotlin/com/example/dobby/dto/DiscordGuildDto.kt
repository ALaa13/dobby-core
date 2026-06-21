package com.example.dobby.dto

import kotlinx.serialization.Serializable

@Serializable
data class DiscordGuildDto(
    val id: String,
    val name: String,
    val icon: String?,
    val owner: Boolean,
    val permissions: String
)
