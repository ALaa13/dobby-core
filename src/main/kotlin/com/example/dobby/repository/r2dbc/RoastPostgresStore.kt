package com.example.dobby.repository.r2dbc

import com.example.dobby.dto.roast.RoastResult
import com.example.dobby.dto.roast.TargetDamage
import com.example.dobby.repository.r2dbc.projection.RoastWithTargetsRow
import com.example.dobby.repository.r2dbc.query.ROAST_WITH_TARGETS_SELECT
import io.r2dbc.spi.Row
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class RoastPostgresStore(
    private val databaseClient: DatabaseClient,
    transactionManager: ReactiveTransactionManager,
) {
    private val transactionalOperator = TransactionalOperator.create(transactionManager)

    suspend fun saveRoastResult(
        guildId: String,
        channelId: String,
        result: RoastResult,
    ) {
        databaseCall("Saving roast log and targets") {
            transactionalOperator.executeAndAwait {
                val roastId = insertRoast(guildId, channelId, result)
                insertTargets(roastId, guildId, result.targets)
            }
        }
    }

    suspend fun findAllByGuildId(guildId: String): List<RoastWithTargetsRow> =
        databaseCall("Fetching roasts and targets by guild") {
            databaseClient
                .sql(
                    ROAST_WITH_TARGETS_SELECT +
                        """
                        WHERE r.guild_id = :guildId
                        GROUP BY r.id
                        ORDER BY r.created_at DESC, r.id DESC
                        """,
                ).bind("guildId", guildId)
                .map { row, _ -> row.toRoastWithTargetsRow() }
                .all()
                .collectList()
                .awaitSingle()
        }

    private suspend fun insertRoast(
        guildId: String,
        channelId: String,
        result: RoastResult,
    ): UUID =
        databaseClient
            .sql(INSERT_ROAST)
            .bind("guildId", guildId)
            .bind("channelId", channelId)
            .bind("roastText", result.text)
            .bindNullable("personaUsed", result.persona)
            .bind("primaryTargetId", result.primaryTargetId)
            .bind("clappedTheMostId", result.clappedTheMostId)
            .bind("burnAccuracy", result.burnAccuracy.toShort())
            .bind("severityScore", result.severityScore.toShort())
            .map { row, _ -> requireNotNull(row.get("id", UUID::class.java)) }
            .one()
            .awaitSingle()

    private suspend fun insertTargets(
        roastId: UUID,
        guildId: String,
        targets: List<TargetDamage>,
    ) {
        if (targets.isEmpty()) return

        val valuesClause =
            targets.indices.joinToString(",\n") { index ->
                "(:roastId$index, :discordUserId$index, :guildId$index, :damageReason$index)"
            }
        val sql =
            """
            INSERT INTO roast_targets (
                roast_id,
                discord_user_id,
                guild_id,
                damage_reason
            )
            VALUES
                $valuesClause
            """.trimIndent()

        var executeSpec = databaseClient.sql(sql)
        targets.forEachIndexed { index, target ->
            executeSpec =
                executeSpec
                    .bind("roastId$index", roastId)
                    .bind("discordUserId$index", target.userId)
                    .bind("guildId$index", guildId)
                    .bind("damageReason$index", target.reason)
        }

        executeSpec.fetch().rowsUpdated().awaitSingle()
    }

    private fun Row.toRoastWithTargetsRow(): RoastWithTargetsRow =
        RoastWithTargetsRow(
            id = requireNotNull(get("id", UUID::class.java)),
            guildId = requireNotNull(get("guildId", String::class.java)),
            channelId = requireNotNull(get("channelId", String::class.java)),
            roastText = requireNotNull(get("roastText", String::class.java)),
            personaUsed = get("personaUsed", String::class.java),
            primaryTargetId = requireNotNull(get("primaryTargetId", String::class.java)),
            clappedTheMostId = requireNotNull(get("clappedTheMostId", String::class.java)),
            burnAccuracy = requireNotNull(get("burnAccuracy", Int::class.javaObjectType)),
            severityScore = requireNotNull(get("severityScore", Int::class.javaObjectType)),
            createdAt = requireNotNull(get("createdAt", OffsetDateTime::class.java)),
            targetsJson = requireNotNull(get("targetsJson", String::class.java)),
        )

    private companion object {
        val INSERT_ROAST =
            """
            INSERT INTO roasts (
                guild_id,
                channel_id,
                roast_text,
                persona_used,
                primary_target_id,
                clapped_the_most_id,
                burn_accuracy,
                severity_score
            )
            VALUES (
                :guildId,
                :channelId,
                :roastText,
                :personaUsed,
                :primaryTargetId,
                :clappedTheMostId,
                :burnAccuracy,
                :severityScore
            )
            RETURNING
                id,
                guild_id,
                channel_id,
                roast_text,
                persona_used,
                primary_target_id,
                clapped_the_most_id,
                burn_accuracy,
                severity_score,
                created_at
            """.trimIndent()
    }
}
