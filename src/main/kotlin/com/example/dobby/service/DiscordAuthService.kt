package com.example.dobby.service

import com.example.dobby.AppProperties
import com.example.dobby.config.log
import com.example.dobby.exception.DobbyException
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class DiscordAuthService(
    private val appProperties: AppProperties,
    private val discordApiService: DiscordApiService,
    private val jwtService: JWTService,
    private val discordAccountService: DiscordAccountService,
) {
    companion object {
        private const val DISCORD_AUTH_URL = "https://discord.com/api/oauth2/authorize"
        private const val FRONT_END_REDIRECT_DESTINATION = "/dashboard"
        private const val FRONT_END_LOGIN_PAGE = "/login"
    }

    // Generates the clean URI for the controller to redirect to
    fun getDiscordLoginUri(): URI {
        val uri =
            UriComponentsBuilder
                .fromUriString(DISCORD_AUTH_URL)
                .queryParam("client_id", appProperties.discord.clientId)
                .queryParam("redirect_uri", appProperties.discord.redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "identify guilds")
                .build()
                .toUri()

        log.info("Generated Discord login URI: $uri")
        return uri
    }

    // Handles the heavy exchange logic and returns the final destination URI
    suspend fun handleCallbackAndGenerateRedirect(code: String?): URI {
        val frontendUrl = appProperties.frontend.url
        return try {
            if (code == null) {
                throw DobbyException.InvalidAuthenticationRequestException(
                    "Missing authorization code in callback request",
                )
            }

            // 1. Delegate Token Exchange to the API Service
            val tokenResponse =
                discordApiService.exchangeCodeForToken(
                    code,
                    appProperties.discord.clientId,
                    appProperties.discord.clientSecret,
                    appProperties.discord.redirectUri,
                )

            val accessToken =
                tokenResponse.accessToken
                    ?: throw DobbyException.AuthorizationException("Failed to retrieve access token from Discord")

            // 2. Delegate Identity Discovery to the API Service
            val userResponse = discordApiService.getUserProfile(accessToken)
            val discordUserId = userResponse.id
            val username = userResponse.username

            // Save the user credentials securely to your DB layer
            discordAccountService.saveDiscordAccount(discordUserId, accessToken)

            val jwtToken = jwtService.generateJWTToken(discordUserId, username)
            buildRedirectUri(frontendUrl + FRONT_END_REDIRECT_DESTINATION, jwtToken)
        } catch (e: DobbyException.InvalidAuthenticationRequestException) {
            log.warn("Authentication request rejected: ${e.message}")
            buildRedirectUri(frontendUrl + FRONT_END_LOGIN_PAGE, "error=invalid_request")
        } catch (e: Exception) {
            log.error("OAuth handshake failed due to a severe system error", e)
            buildRedirectUri(frontendUrl + FRONT_END_LOGIN_PAGE, "error=discord_auth_failed")
        }
    }

    private fun buildRedirectUri(
        baseUrl: String,
        queryValue: String,
    ): URI {
        val querySegment = if (queryValue.contains("=")) queryValue else "token=$queryValue"
        val delimiter = if (baseUrl.contains("?")) "&" else "?"
        val uri = URI.create("$baseUrl$delimiter$querySegment")
        log.info("Generated redirect: $uri")
        return uri
    }
}
