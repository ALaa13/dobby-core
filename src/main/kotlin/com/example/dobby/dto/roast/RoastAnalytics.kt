package com.example.dobby.dto.roast

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

@Serializable
data class RoastAnalytics(
    @JsonProperty("clappedTheMostId")
    val clappedTheMostId: String,
    @JsonProperty("burnAccuracy")
    val burnAccuracy: Int,
    @JsonProperty("severityScore")
    val severityScore: Int,
    @JsonProperty("allTargets")
    val allTargets: List<RoastTargetAnalysis>

)
