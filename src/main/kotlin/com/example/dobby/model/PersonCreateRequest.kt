package com.example.dobby.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import kotlinx.serialization.Serializable


@Serializable
data class PersonCreateRequest(
    @field:NotBlank(message = "Name must not be blank")
    val name: String,
    @field:NotNull(message = "Age is required")
    @field:Positive(message = "Age must be greater than 0")
    val age: Int
)

