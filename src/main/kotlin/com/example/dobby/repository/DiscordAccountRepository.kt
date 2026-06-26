package com.example.dobby.repository

import com.example.dobby.dto.discord.DiscordAccount
import com.example.dobby.supabase.SupabaseDiscordAccount
import org.springframework.stereotype.Repository


@Repository
class DiscordAccountRepository(
    private val supabaseDiscordUAccount: SupabaseDiscordAccount
) {
    suspend fun saveDiscordUser(discordUser: DiscordAccount) {
        return supabaseDiscordUAccount.saveDiscordUser(discordUser)
    }

    suspend fun findByDiscordUserId(discordUserId: String): DiscordAccount? {
        return supabaseDiscordUAccount.findByDiscordUserId(discordUserId)
    }
}