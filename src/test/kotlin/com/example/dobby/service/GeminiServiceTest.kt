package com.example.dobby.service

import com.example.dobby.dto.DiscordChatMessage
import com.example.dobby.exception.DobbyException
import com.example.dobby.llm.GeminiApiPort
import com.example.dobby.llm.GeminiModelManager
import com.google.genai.types.GenerateContentResponse
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith


class GeminiServiceTest {

    private val geminiApi = mockk<GeminiApiPort>()
    private val geminiModelManager = mockk<GeminiModelManager>()
    private val promptLoader = mockk<PromptLoaderService>()

    private lateinit var geminiService: GeminiService

    @BeforeEach
    fun setUp() {
        clearMocks(geminiModelManager, promptLoader)
        geminiService = GeminiService(geminiApi, geminiModelManager, promptLoader)
    }

    @Test
    fun `generateRoast should successfully build prompt, invoke model, and return response text`() = runTest {
        val messages = listOf(
            DiscordChatMessage("User1", "Hello", "2026-06-16T12:00:00Z"),
            DiscordChatMessage("User2", "World", "2026-06-16T12:01:00Z")
        )
        val mockResponse = mockk<GenerateContentResponse>()

        every { promptLoader.loadPrompt() } returns "You are a roasting bot."
        every { geminiModelManager.getBestModel() } returns "gemini-2.5-pro"
        every { mockResponse.text() } returns "That post is older than COBOL."
        coEvery { geminiApi.generateContent("gemini-2.5-pro", any()) } returns mockResponse

        val result = geminiService.generateRoast(messages, "Sarcastic", "User likes Fedora Linux")

        assertEquals("That post is older than COBOL.", result)
        coVerify(exactly = 1) { geminiApi.generateContent("gemini-2.5-pro", any()) }
        verify(exactly = 0) { geminiModelManager.reportModelFailure(any()) }
    }

    @Test
    fun `generateRoast should throw DataMappingException when AI text response is empty`() = runTest {
        val mockResponse = mockk<GenerateContentResponse>()
        every { promptLoader.loadPrompt() } returns "System Prompt"
        every { geminiModelManager.getBestModel() } returns "gemini-2.5-pro"
        every { geminiModelManager.reportModelFailure("gemini-2.5-pro") } returns Unit
        every { mockResponse.text() } returns null

        coEvery {
            geminiApi.generateContent("gemini-2.5-pro", any<String>())
        } returns mockResponse


        assertFailsWith<DobbyException.DataMappingException> {
            geminiService.generateRoast(emptyList(), null, "")
        }

        // Note: Even though it fails due to an empty response body, the service doesn't consider
        // this an SDK/Network connection crash, so it shouldn't report a model failure.
        verify(exactly = 0) { geminiModelManager.reportModelFailure(any()) }
    }

    @Test
    fun `generateRoast should report model failure and throw AiModelException when SDK client crashes`() = runTest {
        every { promptLoader.loadPrompt() } returns "System Prompt"
        every { geminiModelManager.getBestModel() } returns "gemini-2.5-pro"
        every { geminiModelManager.reportModelFailure("gemini-2.5-pro") } returns Unit

        //  Simulate a network error or API timeout from the Google SDK client
        coEvery {
            geminiApi.generateContent("gemini-2.5-pro", any<String>())
        } throws RuntimeException("API quota exceeded or network dropout")


        assertFailsWith<DobbyException.AiModelException> {
            geminiService.generateRoast(emptyList(), "Gamer", "No Context")
        }

        // Make sure the service accurately notified the manager that this model is acting up!
        verify(exactly = 1) { geminiModelManager.reportModelFailure("gemini-2.5-pro") }
    }
}