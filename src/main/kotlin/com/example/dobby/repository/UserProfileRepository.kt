package com.example.dobby.repository

import com.example.dobby.db.SupabaseUserProfileClient
import com.example.dobby.dto.UserProfileCreateRequest
import com.example.dobby.dto.UserProfileResponse
import org.springframework.stereotype.Repository

@Repository
class UserProfileRepository(
    private val supabaseUserProfileClient: SupabaseUserProfileClient
) {
    suspend fun findByDiscordIdAndGuildId(discordUserId: String, guildId: String): UserProfileResponse? {
        return supabaseUserProfileClient.findByDiscordIdAndGuildId(discordUserId, guildId)
    }


    suspend fun insertNewProfile(profile: UserProfileCreateRequest): UserProfileResponse {
        return supabaseUserProfileClient.insertNewProfile(profile)
    }
}