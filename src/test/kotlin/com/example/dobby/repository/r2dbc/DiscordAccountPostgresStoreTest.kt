package com.example.dobby.repository.r2dbc

import com.example.dobby.entity.DiscordAccountEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.RowsFetchSpec
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.function.BiFunction
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DiscordAccountPostgresStoreTest {
    private val databaseClient = mockk<DatabaseClient>()
    private val store = DiscordAccountPostgresStore(databaseClient)

    @Test
    fun `upsert binds the already encrypted token and preserves created at`() =
        runTest {
            val executeSpec = mockk<DatabaseClient.GenericExecuteSpec>()
            val rows = mockk<RowsFetchSpec<DiscordAccountEntity>>()
            val sql = slot<String>()
            val entity =
                DiscordAccountEntity(
                    discordUserId = "user-1",
                    encryptedToken = "encrypted-token",
                    createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z"),
                )
            every { databaseClient.sql(capture(sql)) } returns executeSpec
            every { executeSpec.bind(any<String>(), any()) } returns executeSpec
            every {
                executeSpec.map(any<BiFunction<Row, RowMetadata, DiscordAccountEntity>>())
            } returns rows
            every { rows.one() } returns Mono.just(entity)

            val result = store.upsert("user-1", "encrypted-token")

            assertEquals(entity, result)
            verify { executeSpec.bind("discordUserId", "user-1") }
            verify { executeSpec.bind("encryptedToken", "encrypted-token") }
            assertContains(sql.captured, "ON CONFLICT (discord_user_id) DO UPDATE")
            assertContains(sql.captured, "encrypted_token = EXCLUDED.encrypted_token")
            assertFalse(sql.captured.contains("encrypted-token"))
        }

    @Test
    fun `findByDiscordUserId returns null when no row is emitted`() =
        runTest {
            val executeSpec = mockk<DatabaseClient.GenericExecuteSpec>()
            val rows = mockk<RowsFetchSpec<DiscordAccountEntity>>()
            every { databaseClient.sql(any<String>()) } returns executeSpec
            every { executeSpec.bind("discordUserId", "missing-user") } returns executeSpec
            every {
                executeSpec.map(any<BiFunction<Row, RowMetadata, DiscordAccountEntity>>())
            } returns rows
            every { rows.one() } returns Mono.empty()

            val result = store.findByDiscordUserId("missing-user")

            assertEquals(null, result)
            verify(exactly = 1) { databaseClient.sql(any<String>()) }
        }
}
