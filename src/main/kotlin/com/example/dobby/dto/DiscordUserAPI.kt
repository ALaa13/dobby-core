package com.example.dobby.dto

import kotlinx.serialization.Serializable

@Serializable
data class DiscordUser(
    val id: String,
    val username: String?,
    val discriminator: String?,
    val avatar: String?
)