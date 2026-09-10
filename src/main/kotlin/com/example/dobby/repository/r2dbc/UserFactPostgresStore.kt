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
    ): UserFactEntity =
        databaseCall("Inserting new user fact") {
            databaseClient
                .sql(INSERT_USER_FACT)
                .bind("profileId", profileId)
                .bind("factText", factText)
                .bindNullable("source", source)
                .map { row, _ -> row.toUserFactEntity() }
                .one()
                .awaitSingle()
        }

    suspend fun deleteById(factId: String): Long =
        databaseCall("Deleting user fact") {
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

    suspend fun deleteAllByProfileId(profileId: String): Long =
        databaseCall("Deleting user facts by profile") {
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

    suspend fun findAllByProfileId(profileId: UUID): List<UserFactEntity> =
        databaseCall("Finding user facts by profile") {
            databaseClient
                .sql(
                    """
                    SELECT
                        id,
                        profile_id,
                        fact_text,
                        source,
                        created_at,
                        updated_at
                    FROM user_facts
                    WHERE profile_id = :profileId
                    ORDER BY created_at ASC, id ASC
                    """.trimIndent(),
                ).bind("profileId", profileId)
                .map { row, _ -> row.toUserFactEntity() }
                .all()
                .collectList()
                .awaitSingle()
        }

    private fun Row.toUserFactEntity(): UserFactEntity =
        UserFactEntity(
            id = requireNotNull(get("id", UUID::class.java)),
            profileId = requireNotNull(get("profile_id", UUID::class.java)),
            factText = requireNotNull(get("fact_text", String::class.java)),
            source = get("source", String::class.java),
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
