package com.example.dobby.supabase

import com.example.dobby.exception.DobbyException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.ktor.client.plugins.*
import kotlinx.serialization.SerializationException
import org.slf4j.LoggerFactory

suspend inline fun <reified T> safeDbCall(contextMessage: String, crossinline block: suspend () -> T): T {
    val log = LoggerFactory.getLogger(T::class.java)
    return try {
        block()
    } catch (e: PostgrestRestException) {
        log.error("Postgrest SQL error during ($contextMessage): Code ${e.statusCode} - ${e.message}")
        throw DobbyException.DatabaseException("$contextMessage: ${e.message}", e.statusCode.toString(), e)
    } catch (e: HttpRequestTimeoutException) {
        log.error("Network timeout connecting to Supabase during ($contextMessage): ${e.message}")
        throw DobbyException.NetworkTimeoutException("Database timeout during: $contextMessage", "Supabase", e)
    } catch (e: SerializationException) {
        log.error("Data structure mapping mismatch during ($contextMessage): ${e.message}")
        throw DobbyException.DataMappingException("Failed to decode response during: $contextMessage", e)
    } catch (e: Exception) {
        log.error("Unexpected critical error during ($contextMessage): ${e.message}")
        throw DobbyException.GeneralException("Unexpected failure during: $contextMessage", e)
    }
}
