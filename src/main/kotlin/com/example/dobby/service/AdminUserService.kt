package com.example.dobby.service

import com.example.dobby.AppProperties
import com.example.dobby.config.log
import com.example.dobby.crypto.CryptoUtils.decryptToken
import com.example.dobby.dto.discord.DiscordDashboardResponse
import com.example.dobby.queue.RedisKeyTimeout
import com.example.dobby.queue.RedisKeys
import com.example.dobby.repository.DiscordAccountRepository
import kotlinx.serialization.json.Json
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class AdminUserService(
    private val appProperties: AppProperties,
    private val discordAccountRepository: DiscordAccountRepository,
    private val discordApiService: DiscordApiService,
    private val stringRedisTemplate: StringRedisTemplate,
) {
    suspend fun getCurrentUser(userIdFromJwt: String): DiscordDashboardResponse {
        log.info("Attempting to retrieve current user profile")
        val redisKey = RedisKeys.USER_PROFILE + ":" + userIdFromJwt

        // A cache hit avoids decrypting the OAuth token and making an external Discord request.
        val cachedProfileJson = stringRedisTemplate.opsForValue().get(redisKey)
        if (cachedProfileJson != null) {
            log.info("Cache hit for user profile: $userIdFromJwt")
            return Json.decodeFromString<DiscordDashboardResponse>(cachedProfileJson)
        }

        val account =
            discordAccountRepository.findByDiscordUserId(userIdFromJwt)
                ?: throw NoSuchElementException("No encrypted account record found for user $userIdFromJwt")

        // Delay decryption until refresh is required so plaintext credentials have the narrowest practical lifetime.
        val decryptedToken =
            decryptToken(
                account.encryptedToken,
                appProperties.encryption.secretKey,
            )

        val freshProfile = discordApiService.fetchCompleteUserProfile(decryptedToken)

        // Bound cache lifetime so Discord-side profile and guild changes are eventually observed.
        val jsonPayload = Json.encodeToString(freshProfile)
        log.info("Caching user profile: $userIdFromJwt")
        stringRedisTemplate.opsForValue().set(redisKey, jsonPayload, RedisKeyTimeout.USER_PROFILE)

        return freshProfile
    }
}
