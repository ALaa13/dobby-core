package com.example.dobby.service

import com.example.dobby.config.log
import com.example.dobby.repository.UserProfileRepository
import org.springframework.stereotype.Service

@Service
class GuildManagementService(
    private val userProfileRepository: UserProfileRepository
) {

    suspend fun resetGuildFacts(guildId: String) {
        log.info("Resetting all guild facts for guild $guildId")
        userProfileRepository.deleteAllByGuildId(guildId)
    }
}