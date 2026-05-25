package com.example.dobby.repository

import com.example.dobby.dto.UserProfileCreateRequest
import com.example.dobby.dto.UserProfileResponse
import com.example.dobby.supabase.SupabaseUserProfileClient
import org.springframework.stereotype.Repository

@Repository
class UserProfileRepository(
    private val supabaseUserProfileClient: SupabaseUserProfileClient
) {
    suspend fun findProfile(discordUserId: String, guildId: String): UserProfileResponse? {
        return supabaseUserProfileClient.findByDiscordIdAndGuildId(discordUserId, guildId)
    }

    suspend fun saveProfile(profile: UserProfileCreateRequest): UserProfileResponse {
        return supabaseUserProfileClient.insertNewProfile(profile)
    }
}