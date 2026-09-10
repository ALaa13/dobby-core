package com.example.dobby.repository

import com.example.dobby.dto.fact.FactSource
import com.example.dobby.dto.fact.UserFactCreateRequest
import com.example.dobby.entity.UserFactEntity
import com.example.dobby.repository.r2dbc.UserFactPostgresStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals

class UserFactRepositoryTest {
    private val userFactStore = mockk<UserFactPostgresStore>()
    private val repository = UserFactRepository(userFactStore)

    @Test
    fun `saveFact delegates to PostgreSQL and maps the returned entity`() =
        runTest {
            val profileId = UUID.fromString("10000000-0000-0000-0000-000000000001")
            val factId = UUID.fromString("20000000-0000-0000-0000-000000000001")
            val createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z")
            val request =
                UserFactCreateRequest(
                    profileId = profileId.toString(),
                    factText = "Likes socks",
                    source = FactSource.USER_SUBMISSION,
                )
            coEvery {
                userFactStore.insert(
                    profileId = profileId.toString(),
                    factText = "Likes socks",
                    source = "USER_SUBMISSION",
                )
            } returns
                UserFactEntity(
                    id = factId,
                    profileId = profileId,
                    factText = "Likes socks",
                    source = "USER_SUBMISSION",
                    createdAt = createdAt,
                    updatedAt = null,
                )

            val result = repository.saveFact(request)

            assertEquals(factId.toString(), result.id)
            assertEquals(profileId.toString(), result.profileId)
            assertEquals("Likes socks", result.factText)
            assertEquals(createdAt.toString(), result.createdAt)
        }

    @Test
    fun `delete methods delegate their string identifiers unchanged`() =
        runTest {
            coEvery { userFactStore.deleteById("fact-id") } returns 1
            coEvery { userFactStore.deleteAllByProfileId("profile-id") } returns 2
            coEvery { userFactStore.deleteAllByGuildId("guild-id") } returns 3

            repository.deleteFactById("fact-id")
            repository.deleteFactsByProfileId("profile-id")
            repository.deleteFactsByGuildId("guild-id")

            coVerify(exactly = 1) { userFactStore.deleteById("fact-id") }
            coVerify(exactly = 1) { userFactStore.deleteAllByProfileId("profile-id") }
            coVerify(exactly = 1) { userFactStore.deleteAllByGuildId("guild-id") }
        }
}
