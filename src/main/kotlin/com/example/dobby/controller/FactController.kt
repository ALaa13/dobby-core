package com.example.dobby.controller

import com.example.dobby.dto.DiscordFactRequest
import com.example.dobby.dto.UserFactResponse
import com.example.dobby.service.FactService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/fact")
@RestController
class FactController(
    private val factService: FactService
) {
    @GetMapping
    suspend fun getFacts(
        @RequestParam("discord_user_id") discordUserId: String,
        @RequestParam("guild_id") guildId: String
    ): List<UserFactResponse> {
        return factService.getFacts(discordUserId, guildId)
    }

    @PostMapping
    suspend fun saveFact(@Valid @RequestBody request: DiscordFactRequest) {
        return factService.saveFact(request)
    }

    @DeleteMapping
    suspend fun resetFacts(
        @RequestParam("discord_user_id") discordUserId: String,
        @RequestParam("guild_id") guildId: String
    ): ResponseEntity<Void> {
        val isReset = factService.resetFacts(discordUserId, guildId)
        return if (isReset) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}