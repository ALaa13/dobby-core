package com.example.dobby.controller

import com.example.dobby.dto.ApiResponse
import com.example.dobby.dto.RoastRequest
import com.example.dobby.service.RoastService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RequestMapping("/roast")
@RestController
class RoastController(
    private val roastService: RoastService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun roast(@Valid @RequestBody request: RoastRequest): ApiResponse {
        roastService.processRoastAsync(request)
        return ApiResponse(
            success = true,
            message = "Roast generation job accepted and queued successfully."
        )
    }
}