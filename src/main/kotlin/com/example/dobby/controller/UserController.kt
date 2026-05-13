package com.example.dobby.controller

import com.example.dobby.model.PersonCreateRequest
import com.example.dobby.model.PersonResponse
import com.example.dobby.service.UserService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userService: UserService
) {

    @GetMapping("/")
    suspend fun hello(): List<PersonResponse> {
        val users = userService.getUsers()
        return users
    }

    @PostMapping("/user")
    suspend fun insertUser(@Valid @RequestBody person: PersonCreateRequest): PersonResponse {
        return userService.insertUser(person)
    }
}