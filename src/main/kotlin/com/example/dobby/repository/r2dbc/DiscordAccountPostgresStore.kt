package com.example.dobby.repository.r2dbc

import com.example.dobby.entity.DiscordAccountEntity
import io.r2dbc.spi.Row
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class DiscordAccountPostgresStore(
    private val databaseClient: DatabaseClient,
) {
    suspend fun upsert(
        discordUserId: String,
        encryptedToken: String,
    ): DiscordAccountEntity =
        databaseCall("Saving Discord account") {
            // Conflict updates rotate the encrypted credential without duplicating the Discord account identity.
            databaseClient
                .sql(
                    """
                    INSERT INTO discord_accounts (
                        discord_user_id,
                        encrypted_token
                    )
                    VALUES (
                        :discordUserId,
                        :encryptedToken
                    )
                    ON CONFLICT (discord_user_id) DO UPDATE
                    SET encrypted_token = EXCLUDED.encrypted_token
                    RETURNING
                        discord_user_id,
                        encrypted_token,
                        created_at
                    """.trimIndent(),
                ).bind("discordUserId", discordUserId)
                .bind("encryptedToken", encryptedToken)
                .map { row, _ -> row.toDiscordAccountEntity() }
                .one()
                .awaitSingle()
        }

    suspend fun findByDiscordUserId(discordUserId: String): DiscordAccountEntity? =
        databaseCall("Finding Discord account") {
            databaseClient
                .sql(
                    """
                    SELECT
                        discord_user_id,
                        encrypted_token,
                        created_at
                    FROM discord_accounts
                    WHERE discord_user_id = :discordUserId
                    """.trimIndent(),
                ).bind("discordUserId", discordUserId)
                .map { row, _ -> row.toDiscordAccountEntity() }
                .one()
                .awaitSingleOrNull()
        }

    private fun Row.toDiscordAccountEntity(): DiscordAccountEntity =
        DiscordAccountEntity(
            discordUserId = requireNotNull(get("discord_user_id", String::class.java)),
            encryptedToken =
                requireNotNull(
                    get("encrypted_token", String::class.java),
                ),
            createdAt = requireNotNull(get("created_at", OffsetDateTime::class.java)),
        )
}
