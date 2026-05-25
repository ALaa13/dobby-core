package com.example.dobby.controller

import com.example.dobby.dto.DiscordFactRequest
import com.example.dobby.dto.RoastRequest
import com.example.dobby.service.GeminiService
import com.example.dobby.service.MemoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1")
@RestController
class RoastController(
    private val geminiService: GeminiService,
    private val memoryService: MemoryService
) {

    @GetMapping("/")
    suspend fun hello(): String {
        return "Hello, World!"
    }

    @PostMapping("/roast")
    @ResponseStatus(HttpStatus.ACCEPTED)
    suspend fun roast(@Valid @RequestBody request: RoastRequest) {
        geminiService.processRoastAsync(request)
    }

    @PostMapping("/remember")
    suspend fun remember(@Valid @RequestBody request: DiscordFactRequest) {
        return memoryService.saveFact(request)
    }
}