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
    suspend fun saveFact(fact: UserFactCreateRequest): UserFactResponse =
        userFactStore
            .insert(
                profileId = fact.profileId,
                factText = fact.factText,
                source = fact.source.name,
            ).toResponse()

    suspend fun deleteFactById(factId: String) {
        userFactStore.deleteById(factId)
    }

    suspend fun deleteFactsByProfileId(profileId: String) {
        userFactStore.deleteAllByProfileId(profileId)
    }

    private fun UserFactEntity.toResponse(): UserFactResponse =
        UserFactResponse(
            id = requireNotNull(id).toString(),
            profileId = profileId.toString(),
            factText = factText,
            source = source,
            createdAt = requireNotNull(createdAt).toString(),
            updatedAt = updatedAt?.toString(),
        )
}
