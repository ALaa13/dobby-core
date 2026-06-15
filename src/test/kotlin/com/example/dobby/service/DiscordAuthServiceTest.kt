package com.example.dobby.service

import com.example.dobby.AppProperties
import com.example.dobby.config.configureDobbyJson
import com.example.dobby.dto.DiscordTokenResponse
import com.example.dobby.dto.DiscordUser
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DiscordAuthServiceTest {

    private lateinit var testProperties: AppProperties
    private lateinit var authService: DiscordAuthService
    private val jwtService = mockk<JWTService>()

    @BeforeEach
    fun setUp() {

        testProperties = AppProperties().apply {
            frontend.url = "http://localhost:3000"
            discord.clientId = "mock-client-id"
            discord.clientSecret = "mock-client-secret"
            discord.redirectUri = "http://localhost:8080/callback"
        }

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/v10/oauth2/token" -> {
                    val tokenResponse = DiscordTokenResponse(
                        accessToken = "mock-token-123",
                        tokenType = "Bearer",
                        expiresIn = 604800,
                        refreshToken = "mock-refresh-token",
                        scope = "identify"
                    )

                    respond(
                        content = Json.encodeToString(tokenResponse),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }

                "/api/v10/users/@me" -> {
                    val userResponse = DiscordUser(
                        id = "123456",
                        username = "Alaa",
                        discriminator = "0000",
                        avatar = "mock-avatar-hash"
                    )

                    respond(
                        content = Json.encodeToString(userResponse),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }

                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        val mockHttpClient = HttpClient(mockEngine) {
            configureDobbyJson()
        }
        authService = DiscordAuthService(
            httpClient = mockHttpClient,
            jwtService = jwtService,
            appProperties = testProperties
        )
    }


    @Test
    fun `should successfully handle login callback and return redirect uri`() = runTest {
        every { jwtService.generateJWTToken("123456", "Alaa") } returns "mocked-jwt-cookie-or-string"

        val redirectUri = authService.handleCallbackAndGenerateRedirect("dummy-discord-code")
        val uriString = redirectUri.toString()

        assertTrue(uriString.contains("http://localhost:3000"))
        assertTrue(uriString.contains("mocked-jwt-cookie-or-string"))
    }

    @Test
    fun `should handle total discord server failure`() = runTest {
        val crashEngine = MockEngine { _ ->
            respondError(HttpStatusCode.InternalServerError)
        }
        val localClient = HttpClient(crashEngine) { configureDobbyJson() }

        val buggyAuthService = DiscordAuthService(
            httpClient = localClient,
            jwtService = jwtService,
            appProperties = testProperties
        )
        val redirectUri = buggyAuthService.handleCallbackAndGenerateRedirect("any-code")
        val uriString = redirectUri.toString()
        assertTrue(uriString.contains("http://localhost:3000/login"))
        assertTrue(uriString.contains("error=discord_auth_failed"))
    }
}