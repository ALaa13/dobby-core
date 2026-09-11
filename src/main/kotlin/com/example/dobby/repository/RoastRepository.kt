package com.example.dobby.repository

import com.example.dobby.dto.roast.RoastLogDbResponse
import com.example.dobby.dto.roast.RoastResult
import com.example.dobby.dto.roast.RoastTargetDbResponseDto
import com.example.dobby.exception.DobbyException
import com.example.dobby.repository.r2dbc.RoastPostgresStore
import com.example.dobby.repository.r2dbc.projection.RoastWithTargetsRow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Repository

@Repository
class RoastRepository(
    private val roastStore: RoastPostgresStore,
    private val json: Json,
) {
    suspend fun saveRoastResult(
        guildId: String,
        channelId: String,
        result: RoastResult,
    ) {
        roastStore.saveRoastResult(guildId, channelId, result)
    }

    suspend fun getGuildRoasts(guildId: String): List<RoastLogDbResponse> {
        return roastStore
            .findAllByGuildId(guildId)
            .map { it.toResponse() }
    }

    private fun RoastWithTargetsRow.toResponse(): RoastLogDbResponse {
        return RoastLogDbResponse(
            id = id.toString(),
            guildId = guildId,
            channelId = channelId,
            roastText = roastText,
            personaUsed = personaUsed,
            primaryTargetId = primaryTargetId,
            clappedTheMostId = clappedTheMostId,
            burnAccuracy = burnAccuracy,
            severityScore = severityScore,
            createdAt = createdAt.toString(),
            targets = decodeTargets(targetsJson),
        )
    }

    private fun decodeTargets(targetsJson: String): List<RoastTargetDbResponseDto> {
        return try {
            json.decodeFromString(targetsJson)
        } catch (exception: SerializationException) {
            throw DobbyException.DataMappingException("Failed to map roast targets", exception)
        }
    }
}
