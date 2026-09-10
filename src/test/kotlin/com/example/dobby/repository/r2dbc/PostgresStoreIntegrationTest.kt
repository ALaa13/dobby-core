package com.example.dobby.repository.r2dbc

import io.r2dbc.spi.ConnectionFactories
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.r2dbc.core.DatabaseClient
import org.testcontainers.postgresql.PostgreSQLContainer
import java.nio.file.Files
import java.nio.file.Path

abstract class PostgresStoreIntegrationTest {
    protected val connectionFactory by lazy {
        ConnectionFactories.get(
            "r2dbc:postgresql://${postgres.username}:${postgres.password}" +
                "@${postgres.host}:${postgres.firstMappedPort}/${postgres.databaseName}",
        )
    }

    protected val databaseClient by lazy { DatabaseClient.create(connectionFactory) }
    protected val transactionManager by lazy { R2dbcTransactionManager(connectionFactory) }

    protected suspend fun resetDatabase() {
        databaseClient
            .sql("DROP TABLE IF EXISTS roast_targets, roasts, user_facts, discord_accounts, user_profiles CASCADE")
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        schemaStatements.forEach { statement ->
            databaseClient
                .sql(statement)
                .fetch()
                .rowsUpdated()
                .awaitSingle()
        }
    }

    private val schemaStatements by lazy {
        Files
            .readString(Path.of("db/schema.sql"))
            .split(';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    companion object {
        @JvmField
        val postgres = PostgreSQLContainer("postgres:17-alpine").apply { start() }
    }
}
