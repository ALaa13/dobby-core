package com.example.dobby.service

import com.example.dobby.dto.UserProfileResponse
import com.example.dobby.exception.DobbyException
import com.example.dobby.repository.UserProfileRepository
import com.example.dobby.util.Logging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userProfileRepository: UserProfileRepository
) {

    suspend fun getCurrentUser(): UserProfileResponse {
        Logging.logInfo("Attempting to retrieve current user profile")
        val authentication = SecurityContextHolder.getContext().authentication
        val discordUserId = authentication?.name ?: throw DobbyException.ProfileNotFoundException(null, null)

        val userEntity = userProfileRepository.findProfile(discordUserId)
        return userEntity ?: throw DobbyException.ProfileNotFoundException(discordUserId, null)
    }
}