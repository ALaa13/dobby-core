package com.example.dobby.service

import com.example.dobby.config.SerializationConfig
import com.example.dobby.config.configureDobbyJson
import com.example.dobby.dto.discord.DiscordGuild
import com.example.dobby.dto.discord.DiscordTokenResponse
import com.example.dobby.dto.discord.DiscordUser
import com.example.dobby.exception.DobbyException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DiscordApiServiceTest {
    val json = SerializationConfig().kotlinxSerializationJson()

    @Test
    fun `exchangeCodeForToken should return response on valid status`() =
        runTest {
            val mockEngine =
                MockEngine { _ ->
                    val tokenResponse =
                        DiscordTokenResponse(
                            accessToken = "mock-token-123",
                            tokenType = "Bearer",
                            expiresIn = 604800,
                            refreshToken = "mock-refresh-token",
                            scope = "identify",
                        )
                    respond(
                        content = Json.encodeToString(tokenResponse),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine) { configureDobbyJson(json) }
            val apiService = DiscordApiService(client)

            val result = apiService.exchangeCodeForToken("code", "id", "secret", "uri")
            assertEquals("mock-token-123", result.accessToken)
        }

    @Test
    fun `exchangeCodeForToken should throw AuthorizationException when Discord returns HTTP error`() =
        runTest {
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = """{"error": "invalid_grant"}""",
                        status = HttpStatusCode.BadRequest,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine) { configureDobbyJson(json) }
            val apiService = DiscordApiService(client)

            assertFailsWith<DobbyException.AuthorizationException> {
                apiService.exchangeCodeForToken("bad-code", "id", "secret", "uri")
            }
        }

    @Test
    fun `getUserProfile should throw AuthorizationException when token is expired or invalid`() =
        runTest {
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = """{"message": "401: Unauthorized"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine) { configureDobbyJson(json) }
            val apiService = DiscordApiService(client)

            assertFailsWith<DobbyException.AuthorizationException> {
                apiService.getUserProfile("expired-token")
            }
        }

    @Test
    fun `fetchCompleteUserProfile should filter guilds using bitwise admin flags`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    when (request.url.encodedPath) {
                        "/api/v10/users/@me" ->
                            respond(
                                content = Json.encodeToString(DiscordUser("123456", "SomeUser", "avatar-hash")),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )

                        "/api/v10/users/@me/guilds" -> {
                            val guilds =
                                listOf(
                                    DiscordGuild(
                                        id = "1",
                                        name = "Admin Guild",
                                        icon = null,
                                        owner = false,
                                        permissions = "8",
                                    ), // 0x8 Admin
                                    DiscordGuild(
                                        id = "2",
                                        name = "Regular Guild",
                                        icon = null,
                                        owner = false,
                                        permissions = "0",
                                    ),
                                    DiscordGuild(
                                        id = "3",
                                        name = "Owner Guild",
                                        icon = null,
                                        owner = true,
                                        permissions = "0",
                                    ),
                                )
                            respond(
                                content = Json.encodeToString(guilds),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }

                        else -> respondError(HttpStatusCode.NotFound)
                    }
                }
            val client = HttpClient(mockEngine) { configureDobbyJson(json) }
            val apiService = DiscordApiService(client)

            val dashboardData = apiService.fetchCompleteUserProfile("valid-token")

            assertEquals(2, dashboardData.managedGuilds.size) // Admin and Owner
            assertTrue(dashboardData.managedGuilds.any { it.name == "Admin Guild" })
            assertTrue(dashboardData.managedGuilds.any { it.name == "Owner Guild" })
        }

    @Test
    fun `fetchCompleteUserProfile should use default embed avatar when avatar hash is null`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    when (request.url.encodedPath) {
                        "/api/v10/users/@me" ->
                            respond(
                                // User has NO avatar hash set
                                content = Json.encodeToString(DiscordUser("123456", "NoAvatarUser", avatar = null)),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )

                        "/api/v10/users/@me/guilds" ->
                            respond(
                                content = "[]",
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )

                        else -> respondError(HttpStatusCode.NotFound)
                    }
                }
            val client = HttpClient(mockEngine) { configureDobbyJson(json) }
            val apiService = DiscordApiService(client)

            val result = apiService.fetchCompleteUserProfile("valid-token")

            // Asserts fallback avatar URL mapping logic works smoothly
            assertEquals("https://cdn.discordapp.com/embed/avatars/0.png", result.avatarUrl)
        }

    @Test
    fun `fetchCompleteUserProfile should include complex bitwise combinations containing admin permissions`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    when (request.url.encodedPath) {
                        "/api/v10/users/@me" ->
                            respond(
                                content = Json.encodeToString(DiscordUser("123", "User", "hash")),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )

                        "/api/v10/users/@me/guilds" -> {
                            val guilds =
                                listOf(
                                    // 1,048,584 in decimal = 0x100008 in hex (contains 0x8 Admin bit + other flags)
                                    DiscordGuild(
                                        id = "9",
                                        name = "Complex Permissions Guild",
                                        icon = null,
                                        owner = false,
                                        permissions = "1048584",
                                    ),
                                )
                            respond(
                                content = Json.encodeToString(guilds),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }

                        else -> respondError(HttpStatusCode.NotFound)
                    }
                }
            val client = HttpClient(mockEngine) { configureDobbyJson(json) }
            val apiService = DiscordApiService(client)

            val result = apiService.fetchCompleteUserProfile("valid-token")

            assertEquals(1, result.managedGuilds.size)
            assertEquals("Complex Permissions Guild", result.managedGuilds.first().name)
        }

    @Test
    fun `fetchCompleteUserProfile should handle unparseable or null permissions string gracefully without crashing`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    when (request.url.encodedPath) {
                        "/api/v10/users/@me" ->
                            respond(
                                content = Json.encodeToString(DiscordUser("123", "User", "hash")),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )

                        "/api/v10/users/@me/guilds" -> {
                            val guilds =
                                listOf(
                                    DiscordGuild(
                                        id = "88",
                                        name = "Broken Guild",
                                        icon = null,
                                        owner = false,
                                        permissions = "not-a-number",
                                    ),
                                )
                            respond(
                                content = Json.encodeToString(guilds),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }

                        else -> respondError(HttpStatusCode.NotFound)
                    }
                }
            val client = HttpClient(mockEngine) { configureDobbyJson(json) }
            val apiService = DiscordApiService(client)

            val result = apiService.fetchCompleteUserProfile("valid-token")

            // It shouldn't crash with a NumberFormatException; it should handle it and filter out the broken guild cleanly
            assertTrue(result.managedGuilds.isEmpty())
        }
}
