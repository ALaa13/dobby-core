package com.example.dobby.repository.r2dbc

import com.example.dobby.dto.roast.RoastResult
import com.example.dobby.dto.roast.TargetDamage
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserFactPostgresStoreIntegrationTest : PostgresStoreIntegrationTest() {
    private val profileStore by lazy { UserProfilePostgresStore(databaseClient) }
    private val factStore by lazy { UserFactPostgresStore(databaseClient) }
    private val roastStore by lazy { RoastPostgresStore(databaseClient, transactionManager) }

    @Test
    fun `insert and delete operations use PostgreSQL identifiers`() =
        runTest {
            resetDatabase()
            val profile = profileStore.insert("user-1", "guild-1", null, null)
            val profileId = assertNotNull(profile.id)
            val first = factStore.insert(profileId.toString(), "First", "USER_SUBMISSION")
            factStore.insert(profileId.toString(), "Second", null)

            assertNotNull(first.id)
            assertNotNull(first.createdAt)
            assertEquals(1L, factStore.deleteById(assertNotNull(first.id).toString()))
            assertEquals(listOf("Second"), factStore.findAllByProfileId(profileId).map { it.factText })
            assertEquals(1L, factStore.deleteAllByProfileId(profileId.toString()))
            assertEquals(emptyList(), factStore.findAllByProfileId(profileId))
        }

    @Test
    fun `deleting guild facts preserves profiles and facts from other guilds`() =
        runTest {
            resetDatabase()
            val first = profileStore.insert("user-1", "guild-1", null, null)
            val second = profileStore.insert("user-2", "guild-1", null, null)
            val otherGuild = profileStore.insert("user-1", "guild-2", null, null)
            factStore.insert(assertNotNull(first.id).toString(), "First guild fact", null)
            factStore.insert(assertNotNull(second.id).toString(), "Second guild fact", null)
            factStore.insert(assertNotNull(otherGuild.id).toString(), "Other guild fact", null)
            roastStore.saveRoastResult(
                "guild-1",
                "channel-1",
                RoastResult(
                    text = "Historical roast",
                    persona = null,
                    primaryTargetId = "user-1",
                    clappedTheMostId = "user-1",
                    burnAccuracy = 80,
                    severityScore = 70,
                    targets = listOf(TargetDamage("user-1", "Historical target")),
                ),
            )

            assertEquals(2L, factStore.deleteAllByGuildId("guild-1"))

            assertEquals(2, profileStore.findAllByGuildId("guild-1").size)
            assertEquals(
                1,
                Json.parseToJsonElement(roastStore.findAllByGuildId("guild-1").single().targetsJson).jsonArray.size,
            )
            assertEquals(0, profileStore.findAllByGuildId("guild-1").sumOf { factsCount(it.factsJson) })
            assertEquals(
                1,
                factsCount(assertNotNull(profileStore.findByDiscordUserIdAndGuildId("user-1", "guild-2")).factsJson),
            )
        }

    private fun factsCount(factsJson: String): Int = "\"fact_text\"".toRegex().findAll(factsJson).count()
}
