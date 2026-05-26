package com.example.dobby.service

import com.example.dobby.dto.*
import com.example.dobby.repository.UserFactRepository
import com.example.dobby.repository.UserProfileRepository
import com.example.dobby.util.Logging
import org.springframework.stereotype.Service

@Service
class FactService(
    private val userProfileRepository: UserProfileRepository,
    private val userFactRepository: UserFactRepository
) {
    private suspend fun getOrCreateProfile(profile: UserProfileCreateRequest): UserProfileResponse {
        val existingProfile = userProfileRepository.findProfile(profile.discordUserId, profile.guildId)
        return existingProfile ?: userProfileRepository.saveProfile(profile)
    }

    suspend fun saveFact(request: DiscordFactRequest) {
        Logging.logInfo("Received remember request for user $request")
        val profile = getOrCreateProfile(
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

    suspend fun getFacts(discordUserId: String, guildId: String): List<UserFactResponse> {
        return userProfileRepository.findProfile(discordUserId, guildId)?.facts ?: emptyList()
    }

    suspend fun resetFacts(discordUserId: String, guildId: String): Boolean {
        val profile = userProfileRepository.findProfile(discordUserId, guildId) ?: return false
        userFactRepository.deleteFactsByProfileId(profile.id)
        Logging.logInfo("Deleted all facts for user $discordUserId")
        return true
    }
}