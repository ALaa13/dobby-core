package com.example.dobby.dto.roast


data class RoastResult(
    val text: String,
    val persona: String?,
    val primaryTargetId: String,
    val clappedTheMostId: String,
    val burnAccuracy: Int,
    val severityScore: Int,
    val targets: List<TargetDamage>
)

data class TargetDamage(
    val userId: String,
    val reason: String
)