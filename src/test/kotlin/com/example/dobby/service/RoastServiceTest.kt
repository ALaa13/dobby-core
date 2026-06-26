package com.example.dobby.service

import com.example.dobby.dto.discord.DiscordChatMessage
import com.example.dobby.dto.fact.UserFactResponse
import com.example.dobby.dto.roast.RoastRequest
import com.example.dobby.dto.roast.RoastResult
import com.example.dobby.dto.roast.toResult
import com.example.dobby.dto.user.UserProfileResponse
import com.example.dobby.exception.DobbyException
import com.example.dobby.queue.RedisChannels
import com.example.dobby.queue.RedisPublisher
import com.example.dobby.repository.RoastRepository
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
    private val roastRepository = mockk<RoastRepository>(relaxed = true)
    private val geminiService = mockk<GeminiService>()
    private val redisPublisher = mockk<RedisPublisher>(relaxed = true)

    private lateinit var roastService: RoastService

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { redisPublisher.publishRoastDelivery(any(), any()) } returns Unit
        coEvery { userRepository.upsertProfiles(any()) } returns Unit
    }

    @Test
    fun `processRoastAsync should gather facts, call Gemini, and publish success to Redis`() = runTest {
        roastService = RoastService(
            userRepository,
            roastRepository,
            geminiService,
            redisPublisher,
            CoroutineScope(StandardTestDispatcher(testScheduler))
        )

        val messages = listOf(
            DiscordChatMessage(
                displayName = "user1",
                discordUserId = "user1",
                avatarHash = "a_hash_123",
                content = "Hello",
                timestamp = "2026-06-16T15:00:00Z"
            )
        )
        val request = RoastRequest(
            messages = messages,
            guildId = "guild-777",
            persona = "Sarcastic",
            channelId = "channel-123",
        )

        val mockProfile = mockk<UserProfileResponse>()
        val mockFact = mockk<UserFactResponse>()

        every { mockProfile.discordUserId } returns "user1"
        every { mockFact.factText } returns "Likes Fedora Linux"
        every { mockProfile.facts } returns listOf(mockFact)

        coEvery { userRepository.findProfile("user1", "guild-777") } returns mockProfile

        val expectedMemoryContext = "Facts about <@user1>:\n- Likes Fedora Linux\n\n"

        // Fix: Mock standard RoastResult instead of returning raw String
        val mockRoastResult = mockk<RoastResult>()
        every { mockRoastResult.text } returns "Nice OS choice, grandpas use it too."

        coEvery {
            geminiService.generateRoast(
                messages,
                "Sarcastic",
                expectedMemoryContext
            )
        } returns mockRoastResult

        roastService.processRoastAsync(request)
        advanceUntilIdle()

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
            roastRepository,
            geminiService,
            redisPublisher,
            CoroutineScope(StandardTestDispatcher(testScheduler))
        )

        val messages = listOf(
            DiscordChatMessage(
                displayName = "user1",
                discordUserId = "user1",
                avatarHash = "a_hash_123",
                content = "Hello",
                timestamp = "2026-06-16T15:00:00Z"
            )
        )
        val request = RoastRequest(
            messages = messages,
            guildId = "guild-777",
            persona = "Sarcastic",
            channelId = "channel-123",
        )

        coEvery {
            userRepository.upsertProfiles(any())
        } throws DobbyException.DatabaseException("DB Connection Pool Exhausted", null)

        roastService.processRoastAsync(request)
        advanceUntilIdle()

        val expectedFriendlyMessage = "🤖 Memory vault locked out! I'm struggling to read the database right now."
        val expectedErrorResult = request.toResult(expectedFriendlyMessage, false)

        verify { redisPublisher.publishRoastDelivery(RedisChannels.ROAST_DELIVERY, expectedErrorResult) }
        coVerify(exactly = 0) { geminiService.generateRoast(any(), any(), any()) }
    }

    @Test
    fun `processRoastAsync should catch AiModelException and publish friendly AI error message`() = runTest {
        roastService = RoastService(
            userRepository,
            roastRepository,
            geminiService,
            redisPublisher,
            CoroutineScope(StandardTestDispatcher(testScheduler))
        )

        val messages = listOf(
            DiscordChatMessage(
                displayName = "user2",
                discordUserId = "user2",
                avatarHash = "a_hash_456",
                content = "World",
                timestamp = "2026-06-16T15:00:00Z"
            )
        )
        val request = RoastRequest(
            messages = messages,
            guildId = "guild-777",
            persona = "Mean",
            channelId = "channel-123",
        )

        coEvery { userRepository.findProfile("user2", "guild-777") } returns null

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