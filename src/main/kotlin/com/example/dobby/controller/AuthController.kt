package com.example.dobby.controller

import com.example.dobby.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    // Send the user to Discord's authorization screen
    @GetMapping("/login")
    fun redirectToDiscord(): ResponseEntity<Void> {
        val loginUri = authService.getDiscordLoginUri()
        return ResponseEntity.status(HttpStatus.FOUND).location(loginUri).build()
    }

    // Catch the code from Discord and issue our own JWT
    @GetMapping("/callback")
    suspend fun discordCallback(@RequestParam("code") code: String?): ResponseEntity<Any> {
        if (code == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "No authorization code provided"))
        }
        val redirectDashboardUri = authService.handleCallbackAndGenerateRedirect(code)
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectDashboardUri).build()
    }
}