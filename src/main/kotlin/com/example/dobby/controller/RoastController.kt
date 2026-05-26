package com.example.dobby.controller

import com.example.dobby.dto.RoastRequest
import com.example.dobby.service.RoastService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/roast")
@RestController
class RoastController(
    private val roastService: RoastService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun roast(@Valid @RequestBody request: RoastRequest) {
        roastService.processRoastAsync(request)
    }
}