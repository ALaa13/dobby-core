package com.example.dobby.controller

import com.example.dobby.model.RoastRequest
import com.example.dobby.service.GeminiService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/roasts")
@RestController
class UserController(
    private val geminiService: GeminiService

) {

    @GetMapping("/")
    suspend fun hello(): String {
        return "Hello, World!"
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    suspend fun dobby(@RequestBody request: RoastRequest) {
        geminiService.processRoastAsync(request)
    }
}