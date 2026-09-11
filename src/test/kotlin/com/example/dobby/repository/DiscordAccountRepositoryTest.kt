package com.example.dobby.repository

import com.example.dobby.dto.discord.DiscordAccount
import com.example.dobby.entity.DiscordAccountEntity
import com.example.dobby.repository.r2dbc.DiscordAccountPostgresStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals

class DiscordAccountRepositoryTest {
    private val discordAccountStore = mockk<DiscordAccountPostgresStore>()
    private val repository = DiscordAccountRepository(discordAccountStore)

    @Test
    fun `saveDiscordUser stores and returns the encrypted token unchanged`() {
        runTest {
            val account = DiscordAccount("user-1", "encrypted-token")
            coEvery {
                discordAccountStore.upsert("user-1", "encrypted-token")
            } returns
                DiscordAccountEntity(
                    discordUserId = "user-1",
                    encryptedToken = "encrypted-token",
                    createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z"),
                )

            val result = repository.saveDiscordUser(account)

            assertEquals(account, result)
            coVerify(exactly = 1) {
                discordAccountStore.upsert("user-1", "encrypted-token")
            }
        }
    }

    @Test
    fun `findByDiscordUserId returns null when the account does not exist`() {
        runTest {
            coEvery { discordAccountStore.findByDiscordUserId("missing-user") } returns null

            val result = repository.findByDiscordUserId("missing-user")

            assertEquals(null, result)
        }
    }
}
