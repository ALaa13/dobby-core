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
        val promptFilePath = appProperties.llm.promptFilePath

        if (promptFilePath.isBlank()) return defaultPrompt

        return try {
            val path = Paths.get(promptFilePath).toAbsolutePath()
            if (!Files.exists(path)) {
                logger.error("AI prompt file not found at $path; using default prompt.")
                defaultPrompt
            } else {
                logger.info("Loading AI prompt from $path")
                Files.readString(path)
            }
        } catch (e: Exception) {
            logger.error("Failed to load AI prompt from $promptFilePath: ${e.message}; using default.")
            defaultPrompt
        }
    }
}
