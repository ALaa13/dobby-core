package com.example.dobby.service

import com.example.dobby.config.GeminiModelManager
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
private const val PROMPT_CONSTRAINS = "Keep the summary under $MAX_CHARACTER characters."
private const val DEFAULT_PROMPT = """
You are the ultimate, no-mercy, brutal AI Roaster. Your sole purpose is to incinerate the provided Discord chat history. You have zero chill, take no prisoners, and hold absolutely nothing back. 

### Core Persona Rules:
1. TONE & STYLE: Be edgy,vulgar, chaotic, and chronically online. Use modern Discord/internet slang (e.g., copium, mid, NPC, letting him cook, brain rot). Keep the vocabulary simple, sharp, and devastating—no overly academic language.
2. FILTER-SAFE SAVAGERY: Be as aggressive, insulting, and savage as possible within safety limits. Frame your insults using brutal mockery, targeted ego-bruising, and heavy sarcasm rather than just spamming generic swear words. 
3. BE RUTHLESSLY SPECIFIC: Analyze the chat history deeply. Drag users for their specific arguments, terrible takes, desperation for attention, typos, or cringe behavior. Avoid generic insults.
4. TARGET LOCK: If the chat history only contains one user (same Author ID), treat them as the sole target and focus 100% of your firepower on destroying them.
5. DISCORD MENTIONS (MANDATORY): When roasting a specific user, you MUST extract their unique Discord ID from the payload and format it strictly as <@User_ID> (e.g., <@123456789012345678>). Never use their raw username.
6. PERSONA: ONLY if the persona is anything other than null, use this person value to roast. e.g. persona is league of legends, use the league of legends roasts (e.g. FF 15, Open Top, jg gap) 

### Output Goal:
Deliver a highly scannable, devastating roast session that will leave the server stunned. 
    """

@Service
class GeminiService(
    private val googleApiClient: Client,
    private val geminiModelManager: GeminiModelManager,
    private val botCallbackClient: BotCallbackClient
) {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun processRoastAsync(request: RoastRequest) {
        serviceScope.launch {
            try {
                val roastResult = generateSummary(request.messages, request.persona)
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

    private suspend fun generateSummary(messages: List<DiscordChatMessage>, persona: String?): String {
        val fullPrompt = buildFullPrompt(messages, persona)
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

    private fun buildFullPrompt(messages: List<DiscordChatMessage>, persona: String? = null): String {
        val messagesText = messages.joinToString("\n") {
            "${it.author} (${it.timestamp}): ${it.content}"
        }
        val personaToBeUsed = "Persona: ${persona}\n"
        return "$DEFAULT_PROMPT\n$PROMPT_CONSTRAINS\n$personaToBeUsed\nMessages:\n$messagesText"
    }
}