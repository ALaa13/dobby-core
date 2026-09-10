package com.example.dobby.repository.r2dbc

import com.example.dobby.dto.user.UserProfileCreateRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.FetchSpec
import reactor.core.publisher.Mono
import kotlin.test.assertContains
import kotlin.test.assertFalse

class UserProfilePostgresStoreTest {
    private val databaseClient = mockk<DatabaseClient>()
    private val executeSpec = mockk<DatabaseClient.GenericExecuteSpec>()
    private val fetchSpec = mockk<FetchSpec<Map<String, Any>>>()
    private val store = UserProfilePostgresStore(databaseClient)

    @BeforeEach
    fun setUp() {
        every { executeSpec.bind(any<String>(), any()) } returns executeSpec
        every { executeSpec.bindNull(any<String>(), any()) } returns executeSpec
        every { executeSpec.fetch() } returns fetchSpec
        every { fetchSpec.rowsUpdated() } returns Mono.just(2L)
    }

    @Test
    fun `upsertProfiles does not create a statement for an empty list`() =
        runTest {
            store.upsertProfiles(emptyList())

            verify(exactly = 0) { databaseClient.sql(any<String>()) }
        }

    @Test
    fun `upsertProfiles sends all profiles in one safely bound statement`() =
        runTest {
            val sql = slot<String>()
            every { databaseClient.sql(capture(sql)) } returns executeSpec
            val profiles =
                listOf(
                    UserProfileCreateRequest(
                        discordUserId = "user-1",
                        guildId = "guild-1",
                        displayName = "First",
                        avatarHash = null,
                    ),
                    UserProfileCreateRequest(
                        discordUserId = "user-2",
                        guildId = "guild-1",
                        displayName = null,
                        avatarHash = "avatar-2",
                    ),
                )

            store.upsertProfiles(profiles)

            verify(exactly = 1) { databaseClient.sql(any<String>()) }
            verify(exactly = 1) { fetchSpec.rowsUpdated() }
            verify { executeSpec.bind("discordUserId0", "user-1") }
            verify { executeSpec.bind("guildId0", "guild-1") }
            verify { executeSpec.bind("displayName0", "First") }
            verify { executeSpec.bindNull("avatarHash0", String::class.java) }
            verify { executeSpec.bind("discordUserId1", "user-2") }
            verify { executeSpec.bindNull("displayName1", String::class.java) }
            verify { executeSpec.bind("avatarHash1", "avatar-2") }

            assertContains(sql.captured, "ON CONFLICT (discord_user_id, guild_id) DO UPDATE")
            assertContains(sql.captured, "updated_at = now()")
            assertFalse(sql.captured.contains("user-1"))
            assertFalse(sql.captured.contains("First"))
        }
}
