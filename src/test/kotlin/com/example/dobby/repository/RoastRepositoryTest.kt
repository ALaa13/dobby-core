package com.example.dobby.repository

import com.example.dobby.dto.roast.RoastResult
import com.example.dobby.dto.roast.TargetDamage
import com.example.dobby.repository.r2dbc.RoastPostgresStore
import com.example.dobby.repository.r2dbc.projection.RoastWithTargetsRow
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals

class RoastRepositoryTest {
    private val roastStore = mockk<RoastPostgresStore>()
    private val repository = RoastRepository(roastStore, Json)

    @Test
    fun `getGuildRoasts maps aggregated targets and nested profiles`() =
        runTest {
            val roastId = UUID.fromString("30000000-0000-0000-0000-000000000001")
            coEvery { roastStore.findAllByGuildId("guild-1") } returns
                listOf(
                    RoastWithTargetsRow(
                        id = roastId,
                        guildId = "guild-1",
                        channelId = "channel-1",
                        roastText = "Roast",
                        personaUsed = "Dobby",
                        primaryTargetId = "user-1",
                        clappedTheMostId = "user-1",
                        burnAccuracy = 90,
                        severityScore = 80,
                        createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z"),
                        targetsJson =
                            """
                            [{
                              "roast_id": "$roastId",
                              "discord_user_id": "user-1",
                              "guild_id": "guild-1",
                              "damage_reason": "The reason",
                              "user_profiles": {
                                "display_name": "Dobby",
                                "avatar_hash": "avatar"
                              }
                            }]
                            """.trimIndent(),
                    ),
                )

            val result = repository.getGuildRoasts("guild-1").single()

            assertEquals(roastId.toString(), result.id)
            assertEquals("The reason", result.targets.single().reason)
            assertEquals("Dobby", result.targets.single().userProfile?.displayName)
        }

    @Test
    fun `saveRoastResult delegates the unchanged service model`() =
        runTest {
            val result =
                RoastResult(
                    text = "Roast",
                    persona = "Dobby",
                    primaryTargetId = "user-1",
                    clappedTheMostId = "user-1",
                    burnAccuracy = 90,
                    severityScore = 80,
                    targets = listOf(TargetDamage("user-1", "The reason")),
                )
            coEvery { roastStore.saveRoastResult("guild-1", "channel-1", result) } just Runs

            repository.saveRoastResult("guild-1", "channel-1", result)

            coVerify(exactly = 1) {
                roastStore.saveRoastResult("guild-1", "channel-1", result)
            }
        }
}
