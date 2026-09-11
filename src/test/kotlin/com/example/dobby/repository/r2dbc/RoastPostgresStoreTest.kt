package com.example.dobby.repository.r2dbc

import com.example.dobby.dto.roast.RoastResult
import com.example.dobby.dto.roast.TargetDamage
import com.example.dobby.exception.DobbyException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.FetchSpec
import org.springframework.r2dbc.core.RowsFetchSpec
import org.springframework.transaction.ReactiveTransaction
import org.springframework.transaction.ReactiveTransactionManager
import reactor.core.publisher.Mono
import java.util.UUID
import java.util.function.BiFunction
import kotlin.test.assertFailsWith

class RoastPostgresStoreTest {
    private val databaseClient = mockk<DatabaseClient>()
    private val transactionManager = mockk<ReactiveTransactionManager>()
    private val transaction = mockk<ReactiveTransaction>(relaxed = true)
    private val roastExecuteSpec = mockk<DatabaseClient.GenericExecuteSpec>()
    private val roastRows = mockk<RowsFetchSpec<UUID>>()
    private val targetExecuteSpec = mockk<DatabaseClient.GenericExecuteSpec>()
    private val targetFetchSpec = mockk<FetchSpec<Map<String, Any>>>()
    private val store = RoastPostgresStore(databaseClient, transactionManager)
    private val roastId = UUID.fromString("30000000-0000-0000-0000-000000000001")

    @BeforeEach
    fun setUp() {
        every { transactionManager.getReactiveTransaction(any()) } returns Mono.just(transaction)
        every { transactionManager.commit(transaction) } returns Mono.empty()
        every { transactionManager.rollback(transaction) } returns Mono.empty()

        every { databaseClient.sql(match<String> { it.contains("INSERT INTO roasts (") }) } returns roastExecuteSpec
        every { roastExecuteSpec.bind(any<String>(), any()) } returns roastExecuteSpec
        every { roastExecuteSpec.bindNull(any<String>(), any()) } returns roastExecuteSpec
        every {
            roastExecuteSpec.map(any<BiFunction<Row, RowMetadata, UUID>>())
        } returns roastRows
        every { roastRows.one() } returns Mono.just(roastId)

        every {
            databaseClient.sql(match<String> { it.contains("INSERT INTO roast_targets (") })
        } returns targetExecuteSpec
        every { targetExecuteSpec.bind(any<String>(), any()) } returns targetExecuteSpec
        every { targetExecuteSpec.fetch() } returns targetFetchSpec
    }

    @Test
    fun `saveRoastResult commits roast and targets together`() {
        runTest {
            every { targetFetchSpec.rowsUpdated() } returns Mono.just(2L)

            store.saveRoastResult("guild-1", "channel-1", roastResult())

            verify(exactly = 1) { transactionManager.commit(transaction) }
            verify(exactly = 0) { transactionManager.rollback(transaction) }
            verify(exactly = 1) {
                databaseClient.sql(match<String> { it.contains("INSERT INTO roast_targets (") })
            }
            verify { targetExecuteSpec.bind("roastId0", roastId) }
            verify { targetExecuteSpec.bind("roastId1", roastId) }
        }
    }

    @Test
    fun `saveRoastResult rolls back the roast when target insertion fails`() {
        runTest {
            every {
                targetFetchSpec.rowsUpdated()
            } returns Mono.error(IllegalStateException("target insert failed"))

            assertFailsWith<DobbyException.GeneralException> {
                store.saveRoastResult("guild-1", "channel-1", roastResult())
            }

            verify(exactly = 0) { transactionManager.commit(transaction) }
            verify(exactly = 1) { transactionManager.rollback(transaction) }
        }
    }

    private fun roastResult(): RoastResult =
        RoastResult(
            text = "Roast",
            persona = null,
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
