package com.example.dobby.controller.discord

import com.example.dobby.dto.ApiResponse
import com.example.dobby.dto.roast.DiscordRoastRequest
import com.example.dobby.service.RoastService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/roast")
@RestController
class RoastController(
    private val roastService: RoastService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun roast(
        @Valid @RequestBody request: DiscordRoastRequest,
    ): ApiResponse {
        roastService.processRoastAsync(request)
        return ApiResponse(
            success = true,
            message = "Roast generation job accepted and queued successfully.",
        )
    }
}
