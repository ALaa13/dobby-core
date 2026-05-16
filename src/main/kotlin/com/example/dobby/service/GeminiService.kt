package com.example.dobby.service

import com.example.dobby.model.ChatDiscordMessage
import com.example.dobby.model.RoastRequest
import com.google.genai.Client
import com.google.genai.types.GenerateContentResponse
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service


private const val MAX_CHARACTER = 4096
private const val GEMINI_AI_MODEL = "gemini-2.5-flash"
private const val PROMPT_CONSTRAINS = "Keep the summary under $MAX_CHARACTER characters."
private const val DEFAULT_PROMPT = """
        You are the ultimate, no-mercy, brutal AI Roaster. Your sole purpose is to incinerate this Discord chat history. You have zero chill, you take no prisoners, and you hold absolutely nothing back. 

### Core Persona Rules:
1. INFUSE GEN-Z SLANG & MEME CULTURE" 
2. Use vulgar language. use meme and Reddit troll. reference memes that fit the context.
3. BE SPECIFIC: Don't just give generic insults. Look closely at what users are actually saying, what they are arguing about, or how they are acting, and drag them for it.
4. DISCORD USER MENTIONS (CRITICAL): When roasting a specific user, you MUST look at their Discord ID or unique author string from the data payload and mention them strictly using the `<@User_ID>` format (e.g., <@123456789012345678>). This ensures they get pinged in the final server embed. Never just type their raw text username.

### Output Structure:
- Start with a devastating group summary title (e.g., "The Absolute State of This Server").
- Break down individual roasts for the top 3-4 most chaotic users in the chat history using clear bullet points.
- End with a final, savage closing thought targeting the entire channel.
    """

@Service
class GeminiService(
    private val geminiClient: Client,
    private val botCallbackClient: BotCallbackClient
) {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun processRoastAsync(request: RoastRequest) {
        serviceScope.launch {
            try {
                val roastResult = generateSummary(request.messages)
                botCallbackClient.deliverRoast(
                    channelId = request.channelId,
                    messageId = request.messageId,
                    content = roastResult
                )
            } catch (e: Exception) {
                println("Background roast processing failed: ${e.message}")
            }
        }
    }

    @PreDestroy
    fun cleanup() {
        serviceScope.cancel()
    }

    private suspend fun generateSummary(messages: List<ChatDiscordMessage>): String {
        val fullPrompt = buildFullPrompt(messages)
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

    private fun buildFullPrompt(messages: List<ChatDiscordMessage>): String {
        val messagesText = messages.joinToString("\n") {
            "${it.author} (${it.timestamp}): ${it.content}"
        }
        return "$DEFAULT_PROMPT\n$PROMPT_CONSTRAINS\n\nMessages:\n$messagesText"
    }
}