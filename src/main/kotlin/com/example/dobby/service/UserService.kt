package com.example.dobby.service

import com.example.dobby.model.PersonResponse
import com.example.dobby.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository
) {

    suspend fun getUsers(): List<PersonResponse> {
        return userRepository.getUsers()
    }
}