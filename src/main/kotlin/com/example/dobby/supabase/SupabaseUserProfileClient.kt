package com.example.dobby.supabase

import com.example.dobby.dto.UserProfileCreateRequest
import com.example.dobby.dto.UserProfileResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import org.springframework.stereotype.Component


private const val USER_PROFILE_TABLE = "user_profiles"


@Component
class SupabaseUserProfileClient(
    private val supabaseClient: SupabaseClient
) {

    suspend fun findByDiscordId(discordUserId: String): UserProfileResponse? {
        return safeDbCall("find user by $discordUserId and guild") {
            supabaseClient.from(USER_PROFILE_TABLE)
                .select(
                    columns = Columns.raw("*, user_facts(*)")
                ) {
                    filter {
                        eq("discord_user_id", discordUserId)
                    }
                }
                .decodeSingleOrNull<UserProfileResponse>()
        }
    }


    suspend fun findByDiscordIdAndGuildId(discordUserId: String, guildId: String): UserProfileResponse? {
        return safeDbCall("find user by $discordUserId and guild") {
            supabaseClient.from(USER_PROFILE_TABLE)
                .select(
                    columns = Columns.raw("*, user_facts(*)")
                ) {
                    filter {
                        eq("discord_user_id", discordUserId)
                        eq("guild_id", guildId)
                    }
                }
                .decodeSingleOrNull<UserProfileResponse>()
        }
    }


    suspend fun insertNewProfile(profile: UserProfileCreateRequest): UserProfileResponse {
        return safeDbCall("insert new user profile") {
            supabaseClient.from(USER_PROFILE_TABLE)
                .insert(profile) {
                    select()
                }
                .decodeSingle<UserProfileResponse>()
        }
    }
}