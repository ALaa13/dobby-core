package com.example.dobby.service

import com.example.dobby.config.logger
import com.example.dobby.dto.*
import com.example.dobby.exception.DobbyException
import com.example.dobby.repository.UserFactRepository
import com.example.dobby.repository.UserProfileRepository
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
        logger.info("Received remember request for user $request")
        val profile = getOrCreateProfile(
            UserProfileCreateRequest(
                request.discordUserId,
                request.guildId,
                request.displayName
            )
        )
        val fact = UserFactCreateRequest(
            profile.id,
            request.fact,
            FactSource.USER_SUBMISSION,
            80,
            20
        )
        userFactRepository.saveFact(fact)
    }

    suspend fun getFacts(discordUserId: String, guildId: String): List<UserFactResponse> {
        logger.info("Received fact request for user $discordUserId")
        return userProfileRepository.findProfile(discordUserId, guildId)?.facts ?: emptyList()
    }

    suspend fun resetFacts(discordUserId: String, guildId: String) {
        val profile =
            userProfileRepository.findProfile(discordUserId, guildId) ?: throw DobbyException.ProfileNotFoundException(
                discordUserId,
                guildId
            )
        userFactRepository.deleteFactsByProfileId(profile.id)
        logger.info("Deleted all facts for user $discordUserId")
    }
}