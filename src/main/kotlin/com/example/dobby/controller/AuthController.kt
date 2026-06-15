package com.example.dobby.controller

import com.example.dobby.service.DiscordAuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: DiscordAuthService
) {

    @GetMapping("/login")
    fun redirectToDiscord(): ResponseEntity<Void> {
        val loginUri = authService.getDiscordLoginUri()
        return ResponseEntity.status(HttpStatus.FOUND).location(loginUri).build()
    }

    @GetMapping("/callback")
    suspend fun discordCallback(@RequestParam("code", required = false) code: String?): ResponseEntity<Void> {
        val redirectDashboardUri = authService.handleCallbackAndGenerateRedirect(code)
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectDashboardUri).build()
    }
}