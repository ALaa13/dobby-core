package com.example.dobby.repository

import com.example.dobby.dto.user.UserProfileCreateRequest
import com.example.dobby.dto.user.UserProfileResponse
import com.example.dobby.supabase.SupabaseUserProfile
import org.springframework.stereotype.Repository

@Repository
class UserProfileRepository(
    private val supabaseUserProfile: SupabaseUserProfile,
) {
    suspend fun findProfile(
        discordUserId: String,
        guildId: String,
    ): UserProfileResponse? = supabaseUserProfile.findByDiscordIdAndGuildId(discordUserId, guildId)

    suspend fun findAllByGuildId(guildId: String): List<UserProfileResponse> =
        supabaseUserProfile.findAllByGuildId(guildId)

    suspend fun saveProfile(profile: UserProfileCreateRequest): UserProfileResponse =
        supabaseUserProfile.insertNewProfile(profile)

    suspend fun deleteAllByGuildId(guildId: String) = supabaseUserProfile.deleteAllByGuildId(guildId)

    suspend fun upsertProfiles(profiles: List<UserProfileCreateRequest>) = supabaseUserProfile.upsertProfiles(profiles)
}
