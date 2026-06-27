package com.example.dobby.service

import com.example.dobby.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths

@Component
class PromptLoaderService(
    private val appProperties: AppProperties,
) {
    private val logger = LoggerFactory.getLogger(PromptLoaderService::class.java)
    private val defaultPrompt = "You are a roast bot."

    fun loadPrompt(): String {
        val promptFilePath = appProperties.gemini.promptFilePath

        if (promptFilePath.isBlank()) return defaultPrompt

        return try {
            val path = Paths.get(promptFilePath).toAbsolutePath()
            if (!Files.exists(path)) {
                logger.error("Gemini prompt file not found at $path; using default prompt.")
                defaultPrompt
            } else {
                logger.info("Loading Gemini prompt from $path")
                Files.readString(path)
            }
        } catch (e: Exception) {
            logger.error("Failed to load Gemini prompt from $promptFilePath: ${e.message}; using default.")
            defaultPrompt
        }
    }
}
