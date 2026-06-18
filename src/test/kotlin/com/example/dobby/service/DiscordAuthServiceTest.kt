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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

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
                        username = "SomeUser",
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

        val mockHttpClient = HttpClient(mockEngine) { configureDobbyJson() }
        authService = DiscordAuthService(mockHttpClient, jwtService, testProperties)
    }


    @Test
    fun `should generate valid discord authorization uri`() {
        val expectedUri = org.springframework.web.util.UriComponentsBuilder
            .fromUriString("https://discord.com/api/oauth2/authorize")
            .queryParam("client_id", "mock-client-id")
            .queryParam("redirect_uri", "http://localhost:8080/callback")
            .queryParam("response_type", "code")
            .queryParam("scope", "identify")
            .build()
            .toUri()

        val actualUri = authService.getDiscordLoginUri()

        assertEquals(expectedUri, actualUri)
    }

    @Test
    fun `should successfully handle login callback and return redirect uri`() = runTest {
        every { jwtService.generateJWTToken("123456", "SomeUser") } returns "mocked-jwt-cookie-or-string"

        val redirectUri = authService.handleCallbackAndGenerateRedirect("dummy-discord-code")
        val uriString = redirectUri.toString()

        assertEquals("http://localhost:3000/dashboard?token=mocked-jwt-cookie-or-string", uriString)
    }

    @Test
    fun `should handle total discord server failure`() = runTest {
        val crashEngine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val localClient = HttpClient(crashEngine) { configureDobbyJson() }
        val buggyAuthService = DiscordAuthService(localClient, jwtService, testProperties)

        val redirectUri = buggyAuthService.handleCallbackAndGenerateRedirect("any-code")

        assertEquals("http://localhost:3000/login?error=discord_auth_failed", redirectUri.toString())
    }

    @Test
    fun `should redirect to login page with invalid request error when code is null`() = runTest {
        val redirectUri = authService.handleCallbackAndGenerateRedirect(null)

        assertEquals("http://localhost:3000/login?error=invalid_request", redirectUri.toString())
    }

    @Test
    fun `should redirect to login page when discord rejects the authorization code`() = runTest {
        val badRequestEngine = MockEngine { request ->
            assertEquals("/api/v10/oauth2/token", request.url.encodedPath)
            assertEquals(HttpMethod.Post, request.method)

            respond(
                content = """{"error": "invalid_grant", "error_description": "Invalid code"}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val localClient = HttpClient(badRequestEngine) { configureDobbyJson() }
        val badAuthService = DiscordAuthService(localClient, jwtService, testProperties)

        val redirectUri = badAuthService.handleCallbackAndGenerateRedirect("expired-or-fake-code")

        assertEquals("http://localhost:3000/login?error=discord_auth_failed", redirectUri.toString())
    }

    @Test
    fun `should redirect to login page when token exchange succeeds but user profile fetch fails`() = runTest {
        val partialFailureEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/v10/oauth2/token" -> respond(
                    content = Json.encodeToString(DiscordTokenResponse(accessToken = "valid-token")),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )

                "/api/v10/users/@me" -> respondError(HttpStatusCode.InternalServerError)
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        val localClient = HttpClient(partialFailureEngine) { configureDobbyJson() }
        val badAuthService = DiscordAuthService(localClient, jwtService, testProperties)

        val redirectUri = badAuthService.handleCallbackAndGenerateRedirect("good-code")

        assertEquals("http://localhost:3000/login?error=discord_auth_failed", redirectUri.toString())
    }
}