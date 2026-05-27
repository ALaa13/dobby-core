package com.example.dobby.controller

import com.example.dobby.dto.DiscordFactRequest
import com.example.dobby.dto.UserFactResponse
import com.example.dobby.service.FactService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/fact")
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
        return factService.getFacts(discordUserId, guildId)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun saveFact(@Valid @RequestBody request: DiscordFactRequest) {
        return factService.saveFact(request)
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun resetFacts(
        @RequestParam("discord_user_id") discordUserId: String,
        @RequestParam("guild_id") guildId: String
    ) {
        factService.resetFacts(discordUserId, guildId)
    }
}