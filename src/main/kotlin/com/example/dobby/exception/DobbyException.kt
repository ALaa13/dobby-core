package com.example.dobby.exception

sealed class DobbyException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    // 1. Database-specific errors (Supabase connection issues, pool exhaustion)
    class DatabaseException(
        message: String,
        sqlState: String? = null,
        cause: Throwable? = null
    ) : DobbyException("Database Failure: $message\n $sqlState", cause)

    // 2. Network Timeouts (Supabase cold starts, API dropouts)
    class NetworkTimeoutException(
        message: String,
        targetService: String,
        cause: Throwable? = null
    ) : DobbyException("Network timeout communicating with $targetService: $message", cause)

    // 3. AI model failures (Gemini API errors, response parsing issues)
    class AiModelException(
        message: String,
        targetService: String,
        cause: Throwable? = null
    ) : DobbyException("AI model error from $targetService: $message", cause)

    class ProfileNotFoundException(
        discordUserId: String,
        guildId: String,
    ) : DobbyException("Profile Not Found: $discordUserId\n $guildId", null)

    // 4. Serialization failures (Data model mismatches)
    class DataMappingException(
        message: String,
        cause: Throwable? = null
    ) : DobbyException("Failed to decode or parse internal data: $message", cause)

    // General Unmanaged error
    class GeneralException(
        message: String,
        cause: Throwable? = null
    ) : DobbyException("General error: $message", cause)
}