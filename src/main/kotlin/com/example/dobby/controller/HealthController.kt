package com.example.dobby.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/")
@RestController
class HealthController {
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    suspend fun hello(): String {
        return "Hello, World!"
    }
}