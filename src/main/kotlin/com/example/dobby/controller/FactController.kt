package com.example.dobby.controller

import com.example.dobby.dto.DiscordFactRequest
import com.example.dobby.service.FactService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1")
@RestController
class FactController(
    private val factService: FactService
) {
    @PostMapping("/fact")
    suspend fun remember(@Valid @RequestBody request: DiscordFactRequest) {
        return factService.saveFact(request)
    }
}