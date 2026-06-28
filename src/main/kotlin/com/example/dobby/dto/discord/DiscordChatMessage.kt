package com.example.dobby.dto.discord

import com.example.dobby.util.ValidationConstants.DISCORD_ID_MSG
import com.example.dobby.util.ValidationConstants.DISCORD_ID_REGEX
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import kotlinx.serialization.Serializable

@Serializable
data class DiscordChatMessage(
    @field:NotBlank(message = "Author display name cannot be blank")
    val displayName: String,
    @field:NotBlank
    @field:Pattern(regexp = DISCORD_ID_REGEX, message = DISCORD_ID_MSG + "inside message history")
    val discordUserId: String,
    @field:NotBlank(message = "Avatar hash cannot be blank")
    val avatarHash: String,
    @field:NotBlank(message = "Chat content message cannot be blank")
    val content: String,
    @field:NotBlank(message = "Timestamp cannot be blank")
    val timestamp: String,
)
