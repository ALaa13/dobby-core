package com.example.dobby.service

import com.example.dobby.AppProperties
import com.example.dobby.crypto.CryptoUtils
import com.example.dobby.crypto.CryptoUtils.decryptToken
import com.example.dobby.dto.DiscordAccount
import com.example.dobby.dto.DiscordDashboardResponse
import com.example.dobby.queue.RedisKeyTimeout
import com.example.dobby.queue.RedisKeys
import com.example.dobby.repository.DiscordAccountRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserServiceTest {

    private val appProperties = mockk<AppProperties>()
    private val discordAccountRepository = mockk<DiscordAccountRepository>()
    private val discordApiService = mockk<DiscordApiService>()
    private val stringRedisTemplate = mockk<StringRedisTemplate>()
    private val valueOperations = mockk<ValueOperations<String, String>>()

    private lateinit var userService: UserService

    private val testUserId = "123456789"
    private val redisKey = RedisKeys.USER_PROFILE + ":" + testUserId

    @BeforeEach
    fun setUp() {
        clearAllMocks()

        // Mock the nested Redis structure Spring Template uses
        every { stringRedisTemplate.opsForValue() } returns valueOperations

        userService = UserService(
            appProperties,
            discordAccountRepository,
            discordApiService,
            stringRedisTemplate
        )

        // Mock object we're using for encryption
        mockkObject(CryptoUtils)
    }

    @Test
    fun `getCurrentUser should return cached profile immediately on cache hit`() = runTest {
        val expectedResponse = DiscordDashboardResponse(
            discordUserId = testUserId,
            displayName = "CachedUser",
            avatarUrl = "",
            managedGuilds = emptyList()
        )
        val cachedJson = Json.encodeToString(expectedResponse)

        // Stub Redis hit
        every { valueOperations.get(redisKey) } returns cachedJson

        val result = userService.getCurrentUser(testUserId)

        assertEquals(expectedResponse, result)

        // Verify database and network were bypassed entirely
        verify { discordAccountRepository wasNot Called }
        coVerify { discordApiService wasNot Called }
    }

    @Test
    fun `getCurrentUser should hit DB, decrypt, call API, and cache on cache miss`() = runTest {
        val mockAccount = mockk<DiscordAccount> {
            every { encryptedToken } returns "scrambled-crypto-string"
        }
        val expectedResponse = DiscordDashboardResponse(
            discordUserId = testUserId,
            displayName = "FreshUser",
            avatarUrl = "",
            managedGuilds = emptyList()
        )

        every { valueOperations.get(redisKey) } returns null // Cache miss
        every { appProperties.encryption.secretKey } returns "mock-32-char-encryption-key-aaa"
        every {
            decryptToken(
                "scrambled-crypto-string",
                "mock-32-char-encryption-key-aaa"
            )
        } returns "decrypted-discord-token"

        coEvery { discordAccountRepository.findByDiscordUserId(testUserId) } returns mockAccount
        coEvery { discordApiService.fetchCompleteUserProfile("decrypted-discord-token") } returns expectedResponse
        every { valueOperations.set(redisKey, any(), RedisKeyTimeout.USER_PROFILE) } just Runs

        val result = userService.getCurrentUser(testUserId)

        assertEquals(expectedResponse, result)

        // Verify it backfilled the Redis cache with a 15 min TTL
        verify(exactly = 1) {
            valueOperations.set(redisKey, Json.encodeToString(expectedResponse), RedisKeyTimeout.USER_PROFILE)
        }
    }

    @Test
    fun `getCurrentUser should throw NoSuchElementException when account missing from DB`() = runTest {
        every { valueOperations.get(redisKey) } returns null // Cache miss
        coEvery { discordAccountRepository.findByDiscordUserId(testUserId) } returns null // Database empty

        assertFailsWith<NoSuchElementException> {
            userService.getCurrentUser(testUserId)
        }

        coVerify(exactly = 0) { discordApiService.fetchCompleteUserProfile(any()) }
    }
}