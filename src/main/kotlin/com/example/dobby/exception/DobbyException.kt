package com.example.dobby.exception

sealed class DobbyException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    class DatabaseException(
        message: String,
        sqlState: String? = null,
        cause: Throwable? = null,
    ) : DobbyException(
            buildString {
                append("Database Failure: $message")
                if (sqlState != null) {
                    append("\n $sqlState")
                }
            },
            cause,
        )

    class NetworkTimeoutException(
        message: String,
        targetService: String,
        cause: Throwable? = null,
    ) : DobbyException("Network timeout communicating with $targetService: $message", cause)

    class AiModelException(
        message: String,
        targetService: String,
        cause: Throwable? = null,
    ) : DobbyException("AI model error from $targetService: $message", cause)

    class ProfileNotFoundException(
        discordUserId: String?,
        guildId: String?,
    ) : DobbyException("Profile Not Found: $discordUserId\n $guildId", null)

    class DataMappingException(
        message: String,
        cause: Throwable? = null,
    ) : DobbyException("Failed to decode or parse internal data: $message", cause)

    class AuthorizationException(
        message: String,
        cause: Throwable? = null,
    ) : DobbyException("Authorization failed: $message", cause)

    class InvalidAuthenticationRequestException(
        message: String,
        cause: Throwable? = null,
    ) : DobbyException("Invalid authentication request: $message", cause)

    class JWTException(
        message: String,
        cause: Throwable? = null,
    ) : DobbyException("JWT processing error: $message", cause)

    class LogStreamException(
        message: String,
        cause: Throwable? = null,
    ) : DobbyException("Log stream error: $message", cause)

    class GeneralException(
        message: String,
        cause: Throwable? = null,
    ) : DobbyException("General error: $message", cause)
}
