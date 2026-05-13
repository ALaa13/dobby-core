package com.example.dobby.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class PersonResponse @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    @SerialName("created_at")
    val createdAt: Instant,
    val name: String,
    val age: Int
)
