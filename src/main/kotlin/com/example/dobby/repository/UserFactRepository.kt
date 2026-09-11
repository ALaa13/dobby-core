package com.example.dobby.repository

import com.example.dobby.dto.fact.UserFactCreateRequest
import com.example.dobby.dto.fact.UserFactResponse
import com.example.dobby.entity.UserFactEntity
import com.example.dobby.repository.r2dbc.UserFactPostgresStore
import org.springframework.stereotype.Repository

@Repository
class UserFactRepository(
    private val userFactStore: UserFactPostgresStore,
) {
    suspend fun saveFact(fact: UserFactCreateRequest): UserFactResponse {
        return userFactStore
            .insert(
                profileId = fact.profileId,
                factText = fact.factText,
                source = fact.source.name,
            ).toResponse()
    }

    suspend fun deleteFactById(factId: String) {
        userFactStore.deleteById(factId)
    }

    suspend fun deleteFactsByProfileId(profileId: String) {
        userFactStore.deleteAllByProfileId(profileId)
    }

    suspend fun deleteFactsByGuildId(guildId: String) {
        userFactStore.deleteAllByGuildId(guildId)
    }

    private fun UserFactEntity.toResponse(): UserFactResponse {
        return UserFactResponse(
            id = id.toString(),
            profileId = profileId.toString(),
            factText = factText,
            source = source,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt?.toString(),
        )
    }
}
