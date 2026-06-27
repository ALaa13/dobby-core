package com.example.dobby.service

import com.example.dobby.dto.discord.DiscordChatMessage
import com.example.dobby.exception.DobbyException
import com.example.dobby.llm.GeminiApiPort
import com.example.dobby.llm.GeminiModelManager
import com.google.genai.types.GenerateContentResponse
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    fun `generateRoast should successfully build prompt, invoke model, and return response text`() =
        runTest {
            val messages =
                listOf(
                    DiscordChatMessage("User1", "1", "hash", "Hello", "2026-06-16T12:00:00Z"),
                    DiscordChatMessage("User2", "2", "hash", "World", "2026-06-16T12:01:00Z"),
                )
            val mockResponse = mockk<GenerateContentResponse>()

            every { promptLoader.loadPrompt() } returns "You are a roasting bot."
            every { geminiModelManager.getBestModel() } returns "gemini-2.5-pro"
            val validJsonOutput =
                """
                ```json
                {
                  "roastText": "That post is older than COBOL.",
                  "primaryTargetId": "1",
                  "analytics": {
                    "clappedTheMostId": "1",
                    "burnAccuracy": 85,
                    "severityScore": 90,
                    "allTargets": [
                      {
                        "discordUserId": "1",
                        "reason": "Using legacy structures"
                      }
                    ]
                  }
                }
                ```
                """.trimIndent()

            every { mockResponse.text() } returns validJsonOutput
            coEvery { geminiApi.generateContent("gemini-2.5-pro", any()) } returns mockResponse

            val result = geminiService.generateRoast(messages, "Sarcastic", "User likes Fedora Linux")

            // Asserting properties parsed from the valid mocked JSON object structure
            assertEquals("That post is older than COBOL.", result.text)
            assertEquals(85, result.burnAccuracy)
            assertEquals(90, result.severityScore)

            coVerify(exactly = 1) { geminiApi.generateContent("gemini-2.5-pro", any()) }
            verify(exactly = 0) { geminiModelManager.reportModelFailure(any()) }
        }

    @Test
    fun `generateRoast should throw AiModelException when AI text response is empty`() =
        runTest {
            val mockResponse = mockk<GenerateContentResponse>()
            every { promptLoader.loadPrompt() } returns "System Prompt"
            every { geminiModelManager.getBestModel() } returns "gemini-2.5-pro"
            every { geminiModelManager.reportModelFailure("gemini-2.5-pro") } returns Unit
            every { mockResponse.text() } returns null

            coEvery {
                geminiApi.generateContent("gemini-2.5-pro", any<String>())
            } returns mockResponse

            // 🚀 FIXED: Expected exception updated to AiModelException to match the Service's guard clause line 39
            assertFailsWith<DobbyException.AiModelException> {
                geminiService.generateRoast(emptyList(), null, "")
            }

            verify(exactly = 0) { geminiModelManager.reportModelFailure(any()) }
        }

    @Test
    fun `generateRoast should report model failure and throw AiModelException when SDK client crashes`() =
        runTest {
            every { promptLoader.loadPrompt() } returns "System Prompt"
            every { geminiModelManager.getBestModel() } returns "gemini-2.5-pro"
            every { geminiModelManager.reportModelFailure("gemini-2.5-pro") } returns Unit

            coEvery {
                geminiApi.generateContent("gemini-2.5-pro", any<String>())
            } throws RuntimeException("API quota exceeded or network dropout")

            assertFailsWith<DobbyException.AiModelException> {
                geminiService.generateRoast(emptyList(), "Gamer", "No Context")
            }

            verify(exactly = 1) { geminiModelManager.reportModelFailure("gemini-2.5-pro") }
        }
}
