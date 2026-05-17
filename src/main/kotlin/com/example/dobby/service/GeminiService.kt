package com.example.dobby.service

import com.example.dobby.dto.DiscordChatMessage
import com.example.dobby.dto.RoastRequest
import com.example.dobby.dto.RoastResultRequest
import com.example.dobby.util.Logging
import com.google.genai.Client
import com.google.genai.types.GenerateContentResponse
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import org.springframework.stereotype.Service


private const val MAX_CHARACTER = 4096
private const val GEMINI_AI_MODEL = "gemini-2.5-flash"
private const val PROMPT_CONSTRAINS = "Keep the summary under $MAX_CHARACTER characters."
private const val DEFAULT_PROMPT = """
        You are the ultimate, no-mercy, brutal AI Roaster. Your sole purpose is to incinerate this Discord chat history. You have zero chill, you take no prisoners, and you hold absolutely nothing back. 

### Core Persona Rules:
1. INFUSE SLANG & MEME CULTURE AND BE EDGY AND TROLL" 
2. Use vulgar language (Go wild. no restrictions) and do not use too much big English words. reference memes that fit the context.
3. BE SPECIFIC: Don't just give generic insults. Look closely at what users are actually saying, what they are arguing about, or how they are acting, and drag them for it.
4. If you only receive one user (Same Author ID in all the messages), that means its a target user to be roasted and not the whole channel.
5. DISCORD USER MENTIONS (CRITICAL): When roasting a specific user, you MUST look at their Discord ID or unique author string from the data payload and mention them strictly using the `<@User_ID>` format (e.g., <@123456789012345678>). This ensures they get pinged in the final server embed. Never just type their raw text username.
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
                    RoastResultRequest(
                        channelId = request.channelId,
                        content = roastResult,
                        success = true,
                    )
                )
            } catch (e: Exception) {
                Logging.logError("Background roast processing failed: ${e.message}")
                runCatching {
                    botCallbackClient.deliverRoast(
                        RoastResultRequest(
                            channelId = request.channelId,
                            content = "",
                            success = false
                        )
                    )
                }
            }
        }
    }

    @PreDestroy
    fun cleanup() {
        serviceScope.cancel()
    }

    private suspend fun generateSummary(messages: List<DiscordChatMessage>): String {
        val fullPrompt = buildFullPrompt(messages)
        return try {
            val response: GenerateContentResponse =
                geminiClient.models.generateContent(
                    GEMINI_AI_MODEL,
                    fullPrompt,
                    null
                )
            response.text() ?: "No text generated"
        } catch (e: Exception) {
            throw Exception("AI failed: ${e.message}")
        }
    }

    private fun buildFullPrompt(messages: List<DiscordChatMessage>): String {
        val messagesText = messages.joinToString("\n") {
            "${it.author} (${it.timestamp}): ${it.content}"
        }
        return "$DEFAULT_PROMPT\n$PROMPT_CONSTRAINS\n\nMessages:\n$messagesText"
    }
}