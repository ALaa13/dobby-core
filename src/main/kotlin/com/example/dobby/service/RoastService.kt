package com.example.dobby.service

import com.example.dobby.dto.DiscordChatMessage
import com.example.dobby.dto.RoastRequest
import com.example.dobby.dto.toResult
import com.example.dobby.repository.UserProfileRepository
import com.example.dobby.util.Logging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import org.springframework.stereotype.Service

@Service
class RoastService(
    private val userRepository: UserProfileRepository,
    private val geminiService: GeminiService,
    private val discordCallbackClient: DiscordCallbackClient
) {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @PreDestroy
    private fun cleanup() {
        serviceScope.cancel()
    }

    fun processRoastAsync(request: RoastRequest) {
        serviceScope.launch {
            try {
                val memoryContext = buildFactsMemoryContext(request.messages, request.guildId)
                val roastResult = geminiService.generateRoast(request.messages, request.persona, memoryContext)
                discordCallbackClient.deliverRoast(request.toResult(roastResult, true))
            } catch (e: Exception) {
                Logging.logError("Background roast processing failed: ${e.message}")
                runCatching {
                    discordCallbackClient.deliverRoast(request.toResult(e.message ?: "Unknown error", false))
                }
            }
        }
    }

    private suspend fun buildFactsMemoryContext(
        messages: List<DiscordChatMessage>,
        guildId: String
    ): String {
        val factsMap = getFactsForUsers(messages, guildId)
        val builder = StringBuilder()
        for ((userId, facts) in factsMap) {
            if (facts.isEmpty()) continue
            builder.append("Facts about <@$userId>:\n")
            facts.forEach { fact ->
                builder.append("- $fact\n")
            }
            builder.append("\n")
        }
        return builder.toString()
    }

    private suspend fun getFactsForUsers(
        messages: List<DiscordChatMessage>,
        guildId: String
    ): Map<String, List<String>> {
        val userIds = extractUniqueUserIds(messages)
        val facts = mutableMapOf<String, List<String>>()
        for (userId in userIds) {
            val userFacts = userRepository.findProfile(userId, guildId)
            facts[userFacts?.discordUserId ?: userId] = userFacts?.facts?.map { it.factText } ?: emptyList()
        }
        return facts

    }

    private fun extractUniqueUserIds(messages: List<DiscordChatMessage>): Set<String> {
        return messages.map { it.author }.toSet()
    }
}