package com.example.dobby.service

import com.example.dobby.AppProperties
import com.example.dobby.config.log
import com.example.dobby.crypto.CryptoUtils.encryptToken
import com.example.dobby.dto.discord.DiscordAccount
import com.example.dobby.repository.DiscordAccountRepository
import org.springframework.stereotype.Service

@Service
class DiscordAccountService(
    private val appProperties: AppProperties,
    private val discordAccountRepository: DiscordAccountRepository,
) {
    suspend fun saveDiscordAccount(
        discordUserId: String,
        rawAccessToken: String,
    ) {
        val encryptionKey = appProperties.encryption.secretKey

        val encryptedTokenBase64 = encryptToken(rawAccessToken, encryptionKey)
        val account =
            DiscordAccount(
                discordUserId,
                encryptedTokenBase64,
            )
        log.info("Saving Discord account for user ID: $discordUserId with encrypted token: $encryptedTokenBase64")
        discordAccountRepository.saveDiscordUser(account)
    }
}
