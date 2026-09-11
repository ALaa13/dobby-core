package com.example.dobby.repository

import com.example.dobby.dto.discord.DiscordAccount
import com.example.dobby.entity.DiscordAccountEntity
import com.example.dobby.repository.r2dbc.DiscordAccountPostgresStore
import org.springframework.stereotype.Repository

@Repository
class DiscordAccountRepository(
    private val discordAccountStore: DiscordAccountPostgresStore,
) {
    suspend fun saveDiscordUser(discordUser: DiscordAccount): DiscordAccount {
        return discordAccountStore
            .upsert(
                discordUserId = discordUser.discordUserId,
                encryptedToken = discordUser.encryptedToken,
            ).toDto()
    }

    suspend fun findByDiscordUserId(discordUserId: String): DiscordAccount? {
        return discordAccountStore
            .findByDiscordUserId(discordUserId)
            ?.toDto()
    }

    private fun DiscordAccountEntity.toDto(): DiscordAccount {
        return DiscordAccount(
            discordUserId = discordUserId,
            encryptedToken = encryptedToken,
        )
    }
}
