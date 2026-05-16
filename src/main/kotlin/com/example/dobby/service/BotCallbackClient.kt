package com.example.dobby.service

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class BotCallbackClient(
    private val httpClient: HttpClient,
    @Value($$"${dobby.bot.url}") private val botUrl: String,
    @Value($$"${dobby.security.token}") private val internalToken: String
) {
    suspend fun deliverRoast(channelId: String, messageId: String, content: String) {
        try {
            val response = httpClient.post("$botUrl/api/internal/deliver") {
                header("X-Internal-Token", internalToken)
                contentType(ContentType.Application.Json)
                setBody(
                    mapOf(
                        "channelId" to channelId,
                        "messageId" to messageId,
                        "content" to content
                    )
                )
            }
            println("Bot callback response status: ${response.status}")
        } catch (e: Exception) {
            println("Failed to send HTTP request to Bot: ${e.message}")
            e.printStackTrace()
        }
    }
}