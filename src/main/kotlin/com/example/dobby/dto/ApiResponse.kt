package com.example.dobby.dto

import java.time.Instant

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val timestamp: Long = Instant.now().toEpochMilli()
)