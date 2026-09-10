package com.example.dobby.repository.r2dbc

import com.example.dobby.exception.DobbyException
import io.r2dbc.spi.R2dbcException
import kotlinx.coroutines.CancellationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException

private val databaseLog = LoggerFactory.getLogger("DatabasePersistence")

internal suspend fun <T> databaseCall(
    contextMessage: String,
    block: suspend () -> T,
): T =
    try {
        block()
    } catch (exception: DobbyException) {
        throw exception
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: DataAccessException) {
        databaseLog.error("{} failed", contextMessage, exception)
        throw DobbyException.DatabaseException(
            contextMessage,
            sqlState = (exception.cause as? R2dbcException)?.sqlState,
            cause = exception,
        )
    } catch (exception: R2dbcException) {
        databaseLog.error("{} failed", contextMessage, exception)
        throw DobbyException.DatabaseException(contextMessage, cause = exception)
    } catch (exception: IllegalArgumentException) {
        databaseLog.error("Result mapping failed during {}", contextMessage, exception)
        throw DobbyException.DataMappingException(contextMessage, exception)
    } catch (exception: Exception) {
        databaseLog.error("Unexpected failure during {}", contextMessage, exception)
        throw DobbyException.GeneralException("Unexpected failure during: $contextMessage", exception)
    }
