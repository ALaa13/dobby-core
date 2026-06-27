package com.example.dobby.supabase

import com.example.dobby.dto.user.UserProfileCreateRequest
import com.example.dobby.dto.user.UserProfileResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import org.springframework.stereotype.Component

private const val USER_PROFILE_TABLE = "user_profiles"

@Component
class SupabaseUserProfile(
    private val supabaseClient: SupabaseClient,
) {
    suspend fun findAllByGuildId(guildId: String): List<UserProfileResponse> =
        safeDbCall("find all users and facts by guild $guildId") {
            supabaseClient
                .from(USER_PROFILE_TABLE)
                .select(
                    columns = Columns.raw("*, user_facts(*)"),
                ) {
                    filter {
                        eq("guild_id", guildId)
                    }
                }.decodeList<UserProfileResponse>()
        }

    suspend fun findByDiscordIdAndGuildId(
        discordUserId: String,
        guildId: String,
    ): UserProfileResponse? =
        safeDbCall("find user by $discordUserId and guild") {
            supabaseClient
                .from(USER_PROFILE_TABLE)
                .select(
                    columns = Columns.raw("*, user_facts(*)"),
                ) {
                    filter {
                        eq("discord_user_id", discordUserId)
                        eq("guild_id", guildId)
                    }
                }.decodeSingleOrNull<UserProfileResponse>()
        }

    suspend fun insertNewProfile(profile: UserProfileCreateRequest): UserProfileResponse =
        safeDbCall("insert new user profile") {
            supabaseClient
                .from(USER_PROFILE_TABLE)
                .insert(profile) {
                    select()
                }.decodeSingle<UserProfileResponse>()
        }

    suspend fun deleteAllByGuildId(guildId: String) =
        safeDbCall("delete all users and facts by guild $guildId") {
            supabaseClient
                .from(USER_PROFILE_TABLE)
                .delete {
                    filter {
                        eq("guild_id", guildId)
                    }
                }
        }

    suspend fun upsertProfiles(profiles: List<UserProfileCreateRequest>) {
        if (profiles.isEmpty()) return

        safeDbCall("batch upsert user profiles") {
            supabaseClient.from(USER_PROFILE_TABLE).upsert(profiles) {
                // Tells Supabase to overwrite fields on unique constraint conflict
                onConflict = "discord_user_id,guild_id"
            }
        }
    }
}
