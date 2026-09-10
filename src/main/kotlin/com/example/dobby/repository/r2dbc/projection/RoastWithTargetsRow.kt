package com.example.dobby.repository.r2dbc.projection

import java.time.OffsetDateTime
import java.util.UUID

data class RoastWithTargetsRow(
    val id: UUID,
    val guildId: String,
    val channelId: String,
    val roastText: String,
    val personaUsed: String?,
    val primaryTargetId: String,
    val clappedTheMostId: String,
    val burnAccuracy: Int,
    val severityScore: Int,
    val createdAt: OffsetDateTime,
    val targetsJson: String,
)
