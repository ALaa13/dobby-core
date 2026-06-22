package com.example.dobby.controller.web

import com.example.dobby.dto.ApiResponse
import com.example.dobby.dto.UserProfileResponse
import com.example.dobby.service.FactService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/guild")
class GuildController(
    private val factService: FactService
) {

    @GetMapping("/{guildId}")
    suspend fun getAllGuildFacts(
        @PathVariable("guildId") guildId: String
    ): List<UserProfileResponse> {
        return factService.getAllFactsByGuild(guildId)
    }


    @DeleteMapping("facts/{factId}")
    suspend fun deleteGuildFact(
        @PathVariable("factId") factId: String
    ): ApiResponse {
        factService.deleteUserFact(factId)
        return ApiResponse(
            success = true,
            message = "Fact deleted successfully."
        )
    }
}