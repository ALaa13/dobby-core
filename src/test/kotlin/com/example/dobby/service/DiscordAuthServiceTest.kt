package com.example.dobby.service

import com.example.dobby.AppProperties
import com.example.dobby.dto.discord.DiscordTokenResponse
import com.example.dobby.dto.discord.DiscordUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DiscordAuthServiceTest {

    private lateinit var testProperties: AppProperties
    private lateinit var authService: DiscordAuthService

    private val discordApiService = mockk<DiscordApiService>()
    private val jwtService = mockk<JWTService>()
    private val discordAccountService = mockk<DiscordAccountService>(relaxed = true)

    @BeforeEach
    fun setUp() {
        testProperties = AppProperties().apply {
            frontend.url = "http://localhost:3000"
            discord.clientId = "mock-client-id"
            discord.clientSecret = "mock-client-secret"
            discord.redirectUri = "http://localhost:8080/callback"
        }
        authService = DiscordAuthService(testProperties, discordApiService, jwtService, discordAccountService)
    }

    @Test
    fun `should generate valid discord authorization uri`() {
        val actualUri = authService.getDiscordLoginUri()

        assertTrue(actualUri.toString().contains("client_id=mock-client-id"))
        assertTrue(actualUri.toString().contains("scope=identify%20guilds"))
    }

    @Test
    fun `should successfully handle login callback and return redirect uri`() = runTest {
        val mockTokenResponse = DiscordTokenResponse(accessToken = "mock-access-token")
        val mockUserResponse = DiscordUser(id = "123456", username = "SomeUser", avatar = null)

        coEvery { discordApiService.exchangeCodeForToken(any(), any(), any(), any()) } returns mockTokenResponse
        coEvery { discordApiService.getUserProfile("mock-access-token") } returns mockUserResponse
        every { jwtService.generateJWTToken("123456", "SomeUser") } returns "mocked-jwt"

        val redirectUri = authService.handleCallbackAndGenerateRedirect("dummy-code")

        assertEquals("http://localhost:3000/dashboard?token=mocked-jwt", redirectUri.toString())
        coVerify(exactly = 1) { discordAccountService.saveDiscordAccount("123456", "mock-access-token") }
    }

    @Test
    fun `should redirect to login page with invalid request error when code is null`() = runTest {
        val redirectUri = authService.handleCallbackAndGenerateRedirect(null)
        assertEquals("http://localhost:3000/login?error=invalid_request", redirectUri.toString())
    }

    @Test
    fun `should redirect to login page when api execution fails`() = runTest {
        coEvery {
            discordApiService.exchangeCodeForToken(any(), any(), any(), any())
        } throws RuntimeException("Discord servers exploded")

        val redirectUri = authService.handleCallbackAndGenerateRedirect("any-code")
        assertEquals("http://localhost:3000/login?error=discord_auth_failed", redirectUri.toString())
    }
}