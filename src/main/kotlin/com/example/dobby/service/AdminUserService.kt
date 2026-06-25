package com.example.dobby.service

import com.example.dobby.AppProperties
import com.example.dobby.config.log
import com.example.dobby.crypto.CryptoUtils.decryptToken
import com.example.dobby.dto.DiscordDashboardResponse
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
    private val guildManagementService: GuildManagementService
) {

    suspend fun getCurrentUser(userIdFromJwt: String): DiscordDashboardResponse {
        log.info("Attempting to retrieve current user profile")
        val redisKey = RedisKeys.USER_PROFILE + ":" + userIdFromJwt

        // Check Redis Cache First
        val cachedProfileJson = stringRedisTemplate.opsForValue().get(redisKey)
        if (cachedProfileJson != null) {
            // Cache hit! Return the data instantly without hitting Supabase or Discord
            log.info("Cache hit for user profile: $userIdFromJwt")
            return Json.decodeFromString<DiscordDashboardResponse>(cachedProfileJson)
        }

        // Cache Miss: Grab the encrypted account record from Supabase
        val account = discordAccountRepository.findByDiscordUserId(userIdFromJwt)
            ?: throw NoSuchElementException("No encrypted account record found for user $userIdFromJwt")

        // Decrypt the access token using your AES-256 function
        val decryptedToken = decryptToken(
            account.encryptedToken,
            appProperties.encryption.secretKey
        )

        // Hit Discord's API via your clean, centralized Ktor service
        val freshProfile = discordApiService.fetchCompleteUserProfile(decryptedToken)

        // Serialize the response to JSON and store it in Redis with a 15-minute TTL
        val jsonPayload = Json.encodeToString(freshProfile)
        log.info("Caching user profile: $userIdFromJwt")
        stringRedisTemplate.opsForValue().set(redisKey, jsonPayload, RedisKeyTimeout.USER_PROFILE)

        return freshProfile
    }

    suspend fun purgeGuildFacts(guildId: String) {
        guildManagementService.resetGuildFacts(guildId)
    }
}