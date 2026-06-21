package com.example.dobby.service

import com.example.dobby.config.log
import com.example.dobby.dto.DiscordDashboardResponse
import com.example.dobby.dto.DiscordGuildDto
import com.example.dobby.dto.DiscordTokenResponse
import com.example.dobby.dto.DiscordUserDto
import com.example.dobby.exception.DobbyException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.*
import org.springframework.stereotype.Service


@Service
class DiscordApiService(
    private val httpClient: HttpClient
) {
    companion object {
        private const val BASE_URL = "https://discord.com/api/v10"
        private const val DISCORD_TOKEN_URL = "$BASE_URL/oauth2/token"
    }

    suspend fun exchangeCodeForToken(
        code: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String
    ): DiscordTokenResponse {
        return try {
            log.info("Exchanging authorization code for access token with Discord API Service")
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
            throw e
        } catch (e: Exception) {
            log.error("Failed to exchange authorization code with Discord", e)
            throw DobbyException.AuthorizationException("Failed to exchange authorization code with Discord", e)
        }
    }


    suspend fun getUserProfile(accessToken: String): DiscordUserDto {
        return try {
            httpClient.get("$BASE_URL/users/@me") {
                header("Authorization", "Bearer $accessToken")
            }.body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error("Failed to fetch user profile data from Discord", e)
            throw DobbyException.AuthorizationException("Failed to fetch user profile data from Discord", e)
        }
    }


    suspend fun fetchCompleteUserProfile(discordUserToken: String): DiscordDashboardResponse {
        val userDto = getUserProfile(discordUserToken)

        val guildsList = try {
            httpClient.get("$BASE_URL/users/@me/guilds") {
                header("Authorization", "Bearer $discordUserToken")
            }.body<List<DiscordGuildDto>>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error("Failed to fetch managed guilds from Discord", e)
            throw DobbyException.AuthorizationException("Failed to fetch server listings from Discord", e)
        }

        // Bitwise permission filtering for structural management context (0x8 = Admin)
        val managedGuilds = guildsList.filter { guild ->
            guild.owner || (guild.permissions.toLongOrNull()?.let { (it and 0x8L) != 0L } ?: false)
        }

        val avatarUrl = if (userDto.avatar != null) {
            "https://cdn.discordapp.com/avatars/${userDto.id}/${userDto.avatar}.png"
        } else {
            "https://cdn.discordapp.com/embed/avatars/0.png"
        }

        return DiscordDashboardResponse(
            userDto.id,
            userDto.username,
            avatarUrl,
            managedGuilds
        )
    }
}