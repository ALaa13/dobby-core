package com.example.dobby.supabase

import com.example.dobby.exception.DobbyException
import com.example.dobby.util.Logging
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.ktor.client.plugins.*
import kotlinx.serialization.SerializationException

suspend inline fun <T> safeDbCall(contextMessage: String, crossinline block: suspend () -> T): T {
    return try {
        block()
    } catch (e: PostgrestRestException) {
        Logging.logError("Postgrest SQL error during ($contextMessage): Code ${e.statusCode} - ${e.message}")
        throw DobbyException.DatabaseException("$contextMessage: ${e.message}", e.statusCode.toString(), e)
    } catch (e: HttpRequestTimeoutException) {
        Logging.logError("Network timeout connecting to Supabase during ($contextMessage): ${e.message}")
        throw DobbyException.NetworkTimeoutException("Database timeout during: $contextMessage", "Supabase", e)
    } catch (e: SerializationException) {
        Logging.logError("Data structure mapping mismatch during ($contextMessage): ${e.message}")
        throw DobbyException.DataMappingException("Failed to decode response during: $contextMessage", e)
    } catch (e: Exception) {
        Logging.logError("Unexpected critical error during ($contextMessage): ${e.message}")
        throw DobbyException.GeneralException("Unexpected failure during: $contextMessage", e)
    }
}
