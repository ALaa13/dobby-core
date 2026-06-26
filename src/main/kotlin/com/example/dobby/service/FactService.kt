package com.example.dobby.service

import com.example.dobby.config.log
import com.example.dobby.dto.discord.DiscordFactRequest
import com.example.dobby.dto.fact.FactSource
import com.example.dobby.dto.fact.UserFactCreateRequest
import com.example.dobby.dto.fact.UserFactResponse
import com.example.dobby.dto.user.UserProfileCreateRequest
import com.example.dobby.dto.user.UserProfileResponse
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

    suspend fun saveUserFact(request: DiscordFactRequest) {
        log.info("Received remember request for user $request")
        val profile = getOrCreateProfile(
            UserProfileCreateRequest(
                request.discordUserId,
                request.guildId,
                request.displayName,
                request.avatarHash
            )
        )
        val fact = UserFactCreateRequest(
            profile.id,
            request.fact,
            FactSource.USER_SUBMISSION
        )
        userFactRepository.saveFact(fact)
    }

    suspend fun getUserFacts(discordUserId: String, guildId: String): List<UserFactResponse> {
        log.info("Received fact request for user $discordUserId")
        return userProfileRepository.findProfile(discordUserId, guildId)?.facts ?: emptyList()
    }

    suspend fun getAllFactsByGuild(guildId: String): List<UserProfileResponse> {
        log.info("Received fact request for all users in guild $guildId")
        return userProfileRepository.findAllByGuildId(guildId)
    }

    suspend fun deleteUserFact(factId: String) {
        log.info("Received fact delete request for fact $factId")
        userFactRepository.deleteFactById(factId)
    }

    suspend fun resetUserFacts(discordUserId: String, guildId: String) {
        val profile =
            userProfileRepository.findProfile(discordUserId, guildId) ?: throw DobbyException.ProfileNotFoundException(
                discordUserId,
                guildId
            )
        userFactRepository.deleteFactsByProfileId(profile.id)
        log.info("Deleted all facts for user $discordUserId")
    }

    suspend fun resetGuildFacts(guildId: String) {
        log.info("Resetting all guild facts for guild $guildId")
        userProfileRepository.deleteAllByGuildId(guildId)
    }
}