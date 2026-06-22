package com.example.dobby.service

import com.example.dobby.dto.DiscordFactRequest
import com.example.dobby.dto.FactSource
import com.example.dobby.dto.UserFactResponse
import com.example.dobby.dto.UserProfileResponse
import com.example.dobby.exception.DobbyException
import com.example.dobby.repository.UserFactRepository
import com.example.dobby.repository.UserProfileRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertFailsWith

class FactServiceTest {

    private val userProfileRepository = mockk<UserProfileRepository>()
    private val userFactRepository = mockk<UserFactRepository>()
    private lateinit var factService: FactService

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        factService = FactService(userProfileRepository, userFactRepository)
    }

    // saveFact() Tests

    @Test
    fun `saveFact should create a new profile if one does not exist and then save the fact`() = runTest {
        val request = DiscordFactRequest(
            "He'll fix a kernel panic before fixing his posture.",
            "user-123",
            "guild-456",
            "SomeUser"
        )
        val createdProfile = UserProfileResponse(
            id = "1",
            discordUserId = "user-123",
            guildId = "guild-456",
            displayName = "SomeUser",
            facts = emptyList(),
            createdAt = "2023-01-01T00:00:00Z",
            updatedAt = "2023-01-01T00:00:00Z"
        )
        val mockFactResponse = UserFactResponse(
            id = "1",
            profileId = "1",
            factText = "Enjoys Kotlin development",
            source = "USER_SUBMISSION",
            confidenceScore = 80,
            roastabilityScore = 20,
            createdAt = "2026-06-16T12:00:00Z",
            updatedAt = "2026-06-16T12:00:00Z"
        )

        coEvery { userProfileRepository.findProfile("user-123", "guild-456") } returns null
        coEvery { userProfileRepository.saveProfile(any()) } returns createdProfile
        coEvery { userFactRepository.saveFact(any()) } returns mockFactResponse


        factService.saveUserFact(request)


        coVerify(exactly = 1) { userProfileRepository.findProfile("user-123", "guild-456") }
        coVerify(exactly = 1) {
            userProfileRepository.saveProfile(match {
                it.discordUserId == "user-123" && it.guildId == "guild-456" && it.displayName == "SomeUser"
            })
        }
        coVerify(exactly = 1) {
            userFactRepository.saveFact(match {
                it.profileId == "1" && it.factText == "He'll fix a kernel panic before fixing his posture." && it.source == FactSource.USER_SUBMISSION
            })
        }
    }

    @Test
    fun `saveFact should reuse existing profile if it exists and then save the fact`() = runTest {

        val request = DiscordFactRequest(
            "He'll fix a kernel panic before fixing his posture.",
            "user-123",
            "guild-456",
            "SomeUser"
        )
        val existingProfile = UserProfileResponse(
            id = "1",
            discordUserId = "user-123",
            guildId = "guild-456",
            displayName = "SomeUser",
            facts = emptyList(),
            createdAt = "2023-01-01T00:00:00Z",
            updatedAt = "2023-01-01T00:00:00Z"
        )

        val mockFactResponse = UserFactResponse(
            id = "1",
            profileId = "1",
            factText = "He'll fix a kernel panic before fixing his posture.",
            source = "USER_SUBMISSION",
            confidenceScore = 80,
            roastabilityScore = 20,
            createdAt = "2026-06-16T12:00:00Z",
            updatedAt = "2026-06-16T12:00:00Z"
        )

        coEvery { userProfileRepository.findProfile("user-123", "guild-456") } returns existingProfile
        coEvery { userFactRepository.saveFact(any()) } returns mockFactResponse


        factService.saveUserFact(request)


        coVerify(exactly = 1) { userProfileRepository.findProfile("user-123", "guild-456") }
        coVerify(exactly = 0) { userProfileRepository.saveProfile(any()) }
        coVerify(exactly = 1) { userFactRepository.saveFact(any()) }
    }


    // getFacts() Tests


    @Test
    fun `getFacts should return list of facts when profile exists`() = runTest {
        val mockFacts = listOf(
            UserFactResponse(
                id = "1",
                profileId = "1",
                factText = "Fact 1",
                source = "USER_SUBMISSION",
                confidenceScore = 80,
                roastabilityScore = 20,
                createdAt = "2023-01-01T00:00:00Z",
                updatedAt = "2023-01-01T00:00:00Z"
            ),
            UserFactResponse(
                id = "2",
                profileId = "1",
                factText = "Fact 2",
                source = "USER_SUBMISSION",
                confidenceScore = 80,
                roastabilityScore = 20,
                createdAt = "2023-01-01T00:00:00Z",
                updatedAt = "2023-01-01T00:00:00Z"
            )
        )
        val profile = UserProfileResponse(
            id = "1",
            discordUserId = "user-123",
            guildId = "guild-456",
            displayName = "SomeUser",
            facts = mockFacts,
            createdAt = "2023-01-01T00:00:00Z",
            updatedAt = "2023-01-01T00:00:00Z"
        )

        coEvery { userProfileRepository.findProfile("user-123", "guild-456") } returns profile


        val result = factService.getUserFacts("user-123", "guild-456")


        assertEquals(2, result.size)
        assertEquals("Fact 1", result[0].factText)
    }

    @Test
    fun `getFacts should return empty list when profile does not exist`() = runTest {
        coEvery { userProfileRepository.findProfile("any-user", "any-guild") } returns null

        val result = factService.getUserFacts("any-user", "any-guild")

        assertTrue(result.isEmpty())
    }


    // resetFacts() Tests

    @Test
    fun `resetFacts should delete facts when profile exists`() = runTest {
        val profile = UserProfileResponse(
            id = "1",
            discordUserId = "user-123",
            guildId = "guild-456",
            displayName = "Alaa",
            facts = emptyList(),
            createdAt = "2023-01-01T00:00:00Z",
            updatedAt = "2023-01-01T00:00:00Z"
        )
        coEvery { userProfileRepository.findProfile("user-123", "guild-456") } returns profile
        coEvery { userFactRepository.deleteFactsByProfileId("1") } returns Unit


        factService.resetUserFacts("user-123", "guild-456")


        coVerify(exactly = 1) { userFactRepository.deleteFactsByProfileId("1") }
    }

    @Test
    fun `resetFacts should throw ProfileNotFoundException when profile does not exist`() = runTest {
        coEvery { userProfileRepository.findProfile("fake-user", "fake-guild") } returns null

        assertFailsWith<DobbyException.ProfileNotFoundException> {
            factService.resetUserFacts("fake-user", "fake-guild")
        }

        coVerify(exactly = 0) { userFactRepository.deleteFactsByProfileId(any()) }
    }
}