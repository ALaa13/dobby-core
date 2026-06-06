package com.example.dobby.controller

import com.example.dobby.dto.UserProfileResponse
import com.example.dobby.service.UserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {

    @GetMapping("/me")
    suspend fun getCurrentUser(): UserProfileResponse {
        return userService.getCurrentUser()
    }
}