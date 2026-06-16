package com.example.dobby.service

import com.example.dobby.config.logger
import com.example.dobby.dto.DiscordChatMessage
import com.example.dobby.dto.RoastRequest
import com.example.dobby.dto.toResult
import com.example.dobby.exception.DobbyException
import com.example.dobby.queue.RedisChannels
import com.example.dobby.queue.RedisPublisher
import com.example.dobby.repository.UserProfileRepository
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class RoastService(
    private val userRepository: UserProfileRepository,
    private val geminiService: GeminiService,
    private val redisPublisher: RedisPublisher,
    @Qualifier("ioScope") private val serviceScope: CoroutineScope
) {

    @PreDestroy
    private fun cleanup() {
        serviceScope.cancel()
    }

    fun processRoastAsync(request: RoastRequest) {
        serviceScope.launch {
            processRoast(request)
        }
    }

    private suspend fun processRoast(request: RoastRequest) {
        try {
            val memoryContext = buildFactsMemoryContext(request.messages, request.guildId)
            val roastText = geminiService.generateRoast(
                request.messages,
                request.persona,
                memoryContext
            )
            val result = request.toResult(roastText, true)
            redisPublisher.publishRoastDelivery(
                RedisChannels.ROAST_DELIVERY,
                result
            )
        } catch (e: DobbyException) {
            val friendlyBotErrorMessage = when (e) {
                is DobbyException.DatabaseException ->
                    "🤖 Memory vault locked out! I'm struggling to read the database right now."

                is DobbyException.NetworkTimeoutException ->
                    "⏳ Supabase was sleeping and didn't wake up in time. Try roaring at me again!"

                is DobbyException.AiModelException ->
                    "🤖 My brain got scrambled while talking to the AI. The roast got lost in translation!"

                is DobbyException.DataMappingException ->
                    "⚙️ System parsing error inside my memory core."

                is DobbyException.GeneralException ->
                    "System encountered an unexpected glitch."

                else ->
                    "⚠️ System encountered an unexpected glitch while processing your roast."
            }

            logger.error("Managed Dobby Exception caught: ${e.message}")
            val result = request.toResult(friendlyBotErrorMessage, false)
            redisPublisher.publishRoastDelivery(
                RedisChannels.ROAST_DELIVERY,
                result
            )
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