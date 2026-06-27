package com.example.dobby.service

import com.example.dobby.AppProperties
import com.example.dobby.crypto.CryptoUtils
import com.example.dobby.crypto.CryptoUtils.encryptToken
import com.example.dobby.dto.discord.DiscordAccount
import com.example.dobby.repository.DiscordAccountRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DiscordAccountServiceTest {
    private val appProperties = mockk<AppProperties>()
    private val discordAccountRepository = mockk<DiscordAccountRepository>(relaxed = true)

    private lateinit var accountService: DiscordAccountService

    private val testUserId = "user-123456"
    private val testRawToken = "gho_rawDiscordToken789"
    private val testSecretKey = "super-secret-32-char-aes-key-abc"
    private val mockedEncryptedBase64 = "scrambledBase64String=="

    @BeforeEach
    fun setUp() {
        clearAllMocks()

        accountService = DiscordAccountService(appProperties, discordAccountRepository)

        // Mock the object we're using for encryption
        mockkObject(CryptoUtils)
    }

    @AfterEach
    fun tearDown() {
        // Clean up static mocks
        unmockkObject(CryptoUtils)
    }

    @Test
    fun `saveDiscordAccount should encrypt token and persist record successfully`() =
        runTest {
            val mockSavedEntity =
                DiscordAccount(
                    discordUserId = testUserId,
                    encryptedToken = mockedEncryptedBase64,
                )

            every { appProperties.encryption.secretKey } returns testSecretKey
            every { encryptToken(testRawToken, testSecretKey) } returns mockedEncryptedBase64

            // Slot to capture the exact entity sent to the repository
            val accountSlot = slot<DiscordAccount>()
            coEvery { discordAccountRepository.saveDiscordUser(capture(accountSlot)) } returns mockSavedEntity

            accountService.saveDiscordAccount(testUserId, testRawToken)

            verify(exactly = 1) { encryptToken(testRawToken, testSecretKey) }

            val savedAccount = accountSlot.captured
            assertEquals(testUserId, savedAccount.discordUserId)
            assertEquals(mockedEncryptedBase64, savedAccount.encryptedToken)

            coVerify(exactly = 1) { discordAccountRepository.saveDiscordUser(any()) }
        }

    @Test
    fun `saveDiscordAccount should bubble up encryption runtime exceptions and skip DB updates`() =
        runTest {
            every { appProperties.encryption.secretKey } returns testSecretKey
            every { encryptToken(any(), any()) } throws IllegalArgumentException("Invalid key block size")

            try {
                accountService.saveDiscordAccount(testUserId, testRawToken)
            } catch (e: Exception) {
                // Verify it was our specific exception that bubbled up
                assertEquals("Invalid key block size", e.message)
            }

            // Critically assert that the database save step was completely skipped/aborted!
            coVerify(exactly = 0) { discordAccountRepository.saveDiscordUser(any()) }
        }
}
