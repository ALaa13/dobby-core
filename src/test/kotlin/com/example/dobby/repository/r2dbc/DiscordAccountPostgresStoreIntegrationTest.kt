package com.example.dobby.repository.r2dbc

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class DiscordAccountPostgresStoreIntegrationTest : PostgresStoreIntegrationTest() {
    private val accountStore by lazy { DiscordAccountPostgresStore(databaseClient) }

    @Test
    fun `upsert supports lookup and preserves the original creation time`() =
        runTest {
            resetDatabase()
            val inserted = accountStore.upsert("user-1", "encrypted-token-1")
            assertEquals("encrypted-token-1", accountStore.findByDiscordUserId("user-1")?.encryptedToken)

            delay(10.milliseconds)
            val updated = accountStore.upsert("user-1", "encrypted-token-2")

            assertEquals(inserted.createdAt, updated.createdAt)
            assertEquals("encrypted-token-2", updated.encryptedToken)
            assertEquals(updated, accountStore.findByDiscordUserId("user-1"))
        }
}
