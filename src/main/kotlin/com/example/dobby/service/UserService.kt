package com.example.dobby.service

import com.example.dobby.dto.*
import com.example.dobby.repository.UserFactRepository
import com.example.dobby.repository.UserProfileRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userProfileRepository: UserProfileRepository,
    private val userFactRepository: UserFactRepository
) {
    private suspend fun getUserProfile(profile: UserProfileCreateRequest): UserProfileResponse {
        val existingProfile = userProfileRepository.findByDiscordIdAndGuildId(profile.discordUserId, profile.guildId)
        return existingProfile ?: userProfileRepository.insertNewProfile(profile)
    }

    suspend fun saveFact(request: DiscordFactRequest) {
        val profile = getUserProfile(
            UserProfileCreateRequest(
                discordUserId = request.discordUserId,
                guildId = request.guildId,
                displayName = request.displayName
            )
        )
        val fact = UserFactCreateRequest(
            profileId = profile.id,
            factText = request.fact,
            source = FactSource.USER_SUBMISSION,
            confidenceScore = 80,
            roastabilityScore = 20
        )
        userFactRepository.saveFact(fact)
    }
}