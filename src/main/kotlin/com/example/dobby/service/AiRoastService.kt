package com.example.dobby.service

import com.example.dobby.AppProperties
import com.example.dobby.config.log
import com.example.dobby.dto.discord.DiscordChatMessage
import com.example.dobby.dto.roast.RoastGenerationResponse
import com.example.dobby.dto.roast.RoastResult
import com.example.dobby.dto.roast.TargetDamage
import com.example.dobby.exception.DobbyException
import com.example.dobby.llm.LlmApiPort
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Service

@Service
class AiRoastService(
    private val appProperties: AppProperties,
    private val llmApi: LlmApiPort,
    private val promptLoader: PromptLoaderService,
) {
    suspend fun generateRoast(
        messages: List<DiscordChatMessage>,
        persona: String?,
        memoryContext: String,
    ): RoastResult {
        val fullPrompt = buildFullPrompt(messages, persona, memoryContext)
        val model = appProperties.llm.model

        val response =
            try {
                // The configured model keeps deployments deterministic while LlmApiPort hides provider SDK details.
                log.info("Using AI model: $model for roasting")
                llmApi.generate(model, fullPrompt)
            } catch (e: Exception) {
                throw DobbyException.AiModelException(
                    message = "AI model $model failed: ${e.message}",
                    targetService = "AiRoastService",
                    cause = e,
                )
            }
        return parseAndMapResponse(response, persona)
    }

    private fun parseAndMapResponse(
        jsonText: String?,
        persona: String?,
    ): RoastResult {
        if (jsonText.isNullOrBlank()) {
            throw DobbyException.AiModelException(
                message = "Received an empty or null payload from the AI model.",
                targetService = "AiRoastService",
            )
        }

        val cleanJson =
            jsonText
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

        try {
            val parsedDto = Json.decodeFromString<RoastGenerationResponse>(cleanJson)

            return RoastResult(
                text = parsedDto.roastText,
                persona = persona,
                primaryTargetId = parsedDto.primaryTargetId,
                clappedTheMostId = parsedDto.analytics.clappedTheMostId,
                burnAccuracy = parsedDto.analytics.burnAccuracy,
                severityScore = parsedDto.analytics.severityScore,
                targets =
                    parsedDto.analytics.allTargets.map { target ->
                        TargetDamage(userId = target.discordUserId, reason = target.reason)
                    },
            )
        } catch (e: Exception) {
            log.error("Failed to parse AI roast JSON output. Raw output was: $jsonText", e)
            throw DobbyException.AiModelException(
                message = "AI model returned invalid or malformed JSON structure.",
                targetService = "AiRoastService",
                cause = e,
            )
        }
    }

    private fun buildFullPrompt(
        messages: List<DiscordChatMessage>,
        persona: String? = null,
        memoryContext: String,
    ): String {
        val messagesText =
            messages.joinToString("\n") {
                "${it.discordUserId} (${it.timestamp}): ${it.content}"
            }
        val promptText: String = promptLoader.loadPrompt()
        return "$promptText\n\n" +
            "Persona: ${persona}\n\n" +
            "Memory Context: \n$memoryContext\n\n" +
            "Messages:\n$messagesText"
    }
}
