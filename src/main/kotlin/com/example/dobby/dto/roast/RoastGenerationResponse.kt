package com.example.dobby.dto.roast

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

@Serializable
data class RoastGenerationResponse(
    @JsonProperty("roastText")
    val roastText: String,
    @JsonProperty("primaryTargetId")
    val primaryTargetId: String,
    @JsonProperty("analytics")
    val analytics: RoastAnalytics,
)
