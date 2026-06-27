package com.example.dobby.supabase

import com.example.dobby.dto.discord.DiscordAccount
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import org.springframework.stereotype.Component

private const val DISCORD_ACCOUNTS_TABLE = "discord_accounts"

@Component
class SupabaseDiscordAccount(
    private val supabaseClient: SupabaseClient,
) {
    suspend fun saveDiscordUser(discordUser: DiscordAccount) =
        safeDbCall("insert new discord account") {
            supabaseClient
                .from(DISCORD_ACCOUNTS_TABLE)
                .upsert(discordUser) {
                    select()
                }.decodeSingle<DiscordAccount>()
        }

    suspend fun findByDiscordUserId(discordUserId: String): DiscordAccount? =
        safeDbCall("find discord account by $discordUserId") {
            supabaseClient
                .from(DISCORD_ACCOUNTS_TABLE)
                .select(
                    columns = Columns.raw("*"),
                ) {
                    filter {
                        eq("discord_user_id", discordUserId)
                    }
                }.decodeSingleOrNull<DiscordAccount>()
        }
}
