package com.example.dobby.service

import com.example.dobby.AppProperties
import com.example.dobby.dto.discord.DiscordChatMessage
import com.example.dobby.exception.DobbyException
import com.example.dobby.llm.LlmApiPort
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class AiRoastServiceTest {
    private val llmApi = mockk<LlmApiPort>()
    private val promptLoader = mockk<PromptLoaderService>()
    private val appProperties =
        AppProperties(
            llm =
                AppProperties.Llm(
                    apiKey = "test-key",
                    model = "test-model",
                    promptFilePath = "test-prompt",
                ),
        )

    private lateinit var aiRoastService: AiRoastService

    @BeforeEach
    fun setUp() {
        clearMocks(llmApi, promptLoader)
        aiRoastService =
            AiRoastService(
                appProperties = appProperties,
                llmApi = llmApi,
                promptLoader = promptLoader,
            )
    }

    @Test
    fun `generateRoast builds prompt, invokes configured model, and maps response`() =
        runTest {
            val messages =
                listOf(
                    DiscordChatMessage("User1", "1", "hash", "Hello", "2026-06-16T12:00:00Z"),
                    DiscordChatMessage("User2", "2", "hash", "World", "2026-06-16T12:01:00Z"),
                )
            val validJson =
                """
                ```json
                {
                  "roastText": "That post is older than COBOL.",
                  "primaryTargetId": "1",
                  "analytics": {
                    "clappedTheMostId": "2",
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

            every { promptLoader.loadPrompt() } returns "You are a roasting bot."
            coEvery { llmApi.generate("test-model", any()) } returns validJson

            val result = aiRoastService.generateRoast(messages, "Sarcastic", "User likes Fedora Linux")

            assertEquals("That post is older than COBOL.", result.text)
            assertEquals("Sarcastic", result.persona)
            assertEquals("1", result.primaryTargetId)
            assertEquals("2", result.clappedTheMostId)
            assertEquals(85, result.burnAccuracy)
            assertEquals(90, result.severityScore)
            assertEquals(1, result.targets.size)
            assertEquals("1", result.targets.single().userId)
            assertEquals("Using legacy structures", result.targets.single().reason)

            coVerify(exactly = 1) {
                llmApi.generate(
                    "test-model",
                    match { prompt ->
                        prompt.contains("You are a roasting bot.") &&
                            prompt.contains("Persona: Sarcastic") &&
                            prompt.contains("Memory Context: \nUser likes Fedora Linux") &&
                            prompt.contains("1 (2026-06-16T12:00:00Z): Hello") &&
                            prompt.contains("2 (2026-06-16T12:01:00Z): World")
                    },
                )
            }
        }

    @Test
    fun `generateRoast throws AiModelException when AI output is blank`() =
        runTest {
            every { promptLoader.loadPrompt() } returns "System Prompt"
            coEvery { llmApi.generate("test-model", any()) } returns "  "

            val exception =
                assertFailsWith<DobbyException.AiModelException> {
                    aiRoastService.generateRoast(emptyList(), null, "")
                }

            assertTrue(exception.message.orEmpty().contains("Received an empty or null payload from the AI model."))
            assertTrue(exception.message.orEmpty().contains("AiRoastService"))
        }

    @Test
    fun `generateRoast throws AiModelException when AI output is malformed JSON`() =
        runTest {
            every { promptLoader.loadPrompt() } returns "System Prompt"
            coEvery { llmApi.generate("test-model", any()) } returns "not-json"

            val exception =
                assertFailsWith<DobbyException.AiModelException> {
                    aiRoastService.generateRoast(emptyList(), "Gamer", "No Context")
                }

            assertTrue(exception.message.orEmpty().contains("AI model returned invalid or malformed JSON structure."))
            assertTrue(exception.message.orEmpty().contains("AiRoastService"))
        }

    @Test
    fun `generateRoast converts provider failure into AiModelException`() =
        runTest {
            every { promptLoader.loadPrompt() } returns "System Prompt"
            coEvery {
                llmApi.generate(any(), any())
            } throws RuntimeException("provider failure")

            val exception =
                assertFailsWith<DobbyException.AiModelException> {
                    aiRoastService.generateRoast(emptyList(), "Gamer", "No Context")
                }

            assertTrue(exception.message.orEmpty().contains("AI model test-model failed: provider failure"))
            assertTrue(exception.message.orEmpty().contains("AiRoastService"))
        }
}
