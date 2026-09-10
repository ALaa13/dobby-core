package com.example.dobby.entity

import java.time.OffsetDateTime
import java.util.UUID

data class UserProfileEntity(
    val id: UUID,
    val discordUserId: String,
    val guildId: String,
    val displayName: String? = null,
    val avatarHash: String? = null,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime? = null,
)
