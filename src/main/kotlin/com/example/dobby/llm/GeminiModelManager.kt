package com.example.dobby.llm

import com.example.dobby.util.logger
import com.google.genai.Client
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Configuration
class GeminiModelManager(private val aiClient: Client) {

    private var availableModels = listOf<String>()
    private val modelCooldowns = ConcurrentHashMap<String, Instant>()

    /**
     * Call this during your bot's startup sequence.
     * Compatible with com.google.genai:google-genai:1.51.0
     */
    @EventListener(ApplicationReadyEvent::class)
    fun initialize() {
        runCatching {
            val responseList = aiClient.models.list(null)

            // Safely unpack the Optional<String> and filter
            availableModels = responseList
                .mapNotNull { model ->
                    // Get the list of supported actions for this model safely
                    val actions: List<String> = model.supportedActions().orElse(null) ?: emptyList()
                    if (actions.contains("generateContent")) {
                        model.name().orElse(null)?.removePrefix("models/")
                    } else {
                        null
                    }
                }
                .filter { name ->
                    name.isNotBlank() &&
                            name.contains("gemini", ignoreCase = true) &&
                            !name.contains("vision", ignoreCase = true)
                }
                // Sort so "flash" variants appear at index 0 (fast/cheap default options)
                .sortedByDescending { it.contains("flash", ignoreCase = true) }
            logger.info("Dynamically discovered Gemini models count: ${availableModels.size}")
        }.onFailure { error ->
            logger.info("Failed to dynamically discover Gemini models: ${error.message}")
            availableModels = listOf("gemini-2.5-flash", "gemini-2.5-pro")
        }
    }

    /**
     * Pulls the best available model from the cache that isn't currently undergoing a timeout.
     */
    fun getBestModel(): String {
        val now = Instant.now()

        return availableModels.firstOrNull { model ->
            val cooldownEnd = modelCooldowns[model]
            cooldownEnd == null || now.isAfter(cooldownEnd)
        } ?: "gemini-2.5-flash"
    }

    /**
     * Registers a model failure to temporarily isolate it from selection.
     */
    fun reportModelFailure(modelName: String) {
        // Enforce a 15-minute cooldown for the broken/rate-limited model
        modelCooldowns[modelName] = Instant.now().plusSeconds(900)
    }
}