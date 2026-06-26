package com.example.dobby.controller.web

import com.example.dobby.dto.discord.DiscordDashboardResponse
import com.example.dobby.service.AdminUserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/users")
class UserController(
    private val adminUserService: AdminUserService
) {

    @GetMapping("/me")
    suspend fun getCurrentUser(principal: Principal): DiscordDashboardResponse {
        val userIdFromJwt = principal.name
        return adminUserService.getCurrentUser(userIdFromJwt)
    }
}