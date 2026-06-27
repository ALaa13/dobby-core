package com.example.dobby.dto.discord

import com.example.dobby.util.ValidationConstants.DISCORD_ID_MSG
import com.example.dobby.util.ValidationConstants.DISCORD_ID_REGEX
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import kotlinx.serialization.Serializable

@Serializable
data class DiscordFactRequest(
    @field:NotBlank(message = "Fact content cannot be blank")
    val fact: String,
    @field:NotBlank
    @field:Pattern(regexp = DISCORD_ID_REGEX, message = DISCORD_ID_MSG)
    val discordUserId: String,
    @field:NotBlank
    @field:Pattern(regexp = DISCORD_ID_REGEX, message = DISCORD_ID_MSG)
    val guildId: String,
    val displayName: String?,
    val avatarHash: String?,
)
