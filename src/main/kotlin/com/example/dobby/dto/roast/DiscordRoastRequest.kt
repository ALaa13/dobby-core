package com.example.dobby.dto.roast

import com.example.dobby.dto.discord.DiscordChatMessage
import com.example.dobby.util.ValidationConstants.DISCORD_ID_MSG
import com.example.dobby.util.ValidationConstants.DISCORD_ID_REGEX
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import kotlinx.serialization.Serializable

@Serializable
data class DiscordRoastRequest(
    @field:NotBlank
    @field:Pattern(regexp = DISCORD_ID_REGEX, message = DISCORD_ID_MSG)
    val channelId: String,
    @field:NotBlank
    @field:Pattern(regexp = DISCORD_ID_REGEX, message = DISCORD_ID_MSG)
    val guildId: String,
    @field:NotEmpty(message = "Message history list cannot be empty")
    @field:Valid
    val messages: List<DiscordChatMessage>,
    val persona: String?,
)

fun DiscordRoastRequest.toResult(
    content: String,
    success: Boolean,
) = DiscordRoastResult(channelId, content, success)
