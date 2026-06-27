package com.example.dobby.service

import com.example.dobby.config.log
import com.example.dobby.dto.discord.DiscordChatMessage
import com.example.dobby.dto.roast.DiscordRoastRequest
import com.example.dobby.dto.roast.RoastLogDbResponse
import com.example.dobby.dto.roast.toResult
import com.example.dobby.dto.user.UserProfileCreateRequest
import com.example.dobby.exception.DobbyException
import com.example.dobby.queue.RedisChannels
import com.example.dobby.queue.RedisPublisher
import com.example.dobby.repository.RoastRepository
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
    private val roastRepository: RoastRepository,
    private val geminiService: GeminiService,
    private val redisPublisher: RedisPublisher,
    @Qualifier("ioScope") private val serviceScope: CoroutineScope,
) {
    fun processRoastAsync(request: DiscordRoastRequest) {
        serviceScope.launch {
            processRoast(request)
        }
    }

    suspend fun getGuildRoasts(guildId: String): List<RoastLogDbResponse> {
        log.info("Getting roasts for guild $guildId")
        return roastRepository.getGuildRoasts(guildId)
    }

    @PreDestroy
    private fun cleanup() {
        serviceScope.cancel()
    }

    private suspend fun processRoast(request: DiscordRoastRequest) {
        try {
            // Sync user profiles
            syncUserProfiles(request)

            val memoryContext = buildFactsMemoryContext(request.messages, request.guildId)
            val roastResult =
                geminiService.generateRoast(
                    request.messages,
                    request.persona,
                    memoryContext,
                )
            log.info("Roast generation completed successfully")

            // Save to the database
            roastRepository.saveRoastResult(request.guildId, request.channelId, roastResult)
            log.info("Roast result saved to database")

            val result = request.toResult(roastResult.text, true)
            redisPublisher.publishRoastDelivery(
                RedisChannels.ROAST_DELIVERY,
                result,
            )
        } catch (e: DobbyException) {
            val friendlyBotErrorMessage =
                when (e) {
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

            log.error("Managed Dobby Exception caught: ${e.message}")
            val result = request.toResult(friendlyBotErrorMessage, false)
            redisPublisher.publishRoastDelivery(
                RedisChannels.ROAST_DELIVERY,
                result,
            )
        }
    }

    suspend fun syncUserProfiles(request: DiscordRoastRequest) {
        val profilesToSync =
            request.messages
                .distinctBy { it.discordUserId }
                .map { msg ->
                    UserProfileCreateRequest(
                        discordUserId = msg.discordUserId,
                        guildId = request.guildId,
                        displayName = msg.displayName,
                        avatarHash = msg.avatarHash,
                    )
                }

        // Fire the batch upsert to lock down their identities
        userRepository.upsertProfiles(profilesToSync)
        log.info("Successfully synced ${profilesToSync.size} user profiles from chat history")
    }

    private suspend fun buildFactsMemoryContext(
        messages: List<DiscordChatMessage>,
        guildId: String,
    ): String {
        log.info("Building facts memory context for guild $guildId with ${messages.size} messages")
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
        guildId: String,
    ): Map<String, List<String>> {
        val userIds = extractUniqueUserIds(messages)
        val facts = mutableMapOf<String, List<String>>()
        for (userId in userIds) {
            val userFacts = userRepository.findProfile(userId, guildId)
            facts[userFacts?.discordUserId ?: userId] = userFacts?.facts?.map { it.factText } ?: emptyList()
        }
        return facts
    }

    private fun extractUniqueUserIds(messages: List<DiscordChatMessage>): Set<String> =
        messages
            .map {
                it.discordUserId
            }.toSet()
}
