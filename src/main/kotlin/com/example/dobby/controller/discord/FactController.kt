package com.example.dobby.controller.discord

import com.example.dobby.dto.ApiResponse
import com.example.dobby.dto.discord.DiscordFactRequest
import com.example.dobby.dto.fact.UserFactResponse
import com.example.dobby.service.FactService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RequestMapping("/fact")
@RestController
class FactController(
    private val factService: FactService
) {
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    suspend fun getFacts(
        @RequestParam("discord_user_id") discordUserId: String,
        @RequestParam("guild_id") guildId: String
    ): List<UserFactResponse> {
        return factService.getUserFacts(discordUserId, guildId)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun saveFact(@Valid @RequestBody request: DiscordFactRequest): ApiResponse {
        factService.saveUserFact(request)
        return ApiResponse(
            success = true,
            message = "Fact saved successfully."
        )
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    suspend fun resetFacts(
        @RequestParam("discord_user_id") discordUserId: String,
        @RequestParam("guild_id") guildId: String
    ): ApiResponse {
        factService.resetUserFacts(discordUserId, guildId)
        return ApiResponse(
            success = true,
            message = "All facts for this user have been successfully reset."
        )
    }
}