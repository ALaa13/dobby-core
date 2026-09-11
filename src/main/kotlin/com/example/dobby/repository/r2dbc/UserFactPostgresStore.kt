package com.example.dobby.repository.r2dbc

import com.example.dobby.entity.UserFactEntity
import io.r2dbc.spi.Row
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class UserFactPostgresStore(
    private val databaseClient: DatabaseClient,
) {
    suspend fun insert(
        profileId: String,
        factText: String,
        source: String?,
    ): UserFactEntity {
        return databaseCall("Inserting new user fact") {
            databaseClient
                .sql(INSERT_USER_FACT)
                .bind("profileId", profileId)
                .bind("factText", factText)
                .bindNullable("source", source)
                .map { row, _ -> row.toUserFactEntity() }
                .one()
                .awaitSingle()
        }
    }

    suspend fun deleteById(factId: String): Long {
        return databaseCall("Deleting user fact") {
            databaseClient
                .sql(
                    """
                    DELETE FROM user_facts
                    WHERE id = CAST(:factId AS uuid)
                    """.trimIndent(),
                ).bind("factId", factId)
                .fetch()
                .rowsUpdated()
                .awaitSingle()
        }
    }

    suspend fun deleteAllByProfileId(profileId: String): Long {
        return databaseCall("Deleting user facts by profile") {
            databaseClient
                .sql(
                    """
                    DELETE FROM user_facts
                    WHERE profile_id = CAST(:profileId AS uuid)
                    """.trimIndent(),
                ).bind("profileId", profileId)
                .fetch()
                .rowsUpdated()
                .awaitSingle()
        }
    }

    suspend fun deleteAllByGuildId(guildId: String): Long {
        return databaseCall("Deleting user facts by guild") {
            // Delete only fact rows; profiles remain because historical roast targets still reference their identities.
            databaseClient
                .sql(
                    """
                    DELETE FROM user_facts f
                    USING user_profiles p
                    WHERE f.profile_id = p.id
                      AND p.guild_id = :guildId
                    """.trimIndent(),
                ).bind("guildId", guildId)
                .fetch()
                .rowsUpdated()
                .awaitSingle()
        }
    }

    private fun Row.toUserFactEntity(): UserFactEntity {
        return UserFactEntity(
            id = requireNotNull(get("id", UUID::class.java)),
            profileId = requireNotNull(get("profile_id", UUID::class.java)),
            factText = requireNotNull(get("fact_text", String::class.java)),
            source = get("source", String::class.java),
            createdAt = requireNotNull(get("created_at", OffsetDateTime::class.java)),
            updatedAt = get("updated_at", OffsetDateTime::class.java),
        )
    }

    private companion object {
        val INSERT_USER_FACT =
            """
            INSERT INTO user_facts (
                profile_id,
                fact_text,
                source
            )
            VALUES (
                CAST(:profileId AS uuid),
                :factText,
                :source
            )
            RETURNING
                id,
                profile_id,
                fact_text,
                source,
                created_at,
                updated_at
            """.trimIndent()
    }
}
