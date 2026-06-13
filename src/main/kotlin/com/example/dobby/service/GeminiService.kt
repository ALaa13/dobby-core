package com.example.dobby.service

import com.example.dobby.dto.DiscordChatMessage
import com.example.dobby.exception.DobbyException
import com.example.dobby.llm.GeminiModelManager
import com.example.dobby.util.logger
import com.google.genai.Client
import com.google.genai.types.GenerateContentResponse
import org.springframework.stereotype.Service


@Service
class GeminiService(
    private val googleApiClient: Client,
    private val geminiModelManager: GeminiModelManager,
    private val promptLoader: PromptLoader,
) {

    suspend fun generateRoast(messages: List<DiscordChatMessage>, persona: String?, memoryContext: String): String {
        val fullPrompt = buildFullPrompt(messages, persona, memoryContext)
        val aiModel = geminiModelManager.getBestModel()
        return try {
            logger.info("Using Gemini model: $aiModel for roasting")
            val response: GenerateContentResponse =
                googleApiClient.models.generateContent(
                    aiModel,
                    fullPrompt,
                    null
                )
            response.text() ?: throw DobbyException.DataMappingException("AI returned an empty response body.")
        } catch (e: Exception) {
            geminiModelManager.reportModelFailure(aiModel)
            throw DobbyException.AiModelException("AI model $aiModel failed: ${e.message}", "Gemini Service", e)
        }
    }

    private fun buildFullPrompt(
        messages: List<DiscordChatMessage>,
        persona: String? = null,
        memoryContext: String
    ): String {
        val messagesText = messages.joinToString("\n") {
            "${it.author} (${it.timestamp}): ${it.content}"
        }
        val promptText: String = promptLoader.loadPrompt()
        return "$promptText\n\n" +
                "Persona: ${persona}\n\n" +
                "Memory Context: \n$memoryContext\n\n" +
                "Messages:\n$messagesText"
    }
}