package com.example.dobby.service

import com.example.dobby.AppProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class PromptLoaderTest {

    private lateinit var testProperties: AppProperties
    private lateinit var promptLoaderService: PromptLoaderService

    @BeforeEach
    fun setUp() {
        testProperties = AppProperties()
        promptLoaderService = PromptLoaderService(appProperties = testProperties)
    }


    @Test
    fun `should return default prompt when path is blank`() {
        testProperties.gemini.promptFilePath = ""

        val result = promptLoaderService.loadPrompt()
        assertEquals("You are a roast bot.", result)
    }

    @Test
    fun `should return default prompt when file does not exist`() {
        testProperties.gemini.promptFilePath = "this_file_definitely_does_not_exist.txt"

        val result = promptLoaderService.loadPrompt()
        assertEquals("You are a roast bot.", result)
    }

    @Test
    fun `should successfully read and return content of valid prompt file`() {
        val testFile = File.createTempFile("test_prompt", ".txt")
        testFile.writeText("You are an elite Arch Linux hacker.")

        testProperties.gemini.promptFilePath = testFile.absolutePath

        try {
            val result = promptLoaderService.loadPrompt()
            assertEquals("You are an elite Arch Linux hacker.", result)
        } finally {
            testFile.delete()
        }
    }
}