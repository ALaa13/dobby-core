package com.example.dobby.service

import com.example.dobby.config.RedisChannels
import com.example.dobby.dto.DiscordChatMessage
import com.example.dobby.dto.RoastRequest
import com.example.dobby.dto.toResult
import com.example.dobby.exception.DobbyException
import com.example.dobby.queue.RedisPublisher
import com.example.dobby.repository.UserProfileRepository
import com.example.dobby.util.logger
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import org.springframework.stereotype.Service

@Service
class RoastService(
    private val userRepository: UserProfileRepository,
    private val geminiService: GeminiService,
    private val redisPublisher: RedisPublisher
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
                redisPublisher.publishRoastDelivery(
                    RedisChannels.ROAST_DELIVERY,
                    request.toResult(roastResult, true)
                )
            } catch (e: DobbyException) {
                // Determine the message reply to be sent to the Client (Messages fit the UI theme)
                val friendlyBotErrorMessage = when (e) {
                    is DobbyException.DatabaseException -> "🤖 Memory vault locked out! I'm struggling to read the database right now."
                    is DobbyException.NetworkTimeoutException -> "⏳ Supabase was sleeping and didn't wake up in time. Try roaring at me again!"
                    is DobbyException.AiModelException -> "🤖 My brain got scrambled while talking to the AI. The roast got lost in translation!"
                    is DobbyException.DataMappingException -> "⚙️ System parsing error inside my memory core."
                    is DobbyException.GeneralException -> "System encountered an unexpected glitch."

                    else -> "⚠️ System encountered an unexpected glitch while processing your roast."
                }
                logger.error("Managed Dobby Exception caught: ${e.message}")
                redisPublisher.publishRoastDelivery(
                    RedisChannels.ROAST_DELIVERY,
                    request.toResult(friendlyBotErrorMessage, false)
                )
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