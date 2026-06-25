package com.example.dobby.service

import com.example.dobby.repository.UserProfileRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class GuildManagementServiceTest {
    private val userProfileRepository = mockk<UserProfileRepository>()

    private lateinit var guildManagementService: GuildManagementService

    @BeforeEach
    fun setUp() {
        clearAllMocks()

        guildManagementService = GuildManagementService(
            userProfileRepository
        )
    }

    @Test
    fun `resetGuildFacts should invoke repository delete exactly once`() = runTest {
        val targetGuildId = "987654321012345678"

        coEvery { userProfileRepository.deleteAllByGuildId(targetGuildId) } coAnswers { }

        guildManagementService.resetGuildFacts(targetGuildId)

        // Verify the repository function was called with the correct parameter
        coVerify(exactly = 1) {
            userProfileRepository.deleteAllByGuildId(targetGuildId)
        }
    }
}