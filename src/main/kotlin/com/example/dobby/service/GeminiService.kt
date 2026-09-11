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
class GeminiService(
    private val appProperties: AppProperties,
    private val openApi: LlmApiPort,
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
                log.info("Using ChatGPT model: $model for roasting")
                openApi.generate(model, fullPrompt)
            } catch (e: Exception) {
                throw DobbyException.AiModelException("AI model $ failed: ${e.message}", "Gemini Service", e)
            }
        return parseAndMapResponse(response, persona)
    }

    private fun parseAndMapResponse(
        jsonText: String?,
        persona: String?,
    ): RoastResult {
        if (jsonText.isNullOrBlank()) {
            throw DobbyException.AiModelException(
                message = "Received an empty or null payload response from Gemini.",
                targetService = "GeminiRoastService",
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
            log.error("Failed to parse Gemini JSON output. Raw output was: $jsonText", e)
            throw DobbyException.AiModelException(
                message = "Gemini returned invalid or malformed JSON structure.",
                targetService = "GeminiRoastService",
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
