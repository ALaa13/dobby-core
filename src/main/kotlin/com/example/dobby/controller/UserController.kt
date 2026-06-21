package com.example.dobby.controller

import com.example.dobby.dto.DiscordDashboardResponse
import com.example.dobby.service.UserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService
) {

    @GetMapping("/me")
    suspend fun getCurrentUser(principal: Principal): DiscordDashboardResponse {
        val userIdFromJwt = principal.name
        return userService.getCurrentUser(userIdFromJwt)
    }
}