package com.example.dobby.supabase

import com.example.dobby.dto.UserProfileCreateRequest
import com.example.dobby.dto.UserProfileResponse
import com.example.dobby.util.Logging
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import org.springframework.stereotype.Component


private const val USER_PROFILE_TABLE = "user_profiles"


@Component
class SupabaseUserProfileClient(
    private val supabaseClient: SupabaseClient
) {
    suspend fun findByDiscordIdAndGuildId(discordUserId: String, guildId: String): UserProfileResponse? {
        return try {
            supabaseClient.from(USER_PROFILE_TABLE)
                .select {
                    filter {
                        eq("discord_user_id", discordUserId)
                        eq("guild_id", guildId)
                    }
                }
                .decodeSingleOrNull<UserProfileResponse>()
        } catch (e: Exception) {
            Logging.logError("Error fetching user profile: ${e.message}")
            null
        }
    }


    suspend fun insertNewProfile(profile: UserProfileCreateRequest): UserProfileResponse {
        try {
            return supabaseClient.from(USER_PROFILE_TABLE)
                .insert(profile) {
                    select()
                }
                .decodeSingle<UserProfileResponse>()
        } catch (e: Exception) {
            Logging.logError("Error inserting user profile: ${e.message}")
            throw e
        }
    }
}