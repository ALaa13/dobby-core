package com.example.dobby.controller.web

import com.example.dobby.dto.ApiResponse
import com.example.dobby.dto.roast.RoastLogDbResponse
import com.example.dobby.dto.user.UserProfileResponse
import com.example.dobby.service.DashboardService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/dashboard")
class DashboardController(
    private val dashboardService: DashboardService,
) {
    @GetMapping("facts/{guildId}")
    suspend fun getAllGuildFacts(
        @PathVariable("guildId") guildId: String,
    ): List<UserProfileResponse> = dashboardService.getAllFactsByGuildId(guildId)

    @DeleteMapping("facts/{factId}")
    suspend fun deleteGuildFact(
        @PathVariable("factId") factId: String,
    ): ApiResponse {
        dashboardService.deleteFact(factId)
        return ApiResponse(
            success = true,
            message = "Fact deleted successfully.",
        )
    }

    @DeleteMapping("facts/{guildId}/reset")
    suspend fun resetGuildFacts(
        @PathVariable("guildId") guildId: String,
    ): ApiResponse {
        dashboardService.purgeGuildFacts(guildId)
        return ApiResponse(
            success = true,
            message = "Fact deleted successfully.",
        )
    }

    @GetMapping("roasts/{guildId}")
    suspend fun getAllGuildRoasts(
        @PathVariable("guildId") guildId: String,
    ): List<RoastLogDbResponse> = dashboardService.getAllRoastByGuildId(guildId)
}
