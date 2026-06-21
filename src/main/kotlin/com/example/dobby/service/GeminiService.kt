package com.example.dobby.service

import com.example.dobby.config.log
import com.example.dobby.dto.DiscordChatMessage
import com.example.dobby.exception.DobbyException
import com.example.dobby.llm.GeminiApiPort
import com.example.dobby.llm.GeminiModelManager
import org.springframework.stereotype.Service


@Service
class GeminiService(
    private val geminiApi: GeminiApiPort,
    private val geminiModelManager: GeminiModelManager,
    private val promptLoader: PromptLoaderService,
) {

    suspend fun generateRoast(messages: List<DiscordChatMessage>, persona: String?, memoryContext: String): String {
        val fullPrompt = buildFullPrompt(messages, persona, memoryContext)
        val aiModel = geminiModelManager.getBestModel()

        val response = try {
            log.info("Using Gemini model: $aiModel for roasting")
            geminiApi.generateContent(aiModel, fullPrompt)
        } catch (e: Exception) {
            geminiModelManager.reportModelFailure(aiModel)
            throw DobbyException.AiModelException("AI model $aiModel failed: ${e.message}", "Gemini Service", e)
        }

        return response.text() ?: throw DobbyException.DataMappingException("AI returned an empty response body.")
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