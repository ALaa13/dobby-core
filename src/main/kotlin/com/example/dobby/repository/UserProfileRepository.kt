package com.example.dobby.repository

import com.example.dobby.dto.fact.UserFactResponse
import com.example.dobby.dto.user.UserProfileCreateRequest
import com.example.dobby.dto.user.UserProfileResponse
import com.example.dobby.entity.UserProfileEntity
import com.example.dobby.exception.DobbyException
import com.example.dobby.repository.r2dbc.UserProfilePostgresStore
import com.example.dobby.repository.r2dbc.projection.UserProfileWithFactsRow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Repository

@Repository
class UserProfileRepository(
    private val userProfileStore: UserProfilePostgresStore,
    private val json: Json,
) {
    suspend fun findProfile(
        discordUserId: String,
        guildId: String,
    ): UserProfileResponse? =
        userProfileStore
            .findByDiscordUserIdAndGuildId(discordUserId, guildId)
            ?.toResponse()

    suspend fun findAllByGuildId(guildId: String): List<UserProfileResponse> =
        userProfileStore
            .findAllByGuildId(guildId)
            .map { it.toResponse() }

    suspend fun saveProfile(profile: UserProfileCreateRequest): UserProfileResponse =
        userProfileStore
            .insert(
                discordUserId = profile.discordUserId,
                guildId = profile.guildId,
                displayName = profile.displayName,
                avatarHash = profile.avatarHash,
            )
            .toResponse()

    suspend fun deleteAllByGuildId(guildId: String) {
        userProfileStore.deleteAllByGuildId(guildId)
    }

    suspend fun upsertProfiles(profiles: List<UserProfileCreateRequest>) {
        if (profiles.isEmpty()) return

        userProfileStore.upsertProfiles(profiles)
    }

    private fun UserProfileEntity.toResponse(): UserProfileResponse =
        UserProfileResponse(
            id = requireNotNull(id).toString(),
            discordUserId = discordUserId,
            guildId = guildId,
            displayName = displayName,
            avatarHash = avatarHash,
            createdAt = requireNotNull(createdAt).toString(),
            updatedAt = updatedAt?.toString(),
        )

    private fun UserProfileWithFactsRow.toResponse(): UserProfileResponse =
        UserProfileResponse(
            id = id.toString(),
            discordUserId = discordUserId,
            guildId = guildId,
            displayName = displayName,
            avatarHash = avatarHash,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt?.toString(),
            facts = decodeFacts(factsJson),
        )

    private fun decodeFacts(factsJson: String): List<UserFactResponse> =
        try {
            json.decodeFromString(factsJson)
        } catch (exception: SerializationException) {
            throw DobbyException.DataMappingException("Failed to map user profile facts", exception)
        }
}
