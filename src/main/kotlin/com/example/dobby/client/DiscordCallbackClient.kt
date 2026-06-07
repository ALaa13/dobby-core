package com.example.dobby.client

import com.example.dobby.dto.RoastResultRequest
import com.example.dobby.util.Logging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class DiscordCallbackClient(
    private val httpClient: HttpClient,
    @Value($$"${dobby.bot.url}") private val botUrl: String,
    @Value($$"${dobby.security.token}") private val internalToken: String
) {
    suspend fun deliverRoast(roastResponse: RoastResultRequest) {
        try {
            val response = httpClient.post("$botUrl/api/internal/deliver") {
                header("X-Internal-Token", internalToken)
                contentType(ContentType.Application.Json)
                setBody(roastResponse)
            }
            Logging.logInfo("Bot callback response status: ${response.status}")
        } catch (e: Exception) {
            Logging.logError("Failed to send HTTP request to Bot: ${e.message}")
        }
    }
}