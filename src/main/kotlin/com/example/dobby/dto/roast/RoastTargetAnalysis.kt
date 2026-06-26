package com.example.dobby.dto.roast

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

@Serializable
data class RoastTargetAnalysis(
    @JsonProperty("discordUserId")
    val discordUserId: String,
    @JsonProperty("reason")
    val reason: String
)
