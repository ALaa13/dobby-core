package com.example.dobby.exception

sealed class DobbyException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    // Database-specific errors (Supabase connection issues, pool exhaustion)
    class DatabaseException(
        message: String,
        sqlState: String? = null,
        cause: Throwable? = null
    ) : DobbyException("Database Failure: $message\n $sqlState", cause)

    // Network Timeouts (Supabase cold starts, API dropouts)
    class NetworkTimeoutException(
        message: String,
        targetService: String,
        cause: Throwable? = null
    ) : DobbyException("Network timeout communicating with $targetService: $message", cause)

    // AI model failures (Gemini API errors, response parsing issues)
    class AiModelException(
        message: String,
        targetService: String,
        cause: Throwable? = null
    ) : DobbyException("AI model error from $targetService: $message", cause)

    // profile failures (Missing profiles, invalid profile data)
    class ProfileNotFoundException(
        discordUserId: String?,
        guildId: String?,
    ) : DobbyException("Profile Not Found: $discordUserId\n $guildId", null)

    // Serialization failures (Data model mismatches)
    class DataMappingException(
        message: String,
        cause: Throwable? = null
    ) : DobbyException("Failed to decode or parse internal data: $message", cause)

    // Authorization failures (Invalid Discord tokens, unauthorized access attempts)
    class AuthorizationException(
        message: String,
        cause: Throwable? = null
    ) : DobbyException("Authorization failed: $message", cause)

    // JWT failures (Malformed tokens, signature verification failures)
    class JWTException(
        message: String,
        cause: Throwable? = null
    ) : DobbyException("JWT processing error: $message", cause)

    // Log failures (Failed to write to log stream)
    class LogStreamException(
        message: String,
        cause: Throwable? = null
    ) : DobbyException("Log stream error: $message", cause)

    // General Unmanaged error
    class GeneralException(
        message: String,
        cause: Throwable? = null
    ) : DobbyException("General error: $message", cause)
}