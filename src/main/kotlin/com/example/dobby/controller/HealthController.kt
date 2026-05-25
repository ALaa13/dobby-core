package com.example.dobby.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    @GetMapping("/")
    suspend fun hello(): String {
        return "Hello, World!"
    }
}