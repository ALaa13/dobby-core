package com.example.dobby.controller

import com.example.dobby.dto.ApiResponse
import com.example.dobby.service.DevService
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Profile("dev")
@RestController
@RequestMapping("/dev")
class DevController(
    private val devService: DevService,
) {
    @GetMapping("/token")
    fun getTestToken(
        @RequestParam secret: String,
    ): ApiResponse {
        val token = devService.getTestToken(secret)
        return ApiResponse(
            success = true,
            message = token,
        )
    }
}
