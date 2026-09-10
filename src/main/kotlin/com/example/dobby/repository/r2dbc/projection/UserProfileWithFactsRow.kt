package com.example.dobby.repository.r2dbc.projection

import java.time.OffsetDateTime
import java.util.UUID

data class UserProfileWithFactsRow(
    val id: UUID,
    val discordUserId: String,
    val guildId: String,
    val displayName: String?,
    val avatarHash: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime?,
    val factsJson: String,
)
