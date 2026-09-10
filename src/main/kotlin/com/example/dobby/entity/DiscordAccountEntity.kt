package com.example.dobby.entity

import java.time.OffsetDateTime

data class DiscordAccountEntity(
    val discordUserId: String,
    val encryptedToken: String,
    val createdAt: OffsetDateTime,
)
