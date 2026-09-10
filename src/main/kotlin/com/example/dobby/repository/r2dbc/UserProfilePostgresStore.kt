package com.example.dobby.repository.r2dbc

import com.example.dobby.dto.user.UserProfileCreateRequest
import com.example.dobby.entity.UserProfileEntity
import com.example.dobby.repository.r2dbc.projection.UserProfileWithFactsRow
import com.example.dobby.repository.r2dbc.query.USER_PROFILE_WITH_FACTS_SELECT
import io.r2dbc.spi.Row
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class UserProfilePostgresStore(
    private val databaseClient: DatabaseClient,
) {
    suspend fun findByDiscordUserIdAndGuildId(
        discordUserId: String,
        guildId: String,
    ): UserProfileWithFactsRow? =
        databaseCall("Finding user profile") {
            databaseClient
                .sql(
                    USER_PROFILE_WITH_FACTS_SELECT +
                        """
                        WHERE p.discord_user_id = :discordUserId
                          AND p.guild_id = :guildId
                        GROUP BY p.id
                        """,
                ).bind("discordUserId", discordUserId)
                .bind("guildId", guildId)
                .map { row, _ -> row.toUserProfileWithFactsRow() }
                .one()
                .awaitSingleOrNull()
        }

    suspend fun findAllByGuildId(guildId: String): List<UserProfileWithFactsRow> =
        databaseCall("Finding user profiles by guild") {
            databaseClient
                .sql(
                    USER_PROFILE_WITH_FACTS_SELECT +
                        """
                        WHERE p.guild_id = :guildId
                        GROUP BY p.id
                        ORDER BY p.created_at ASC, p.id ASC
                        """,
                ).bind("guildId", guildId)
                .map { row, _ -> row.toUserProfileWithFactsRow() }
                .all()
                .collectList()
                .awaitSingle()
        }

    suspend fun findAllByDiscordUserIdsAndGuildId(
        discordUserIds: Collection<String>,
        guildId: String,
    ): List<UserProfileWithFactsRow> {
        if (discordUserIds.isEmpty()) return emptyList()

        return databaseCall("Finding user profiles by Discord IDs and guild") {
            val placeholders = discordUserIds.indices.joinToString(", ") { index -> ":discordUserId$index" }
            val sql =
                USER_PROFILE_WITH_FACTS_SELECT +
                    """
                    WHERE p.guild_id = :guildId
                      AND p.discord_user_id IN ($placeholders)
                    GROUP BY p.id
                    ORDER BY p.created_at ASC, p.id ASC
                    """

            var executeSpec = databaseClient.sql(sql).bind("guildId", guildId)
            discordUserIds.forEachIndexed { index, discordUserId ->
                executeSpec = executeSpec.bind("discordUserId$index", discordUserId)
            }

            executeSpec
                .map { row, _ -> row.toUserProfileWithFactsRow() }
                .all()
                .collectList()
                .awaitSingle()
        }
    }

    suspend fun insert(
        discordUserId: String,
        guildId: String,
        displayName: String?,
        avatarHash: String?,
    ): UserProfileEntity =
        databaseCall("Inserting user profile") {
            databaseClient
                .sql(INSERT_USER_PROFILE)
                .bind("discordUserId", discordUserId)
                .bind("guildId", guildId)
                .bindNullable("displayName", displayName)
                .bindNullable("avatarHash", avatarHash)
                .map { row, _ -> row.toUserProfileEntity() }
                .one()
                .awaitSingle()
        }

    suspend fun upsertProfiles(profiles: List<UserProfileCreateRequest>) {
        if (profiles.isEmpty()) return

        databaseCall("Batch upserting user profiles") {
            val valuesClause =
                profiles.indices.joinToString(",\n") { index ->
                    "(:discordUserId$index, :guildId$index, :displayName$index, :avatarHash$index)"
                }
            val sql =
                """
                INSERT INTO user_profiles (
                    discord_user_id,
                    guild_id,
                    display_name,
                    avatar_hash
                )
                VALUES
                    $valuesClause
                ON CONFLICT (discord_user_id, guild_id) DO UPDATE
                SET
                    display_name = EXCLUDED.display_name,
                    avatar_hash = EXCLUDED.avatar_hash,
                    updated_at = now()
                """.trimIndent()

            var executeSpec = databaseClient.sql(sql)

            profiles.forEachIndexed { index, profile ->
                executeSpec =
                    executeSpec
                        .bind("discordUserId$index", profile.discordUserId)
                        .bind("guildId$index", profile.guildId)
                        .bindNullable("displayName$index", profile.displayName)
                        .bindNullable("avatarHash$index", profile.avatarHash)
            }

            executeSpec.fetch().rowsUpdated().awaitSingle()
        }
    }

    private fun Row.toUserProfileWithFactsRow(): UserProfileWithFactsRow =
        UserProfileWithFactsRow(
            id = requireNotNull(get("id", UUID::class.java)),
            discordUserId = requireNotNull(get("discordUserId", String::class.java)),
            guildId = requireNotNull(get("guildId", String::class.java)),
            displayName = get("displayName", String::class.java),
            avatarHash = get("avatarHash", String::class.java),
            createdAt = requireNotNull(get("createdAt", OffsetDateTime::class.java)),
            updatedAt = get("updatedAt", OffsetDateTime::class.java),
            factsJson = requireNotNull(get("factsJson", String::class.java)),
        )

    private fun Row.toUserProfileEntity(): UserProfileEntity =
        UserProfileEntity(
            id = requireNotNull(get("id", UUID::class.java)),
            discordUserId = requireNotNull(get("discord_user_id", String::class.java)),
            guildId = requireNotNull(get("guild_id", String::class.java)),
            displayName = get("display_name", String::class.java),
            avatarHash = get("avatar_hash", String::class.java),
            createdAt = requireNotNull(get("created_at", OffsetDateTime::class.java)),
            updatedAt = get("updated_at", OffsetDateTime::class.java),
        )

    private inline fun <reified T : Any> DatabaseClient.GenericExecuteSpec.bindNullable(
        name: String,
        value: T?,
    ): DatabaseClient.GenericExecuteSpec =
        if (value == null) {
            bindNull(name, T::class.java)
        } else {
            bind(name, value)
        }

    private companion object {
        val INSERT_USER_PROFILE =
            """
            INSERT INTO user_profiles (
                discord_user_id,
                guild_id,
                display_name,
                avatar_hash
            )
            VALUES (
                :discordUserId,
                :guildId,
                :displayName,
                :avatarHash
            )
            RETURNING
                id,
                discord_user_id,
                guild_id,
                display_name,
                avatar_hash,
                created_at,
                updated_at
            """.trimIndent()
    }
}
