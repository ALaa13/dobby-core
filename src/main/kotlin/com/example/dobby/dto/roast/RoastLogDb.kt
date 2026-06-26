package com.example.dobby.dto.roast

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class RoastLogDb(
    @SerialName("guild_id")
    val guildId: String,
    @SerialName("channel_id")
    val channelId: String,
    @SerialName("roast_text")
    val roastText: String,
    @SerialName("persona_used")
    val personaUsed: String?,
    @SerialName("primary_target_id")
    val primaryTargetId: String,
    @SerialName("clapped_the_most_id")
    val clappedTheMostId: String,
    @SerialName("burn_accuracy")
    val burnAccuracy: Int,
    @SerialName("severity_score")
    val severityScore: Int
)


@Serializable
data class RoastLogDbResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("guild_id")
    val guildId: String,
    @SerialName("channel_id")
    val channelId: String,
    @SerialName("roast_text")
    val roastText: String,
    @SerialName("persona_used")
    val personaUsed: String?,
    @SerialName("primary_target_id")
    val primaryTargetId: String,
    @SerialName("clapped_the_most_id")
    val clappedTheMostId: String,
    @SerialName("burn_accuracy")
    val burnAccuracy: Int,
    @SerialName("severity_score")
    val severityScore: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("roast_targets")
    val targets: List<RoastTargetDbResponseDto> = emptyList()
)
