package com.example.dobby.repository.r2dbc

import com.example.dobby.entity.UserFactEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.FetchSpec
import org.springframework.r2dbc.core.RowsFetchSpec
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID
import java.util.function.BiFunction
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class UserFactPostgresStoreTest {
    private val databaseClient = mockk<DatabaseClient>()
    private val store = UserFactPostgresStore(databaseClient)

    @Test
    fun `insert binds fact values and uses explicit returning columns`() {
        runTest {
            val executeSpec = mockk<DatabaseClient.GenericExecuteSpec>()
            val rows = mockk<RowsFetchSpec<UserFactEntity>>()
            val sql = slot<String>()
            val entity =
                UserFactEntity(
                    id = UUID.fromString("20000000-0000-0000-0000-000000000001"),
                    profileId = UUID.fromString("10000000-0000-0000-0000-000000000001"),
                    factText = "Likes socks",
                    source = "USER_SUBMISSION",
                    createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z"),
                )
            every { databaseClient.sql(capture(sql)) } returns executeSpec
            every { executeSpec.bind(any<String>(), any()) } returns executeSpec
            every {
                executeSpec.map(any<BiFunction<Row, RowMetadata, UserFactEntity>>())
            } returns rows
            every { rows.one() } returns Mono.just(entity)

            val result =
                store.insert(
                    profileId = entity.profileId.toString(),
                    factText = entity.factText,
                    source = entity.source,
                )

            assertEquals(entity, result)
            verify { executeSpec.bind("profileId", entity.profileId.toString()) }
            verify { executeSpec.bind("factText", "Likes socks") }
            verify { executeSpec.bind("source", "USER_SUBMISSION") }
            assertContains(sql.captured, "RETURNING")
            assertContains(sql.captured, "updated_at")
            assertFalse(sql.captured.contains("Likes socks"))
        }
    }

    @Test
    fun `deleteById executes one bound delete statement`() {
        runTest {
            val executeSpec = mockk<DatabaseClient.GenericExecuteSpec>()
            val fetchSpec = mockk<FetchSpec<Map<String, Any>>>()
            val sql = slot<String>()
            every { databaseClient.sql(capture(sql)) } returns executeSpec
            every { executeSpec.bind("factId", "fact-id") } returns executeSpec
            every { executeSpec.fetch() } returns fetchSpec
            every { fetchSpec.rowsUpdated() } returns Mono.just(1L)

            val count = store.deleteById("fact-id")

            assertEquals(1L, count)
            verify(exactly = 1) { databaseClient.sql(any<String>()) }
            assertContains(sql.captured, "WHERE id = CAST(:factId AS uuid)")
        }
    }

    @Test
    fun `deleteAllByGuildId deletes facts through profiles in one bound statement`() {
        runTest {
            val executeSpec = mockk<DatabaseClient.GenericExecuteSpec>()
            val fetchSpec = mockk<FetchSpec<Map<String, Any>>>()
            val sql = slot<String>()
            every { databaseClient.sql(capture(sql)) } returns executeSpec
            every { executeSpec.bind("guildId", "guild-1") } returns executeSpec
            every { executeSpec.fetch() } returns fetchSpec
            every { fetchSpec.rowsUpdated() } returns Mono.just(2L)

            val count = store.deleteAllByGuildId("guild-1")

            assertEquals(2L, count)
            verify(exactly = 1) { databaseClient.sql(any<String>()) }
            assertContains(sql.captured, "DELETE FROM user_facts f")
            assertContains(sql.captured, "USING user_profiles p")
            assertFalse(sql.captured.contains("guild-1"))
        }
    }
}
