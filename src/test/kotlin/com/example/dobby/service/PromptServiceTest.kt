package com.example.dobby.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class PromptLoaderTest {

    @Test
    fun `should return default prompt when path is blank`() {
        val loader = PromptLoader(promptFilePath = "")
        val result = loader.loadPrompt()
        assertEquals("You are a roast bot.", result)
    }

    @Test
    fun `should return default prompt when file does not exist`() {
        val loader = PromptLoader(promptFilePath = "this_file_definitely_does_not_exist.txt")
        val result = loader.loadPrompt()
        assertEquals("You are a roast bot.", result)
    }

    @Test
    fun `should successfully read and return content of valid prompt file`() {
        val testFile = File.createTempFile("test_prompt", ".txt")
        testFile.writeText("You are an elite Arch Linux hacker.")
        val loader = PromptLoader(promptFilePath = testFile.absolutePath)
        try {
            val result = loader.loadPrompt()
            assertEquals("You are an elite Arch Linux hacker.", result)
        } finally {
            testFile.delete()
        }
    }
}