package com.example.dobby.service

import com.example.dobby.dto.roast.RoastLogDbResponse
import com.example.dobby.dto.user.UserProfileResponse
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DashboardServiceTest {
    // 1. Mock the actual dependencies required by DashboardService
    private val factService = mockk<FactService>()
    private val roastService = mockk<RoastService>()

    private lateinit var dashboardService: DashboardService

    @BeforeEach
    fun setUp() {
        clearAllMocks()

        // 2. Initialize the correct service instance
        dashboardService = DashboardService(factService, roastService)
    }

    @Test
    fun `getAllFactsByGuildId should fetch and return profiles from factService`() =
        runTest {
            val guildId = "guild-123"
            val mockProfiles = listOf(mockk<UserProfileResponse>())

            coEvery { factService.getAllFactsByGuild(guildId) } returns mockProfiles

            val result = dashboardService.getAllFactsByGuildId(guildId)

            assertEquals(mockProfiles, result)
            coVerify(exactly = 1) { factService.getAllFactsByGuild(guildId) }
        }

    @Test
    fun `getAllRoastByGuildId should fetch and return roast logs from roastService`() =
        runTest {
            val guildId = "guild-123"
            val mockRoasts = listOf(mockk<RoastLogDbResponse>())

            coEvery { roastService.getGuildRoasts(guildId) } returns mockRoasts

            val result = dashboardService.getAllRoastByGuildId(guildId)

            assertEquals(mockRoasts, result)
            coVerify(exactly = 1) { roastService.getGuildRoasts(guildId) }
        }

    @Test
    fun `deleteFact should invoke factService removal exactly once`() =
        runTest {
            val factId = "fact-999"

            coEvery { factService.deleteUserFact(factId) } just Runs

            dashboardService.deleteFact(factId)

            coVerify(exactly = 1) { factService.deleteUserFact(factId) }
        }

    @Test
    fun `purgeGuildFacts should invoke factService reset exactly once`() =
        runTest {
            val guildId = "guild-777"

            coEvery { factService.resetGuildFacts(guildId) } just Runs

            dashboardService.purgeGuildFacts(guildId)

            coVerify(exactly = 1) { factService.resetGuildFacts(guildId) }
        }
}
