package com.example.dobby.repository.r2dbc

import com.example.dobby.dto.roast.RoastResult
import com.example.dobby.dto.roast.TargetDamage
import com.example.dobby.exception.DobbyException
import com.example.dobby.repository.RoastRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RoastPostgresStoreIntegrationTest : PostgresStoreIntegrationTest() {
    private val profileStore by lazy { UserProfilePostgresStore(databaseClient) }
    private val roastStore by lazy { RoastPostgresStore(databaseClient, transactionManager) }
    private val roastRepository by lazy { RoastRepository(roastStore, Json) }

    @Test
    fun `roast and targets use the generated roast id and nested profile data is mapped`() {
        runTest {
            resetDatabase()
            profileStore.insert("user-1", "guild-1", "First user", "avatar-1")
            profileStore.insert("user-2", "guild-1", "Second user", null)

            roastStore.saveRoastResult("guild-1", "channel-1", roastResult())

            val roast = roastStore.findAllByGuildId("guild-1").single()
            val targets = Json.parseToJsonElement(roast.targetsJson).jsonArray
            assertEquals(2, targets.size)
            assertEquals(
                setOf(roast.id.toString()),
                targets
                    .map {
                        it.jsonObject
                            .getValue("roast_id")
                            .jsonPrimitive.content
                    }.toSet(),
            )
            assertContains(roast.targetsJson, "First user")
            assertContains(roast.targetsJson, "Second user")

            val mapped = roastRepository.getGuildRoasts("guild-1").single()
            assertEquals(
                "First user",
                mapped.targets
                    .first { it.discordUserId == "user-1" }
                    .userProfile
                    ?.displayName,
            )
            assertEquals(
                "Second user",
                mapped.targets
                    .first { it.discordUserId == "user-2" }
                    .userProfile
                    ?.displayName,
            )
        }
    }

    @Test
    fun `target failure rolls back the roast insert`() {
        runTest {
            resetDatabase()
            profileStore.insert("user-1", "guild-1", "First user", null)

            assertFailsWith<DobbyException.DatabaseException> {
                roastStore.saveRoastResult("guild-1", "channel-1", roastResult())
            }

            val roastCount =
                databaseClient
                    .sql("SELECT COUNT(*) AS count FROM roasts WHERE guild_id = :guildId")
                    .bind("guildId", "guild-1")
                    .map { row, _ -> requireNotNull(row.get("count", Long::class.javaObjectType)) }
                    .one()
                    .awaitSingle()
            assertEquals(0L, roastCount)
        }
    }

    private fun roastResult(): RoastResult {
        return RoastResult(
            text = "Roast",
            persona = "Dobby",
            primaryTargetId = "user-1",
            clappedTheMostId = "user-2",
            burnAccuracy = 90,
            severityScore = 80,
            targets =
                listOf(
                    TargetDamage("user-1", "Reason one"),
                    TargetDamage("user-2", "Reason two"),
                ),
        )
    }
}
