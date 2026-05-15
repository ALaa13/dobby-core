package com.example.dobby.controller

import com.example.dobby.model.ChatDiscordMessage
import com.example.dobby.model.PersonResponse
import com.example.dobby.service.GeminiService
import com.example.dobby.service.UserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userService: UserService,
    private val geminiService: GeminiService

) {

    @GetMapping("/")
    suspend fun hello(): List<PersonResponse> {
        val users = userService.getUsers()
        return users
    }

    @PostMapping("/dobby")
    suspend fun dobby(@RequestBody messages: List<ChatDiscordMessage>): String {
        return geminiService.generateSummary(messages, null)
    }
}