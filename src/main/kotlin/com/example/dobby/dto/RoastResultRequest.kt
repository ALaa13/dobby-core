package com.example.dobby.dto

import kotlinx.serialization.Serializable

@Serializable
data class RoastResultRequest(
    val channelId: String,
    val content: String,
    val success: Boolean
)
