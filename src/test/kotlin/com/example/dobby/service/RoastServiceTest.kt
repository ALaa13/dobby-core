package com.example.dobby.service

import com.example.dobby.dto.*
import com.example.dobby.exception.DobbyException
import com.example.dobby.queue.RedisChannels
import com.example.dobby.queue.RedisPublisher
import com.example.dobby.repository.UserProfileRepository
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoastServiceTest {

    private val userRepository = mockk<UserProfileRepository>()
    private val geminiService = mockk<GeminiService>()
    private val redisPublisher = mockk<RedisPublisher>(relaxed = true)

    private lateinit var roastService: RoastService

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { redisPublisher.publishRoastDelivery(any(), any()) } returns Unit
    }

    @Test
    fun `processRoastAsync should gather facts, call Gemini, and publish success to Redis`() = runTest {
        roastService = RoastService(
            userRepository,
            geminiService,
            redisPublisher,
            CoroutineScope(StandardTestDispatcher(testScheduler))
        )

        val messages = listOf(DiscordChatMessage("user1", "Hello", "2026-06-16T15:00:00Z"))
        val request = RoastRequest(
            messages = messages,
            guildId = "guild-777",
            persona = "Sarcastic",
            channelId = "channel-123",
        )

        // Mocking database entities based on your logic's mapping properties
        val mockProfile = mockk<UserProfileResponse>()
        val mockFact = mockk<UserFactResponse>()

        every { mockProfile.discordUserId } returns "user1"
        every { mockFact.factText } returns "Likes Fedora Linux"
        every { mockProfile.facts } returns listOf(mockFact)

        coEvery { userRepository.findProfile("user1", "guild-777") } returns mockProfile

        val expectedMemoryContext = "Facts about <@user1>:\n- Likes Fedora Linux\n\n"
        coEvery {
            geminiService.generateRoast(
                messages,
                "Sarcastic",
                expectedMemoryContext
            )
        } returns "Nice OS choice, grandpas use it too."


        roastService.processRoastAsync(request)
        println("after processRoastAsync")

        // CRITICAL: Let background coroutines execution catch up completely
        advanceUntilIdle()


        val expectedSuccessResult = request.toResult("Nice OS choice, grandpas use it too.", true)
        coVerify(exactly = 1) {
            userRepository.findProfile("user1", "guild-777")
        }
        coVerify(exactly = 1) {
            geminiService.generateRoast(any(), any(), any())
        }
        verify(exactly = 1) {
            redisPublisher.publishRoastDelivery(any(), any())
        }

    }

    @Test
    fun `processRoastAsync should catch DatabaseException and publish friendly bot error message`() = runTest {
        roastService = RoastService(
            userRepository,
            geminiService,
            redisPublisher,
            CoroutineScope(StandardTestDispatcher(testScheduler))
        )

        val messages = listOf(DiscordChatMessage("user1", "Hello", "2026-06-16T15:00:00Z"))
        val request = RoastRequest(
            messages = messages,
            guildId = "guild-777",
            persona = "Sarcastic",
            channelId = "channel-123",
        )

        coEvery {
            userRepository.findProfile("user1", "guild-777")
        } throws DobbyException.DatabaseException("DB Connection Pool Exhausted", null)


        roastService.processRoastAsync(request)
        advanceUntilIdle()

        // 3. Assert (Verify what happened AFTER the code ran)
        val expectedFriendlyMessage = "🤖 Memory vault locked out! I'm struggling to read the database right now."
        val expectedErrorResult = request.toResult(expectedFriendlyMessage, false)

        // Check with loose parameters first to see if it works!
        verify { redisPublisher.publishRoastDelivery(RedisChannels.ROAST_DELIVERY, expectedErrorResult) }


        // Verify Gemini was bypassed entirely since database failed early
        coVerify(exactly = 0) { geminiService.generateRoast(any(), any(), any()) }
    }

    @Test
    fun `processRoastAsync should catch AiModelException and publish friendly AI error message`() = runTest {
        roastService = RoastService(
            userRepository,
            geminiService,
            redisPublisher,
            CoroutineScope(StandardTestDispatcher(testScheduler))
        )

        val messages = listOf(DiscordChatMessage("user2", "World", "2026-06-16T15:00:00Z"))
        val request = RoastRequest(
            messages = messages, guildId = "guild-777", persona = "Mean",
            channelId = "channel-123",
        )

        // Database works fine but returns no facts this time
        coEvery { userRepository.findProfile("user2", "guild-777") } returns null

        // AI call breaks down
        coEvery {
            geminiService.generateRoast(
                messages,
                "Mean",
                ""
            )
        } throws DobbyException.AiModelException("Model Timeout Error", "Gemini Core Engine", null)


        roastService.processRoastAsync(request)
        advanceUntilIdle()


        val expectedFriendlyMessage =
            "🤖 My brain got scrambled while talking to the AI. The roast got lost in translation!"
        val expectedErrorResult = request.toResult(expectedFriendlyMessage, false)

        verify(exactly = 1) {
            redisPublisher.publishRoastDelivery(RedisChannels.ROAST_DELIVERY, expectedErrorResult)
        }
    }
}