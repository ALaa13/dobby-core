package com.example.dobby.controller

import com.example.dobby.dto.ApiResponse
import com.example.dobby.exception.DobbyException
import com.example.dobby.service.JWTService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Profile("dev")
@RestController
@RequestMapping("/dev")
class DevController(
    @Value($$"${dev.secret.key}") private val devSecret: String,
    private val jwtService: JWTService
) {
    @GetMapping("/token")
    fun getTestToken(@RequestParam secret: String): ApiResponse {
        if (secret != devSecret) {
            throw DobbyException.AuthorizationException("Invalid secret")
        }

        val token = jwtService.generateJWTToken("dev", "dev@example.com")
        return ApiResponse(
            success = true,
            message = token
        )
    }
}