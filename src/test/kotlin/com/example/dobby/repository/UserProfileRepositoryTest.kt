package com.example.dobby.repository

import com.example.dobby.dto.user.UserProfileCreateRequest
import com.example.dobby.repository.r2dbc.UserProfilePostgresStore
import com.example.dobby.repository.r2dbc.projection.UserProfileWithFactsRow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals

class UserProfileRepositoryTest {
    private val userProfileStore = mockk<UserProfilePostgresStore>()
    private val repository = UserProfileRepository(userProfileStore, Json)

    @Test
    fun `findProfile maps the profile and aggregated facts`() {
        runTest {
            val profileId = UUID.fromString("10000000-0000-0000-0000-000000000001")
            val factId = UUID.fromString("20000000-0000-0000-0000-000000000001")
            coEvery {
                userProfileStore.findByDiscordUserIdAndGuildId("user-1", "guild-1")
            } returns
                UserProfileWithFactsRow(
                    id = profileId,
                    discordUserId = "user-1",
                    guildId = "guild-1",
                    displayName = "Dobby",
                    avatarHash = "avatar",
                    createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z"),
                    updatedAt = null,
                    factsJson =
                        """
                        [{
                          "id": "$factId",
                          "profile_id": "$profileId",
                          "fact_text": "Likes socks",
                          "source": "USER_SUBMISSION",
                          "created_at": "2026-01-01T10:01:00Z",
                          "updated_at": null
                        }]
                        """.trimIndent(),
                )

            val result = repository.findProfile("user-1", "guild-1")

            assertEquals(profileId.toString(), result?.id)
            assertEquals("Dobby", result?.displayName)
            assertEquals(1, result?.facts?.size)
            assertEquals("Likes socks", result?.facts?.single()?.factText)
        }
    }

    @Test
    fun `upsertProfiles returns without calling persistence for an empty list`() {
        runTest {
            repository.upsertProfiles(emptyList())

            coVerify(exactly = 0) {
                userProfileStore.upsertProfiles(any<List<UserProfileCreateRequest>>())
            }
        }
    }

    @Test
    fun `findProfilesWithFacts maps all rows from the bulk store query`() {
        runTest {
            val profileId = UUID.fromString("10000000-0000-0000-0000-000000000002")
            coEvery {
                userProfileStore.findAllByDiscordUserIdsAndGuildId(listOf("user-2"), "guild-1")
            } returns
                listOf(
                    UserProfileWithFactsRow(
                        id = profileId,
                        discordUserId = "user-2",
                        guildId = "guild-1",
                        displayName = "Winky",
                        avatarHash = null,
                        createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z"),
                        updatedAt = null,
                        factsJson = "[]",
                    ),
                )

            val result = repository.findProfilesWithFacts(listOf("user-2"), "guild-1")

            assertEquals(listOf("user-2"), result.map { it.discordUserId })
            assertEquals(emptyList(), result.single().facts)
        }
    }
}
