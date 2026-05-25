package com.example.dobby.service

import com.example.dobby.config.GeminiModelManager
import com.example.dobby.dto.DiscordChatMessage
import com.example.dobby.util.Logging
import com.google.genai.Client
import com.google.genai.types.GenerateContentResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths


@Service
class GeminiService(
    private val googleApiClient: Client,
    private val geminiModelManager: GeminiModelManager,
    @Value($$"${gemini.prompt.file:}") private val promptFilePath: String
) {
    private val promptText: String = loadPrompt()

    private fun loadPrompt(): String {
        val defaultPrompt = """You are a roast bot.""".trimIndent()
        if (promptFilePath.isBlank()) {
            return defaultPrompt
        }
        return try {
            val path = Paths.get(promptFilePath).toAbsolutePath()
            if (!Files.exists(path)) {
                Logging.logError("Gemini prompt file not found at $path; using default prompt.")
                defaultPrompt
            } else {
                Logging.logInfo("Loading Gemini prompt from $path")
                Files.readString(path)
            }
        } catch (ex: Exception) {
            Logging.logError("Failed to load Gemini prompt from $promptFilePath: ${ex.message}; using default.")
            defaultPrompt
        }
    }

    suspend fun generateRoast(messages: List<DiscordChatMessage>, persona: String?, memoryContext: String): String {
        val fullPrompt = buildFullPrompt(messages, persona, memoryContext)
        val aiModel = geminiModelManager.getBestModel()
        return try {
            Logging.logInfo("Using Gemini model: $aiModel for roasting")
            val response: GenerateContentResponse =
                googleApiClient.models.generateContent(
                    aiModel,
                    fullPrompt,
                    null
                )
            response.text() ?: "No text generated"
        } catch (e: Exception) {
            geminiModelManager.reportModelFailure(aiModel)
            throw Exception("AI failed: ${e.message}")
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
        return "$promptText\n\n" +
                "Persona: ${persona}\n\n" +
                "Memory Context: \n$memoryContext\n\n" +
                "Messages:\n$messagesText"
    }
}