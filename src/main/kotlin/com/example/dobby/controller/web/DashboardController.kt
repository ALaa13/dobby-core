package com.example.dobby.controller.web

import com.example.dobby.dto.ApiResponse
import com.example.dobby.dto.roast.RoastLogDbResponse
import com.example.dobby.dto.user.UserProfileResponse
import com.example.dobby.service.DashboardService
import com.example.dobby.util.ValidationConstants.DISCORD_ID_MSG
import com.example.dobby.util.ValidationConstants.DISCORD_ID_REGEX
import com.example.dobby.util.ValidationConstants.UUID_MSG
import com.example.dobby.util.ValidationConstants.UUID_REGEX
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/dashboard")
@Validated
class DashboardController(
    private val dashboardService: DashboardService,
) {
    @GetMapping("facts/{guildId}")
    suspend fun getAllGuildFacts(
        @PathVariable("guildId")
        @NotBlank
        @Pattern(regexp = DISCORD_ID_REGEX, message = DISCORD_ID_MSG)
        guildId: String,
    ): List<UserProfileResponse> = dashboardService.getAllFactsByGuildId(guildId)

    @DeleteMapping("facts/{factId}")
    suspend fun deleteGuildFact(
        @PathVariable("factId")
        @NotBlank
        @Pattern(regexp = UUID_REGEX, message = UUID_MSG)
        factId: String,
    ): ApiResponse {
        dashboardService.deleteFact(factId)
        return ApiResponse(
            success = true,
            message = "Fact deleted successfully.",
        )
    }

    @DeleteMapping("facts/{guildId}/reset")
    suspend fun resetGuildFacts(
        @PathVariable("guildId")
        @NotBlank
        @Pattern(regexp = DISCORD_ID_REGEX, message = DISCORD_ID_MSG)
        guildId: String,
    ): ApiResponse {
        dashboardService.purgeGuildFacts(guildId)
        return ApiResponse(
            success = true,
            message = "All guild facts reset successfully.",
        )
    }

    @GetMapping("roasts/{guildId}")
    suspend fun getAllGuildRoasts(
        @PathVariable("guildId")
        @NotBlank
        @Pattern(regexp = DISCORD_ID_REGEX, message = DISCORD_ID_MSG)
        guildId: String,
    ): List<RoastLogDbResponse> = dashboardService.getAllRoastByGuildId(guildId)
}
