package com.example.dobby.util

object ValidationConstants {
    // Discord Snowflake ID: Must be entirely numeric digits
    const val DISCORD_ID_REGEX = "^\\d+$"
    const val DISCORD_ID_MSG = "Invalid Discord ID format"

    // Standard UUID: Matches 8-4-4-4-12 hex structure
    const val UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    const val UUID_MSG = "Identifier must be a valid UUID format"
}
