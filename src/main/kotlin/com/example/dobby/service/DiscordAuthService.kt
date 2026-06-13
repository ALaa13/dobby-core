package com.example.dobby.service

import com.example.dobby.dto.DiscordTokenResponse
import com.example.dobby.dto.DiscordUser
import com.example.dobby.exception.DobbyException
import com.example.dobby.util.logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.URLEncoder

@Service
class DiscordAuthService(
    private val httpClient: HttpClient,
    private val jwtService: JWTService,
    @Value($$"${discord.client.id}") private val clientId: String,
    @Value($$"${discord.client.secret}") private val clientSecret: String,
    @Value($$"${discord.redirect.uri}") private val redirectUri: String,
    @Value($$"${frontend.url}") private val frontendUrl: String
) {

    companion object {
        private const val DISCORD_AUTH_URL = "https://discord.com/api/oauth2/authorize"
        private const val DISCORD_USER_URL = "https://discord.com/api/v10/users/@me"
        private const val DISCORD_TOKEN_URL = "https://discord.com/api/v10/oauth2/token"
        private const val FRONT_END_REDIRECT_DESTINATION = "/dashboard"
        private const val FRONT_END_LOGIN_PAGE = "/login"
    }

    // Generates the clean URI for the controller to redirect to
    fun getDiscordLoginUri(): URI {
        val url = DISCORD_AUTH_URL +
                "?client_id=$clientId" +
                "&redirect_uri=${URLEncoder.encode(redirectUri, "UTF-8")}" +
                "&response_type=code" +
                "&scope=identify"
        val uri = URI.create(url)
        logger.info("Generated Discord login URI: $uri")
        return uri
    }

    // Handles the heavy exchange logic and returns the final destination URI
    suspend fun handleCallbackAndGenerateRedirect(code: String?): URI {
        return try {
            if (code == null) {
                throw DobbyException.InvalidAuthenticationRequestException("Missing authorization code in callback request")
            }
            val tokenResponse = getTokenFromDiscordOAuth(code)
            val accessToken = tokenResponse.accessToken
                ?: throw DobbyException.AuthorizationException("Failed to retrieve access token from Discord")

            val userResponse = getUserDataFromDiscordToken(accessToken)
            val discordUserId = userResponse.id
            val username = userResponse.username ?: "Unknown User"

            val jwtToken = jwtService.generateJWTToken(discordUserId, username)
            buildRedirectUri(frontendUrl + FRONT_END_REDIRECT_DESTINATION, jwtToken)
        } catch (e: DobbyException.InvalidAuthenticationRequestException) {
            logger.warn("Authentication request rejected: ${e.message}")
            buildRedirectUri(frontendUrl + FRONT_END_LOGIN_PAGE, "error=invalid_request")
        } catch (e: Exception) {
            logger.error("OAuth handshake failed due to a severe system error", e)
            buildRedirectUri(frontendUrl + FRONT_END_LOGIN_PAGE, "error=discord_auth_failed")
        }
    }

    /*** Helper functions ***/

    private suspend fun getTokenFromDiscordOAuth(code: String): DiscordTokenResponse {
        return try {
            logger.info("Exchanging authorization code for access token with Discord")
            logger.info("Requesting token with code: $code, clientId: $clientId, redirectUri: $redirectUri")
            httpClient.post(DISCORD_TOKEN_URL) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(Parameters.build {
                    append("client_id", clientId)
                    append("client_secret", clientSecret)
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("redirect_uri", redirectUri)
                }))
            }.body()
        } catch (e: CancellationException) {
            throw e // Let coroutine cancellation structure pass through unhindered!
        } catch (e: Exception) {
            logger.error("Failed to exchange authorization code with Discord", e)
            throw DobbyException.AuthorizationException("Failed to exchange authorization code with Discord", e)
        }
    }

    private suspend fun getUserDataFromDiscordToken(accessToken: String): DiscordUser {
        return try {
            httpClient.get(DISCORD_USER_URL) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            }.body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to exchange authorization code with Discord", e)
            throw DobbyException.AuthorizationException("Failed to fetch user profile data from Discord", e)
        }
    }

    private fun buildRedirectUri(baseUrl: String, token: String): URI {
        val delimiter = if (baseUrl.contains("?")) "&" else "?"
        val uri = URI.create("$baseUrl${delimiter}token=$token")
        logger.info("Generated redirect: $uri")
        return uri
    }
}