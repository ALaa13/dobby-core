package com.example.dobby.service

import com.example.dobby.AppProperties
import com.example.dobby.crypto.CryptoUtils
import com.example.dobby.crypto.CryptoUtils.decryptToken
import com.example.dobby.dto.discord.DiscordAccount
import com.example.dobby.dto.discord.DiscordDashboardResponse
import com.example.dobby.queue.RedisKeyTimeout
import com.example.dobby.queue.RedisKeys
import com.example.dobby.repository.DiscordAccountRepository
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdminUserServiceTest {
    private val appProperties = mockk<AppProperties>()
    private val discordAccountRepository = mockk<DiscordAccountRepository>()
    private val discordApiService = mockk<DiscordApiService>()
    private val stringRedisTemplate = mockk<StringRedisTemplate>()
    private val valueOperations = mockk<ValueOperations<String, String>>()

    private lateinit var adminUserService: AdminUserService

    private val testUserId = "123456789"
    private val redisKey = RedisKeys.USER_PROFILE + ":" + testUserId

    @BeforeEach
    fun setUp() {
        clearAllMocks()

        every { stringRedisTemplate.opsForValue() } returns valueOperations

        adminUserService =
            AdminUserService(
                appProperties,
                discordAccountRepository,
                discordApiService,
                stringRedisTemplate,
            )

        mockkObject(CryptoUtils)
    }

    @Test
    fun `getCurrentUser should return cached profile immediately on cache hit`() {
        runTest {
            val expectedResponse =
                DiscordDashboardResponse(
                    discordUserId = testUserId,
                    displayName = "CachedUser",
                    avatarUrl = "",
                    managedGuilds = emptyList(),
                )
            val cachedJson = Json.encodeToString(expectedResponse)

            every { valueOperations.get(redisKey) } returns cachedJson

            val result = adminUserService.getCurrentUser(testUserId)

            assertEquals(expectedResponse, result)

            // A cache hit must not expose the stored token or trigger an external Discord request.
            verify { discordAccountRepository wasNot Called }
            coVerify { discordApiService wasNot Called }
        }
    }

    @Test
    fun `getCurrentUser should hit DB, decrypt, call API, and cache on cache miss`() {
        runTest {
            val mockAccount =
                mockk<DiscordAccount> {
                    every { encryptedToken } returns "scrambled-crypto-string"
                }
            val expectedResponse =
                DiscordDashboardResponse(
                    discordUserId = testUserId,
                    displayName = "FreshUser",
                    avatarUrl = "",
                    managedGuilds = emptyList(),
                )

            every { valueOperations.get(redisKey) } returns null
            every { appProperties.encryption.secretKey } returns "mock-32-char-encryption-key-aaa"
            every {
                decryptToken(
                    "scrambled-crypto-string",
                    "mock-32-char-encryption-key-aaa",
                )
            } returns "decrypted-discord-token"

            coEvery { discordAccountRepository.findByDiscordUserId(testUserId) } returns mockAccount
            coEvery { discordApiService.fetchCompleteUserProfile("decrypted-discord-token") } returns expectedResponse
            every { valueOperations.set(redisKey, any(), RedisKeyTimeout.USER_PROFILE) } just Runs

            val result = adminUserService.getCurrentUser(testUserId)

            assertEquals(expectedResponse, result)

            verify(exactly = 1) {
                valueOperations.set(redisKey, Json.encodeToString(expectedResponse), RedisKeyTimeout.USER_PROFILE)
            }
        }
    }

    @Test
    fun `getCurrentUser should throw NoSuchElementException when account missing from DB`() {
        runTest {
            every { valueOperations.get(redisKey) } returns null
            coEvery { discordAccountRepository.findByDiscordUserId(testUserId) } returns null

            assertFailsWith<NoSuchElementException> {
                adminUserService.getCurrentUser(testUserId)
            }

            coVerify(exactly = 0) { discordApiService.fetchCompleteUserProfile(any()) }
        }
    }
}
