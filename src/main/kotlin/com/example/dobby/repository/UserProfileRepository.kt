package com.example.dobby.repository

import com.example.dobby.dto.user.UserProfileCreateRequest
import com.example.dobby.dto.user.UserProfileResponse
import com.example.dobby.supabase.SupabaseUserProfileClient
import org.springframework.stereotype.Repository

@Repository
class UserProfileRepository(
    private val supabaseUserProfileClient: SupabaseUserProfileClient
) {
    suspend fun findProfile(discordUserId: String, guildId: String): UserProfileResponse? {
        return supabaseUserProfileClient.findByDiscordIdAndGuildId(discordUserId, guildId)
    }

    suspend fun findAllByGuildId(guildId: String): List<UserProfileResponse> {
        return supabaseUserProfileClient.findAllByGuildId(guildId)
    }

    suspend fun saveProfile(profile: UserProfileCreateRequest): UserProfileResponse {
        return supabaseUserProfileClient.insertNewProfile(profile)
    }

    suspend fun deleteAllByGuildId(guildId: String) {
        return supabaseUserProfileClient.deleteAllByGuildId(guildId)
    }

    suspend fun upsertProfiles(profiles: List<UserProfileCreateRequest>) {
        return supabaseUserProfileClient.upsertProfiles(profiles)
    }
}