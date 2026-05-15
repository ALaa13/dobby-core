package com.example.dobby.service

import com.example.dobby.model.ChatDiscordMessage
import com.google.genai.Client
import com.google.genai.types.GenerateContentResponse
import org.springframework.stereotype.Service


private const val MAX_CHARACTER = 4096
private const val GEMINI_AI_MODEL = "gemini-2.5-flash"
private const val PROMPT_CONSTRAINS = "Keep the summary under $MAX_CHARACTER characters."
private const val DEFAULT_PROMPT = """
        Summarize the following Discord chat messages in a concise and informative way.
        Focus on the main topics discussed and key points mentioned.
    """

@Service
class GeminiService(
    private val geminiClient: Client
) {

    suspend fun generateSummary(messages: List<ChatDiscordMessage>, customPrompt: String?): String {
        val fullPrompt = buildFullPrompt(customPrompt, messages)
        return try {
            val response: GenerateContentResponse =
                geminiClient.models.generateContent(
                    GEMINI_AI_MODEL,
                    fullPrompt,
                    null
                )
            response.text() ?: "No summary generated"
        } catch (e: Exception) {
            throw Exception("AI Summary failed: ${e.message}")
        }
    }

    private fun buildFullPrompt(customPrompt: String?, messages: List<ChatDiscordMessage>): String {
        val prompt = customPrompt ?: DEFAULT_PROMPT
        val messagesText = messages.joinToString("\n") {
            "${it.author} (${it.timestamp}): ${it.content}"
        }
        return "$prompt\n$PROMPT_CONSTRAINS\n\nMessages:\n$messagesText"
    }
}