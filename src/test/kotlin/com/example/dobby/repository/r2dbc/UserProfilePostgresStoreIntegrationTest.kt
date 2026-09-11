package com.example.dobby.repository.r2dbc

import com.example.dobby.dto.user.UserProfileCreateRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class UserProfilePostgresStoreIntegrationTest : PostgresStoreIntegrationTest() {
    private val profileStore by lazy { UserProfilePostgresStore(databaseClient) }
    private val factStore by lazy { UserFactPostgresStore(databaseClient) }

    @Test
    fun `insert lets PostgreSQL generate identity and aggregates facts in profile reads`() {
        runTest {
            resetDatabase()
            val profile = profileStore.insert("user-1", "guild-1", "Dobby", "avatar-1")
            val profileId = profile.id
            assertNull(profile.updatedAt)

            factStore.insert(profileId.toString(), "First fact", "USER_SUBMISSION")
            factStore.insert(profileId.toString(), "Second fact", null)

            val found = assertNotNull(profileStore.findByDiscordUserIdAndGuildId("user-1", "guild-1"))
            assertContains(found.factsJson, "First fact")
            assertContains(found.factsJson, "Second fact")
            assertEquals(listOf(profileId), profileStore.findAllByGuildId("guild-1").map { it.id })
        }
    }

    @Test
    fun `batch upsert updates mutable fields and timestamp while preserving identity and creation time`() {
        runTest {
            resetDatabase()
            profileStore.upsertProfiles(
                listOf(
                    profile("user-1", "guild-1", "First", "avatar-1"),
                    profile("user-2", "guild-1", "Second", "avatar-2"),
                    profile("user-1", "guild-2", "Other guild", null),
                ),
            )
            val original = assertNotNull(profileStore.findByDiscordUserIdAndGuildId("user-1", "guild-1"))

            delay(10.milliseconds)
            profileStore.upsertProfiles(listOf(profile("user-1", "guild-1", "Updated", "avatar-new")))

            val updated = assertNotNull(profileStore.findByDiscordUserIdAndGuildId("user-1", "guild-1"))
            assertEquals(original.id, updated.id)
            assertEquals(original.createdAt, updated.createdAt)
            assertEquals("Updated", updated.displayName)
            assertEquals("avatar-new", updated.avatarHash)
            assertTrue(assertNotNull(updated.updatedAt) > updated.createdAt)
            assertNotEquals(original.updatedAt, updated.updatedAt)
            assertNotNull(profileStore.findByDiscordUserIdAndGuildId("user-1", "guild-2"))
            assertEquals(2, profileStore.findAllByGuildId("guild-1").size)
        }
    }

    @Test
    fun `bulk lookup returns requested guild profiles with aggregated facts`() {
        runTest {
            resetDatabase()
            val requested = profileStore.insert("user-1", "guild-1", "Dobby", null)
            profileStore.insert("user-2", "guild-1", "Other", null)
            profileStore.insert("user-1", "guild-2", "Other guild", null)
            factStore.insert(requested.id.toString(), "Remember me", "USER_SUBMISSION")

            val results =
                profileStore.findAllByDiscordUserIdsAndGuildId(
                    listOf("user-1", "missing-user"),
                    "guild-1",
                )

            assertEquals(listOf("user-1"), results.map { it.discordUserId })
            assertContains(results.single().factsJson, "Remember me")
        }
    }

    private fun profile(
        userId: String,
        guildId: String,
        displayName: String?,
        avatarHash: String?,
    ): UserProfileCreateRequest = UserProfileCreateRequest(userId, guildId, displayName, avatarHash)
}
