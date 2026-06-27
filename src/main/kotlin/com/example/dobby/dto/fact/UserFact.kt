package com.example.dobby.dto.fact

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class FactSource {
    USER_SUBMISSION,
}

@Serializable
data class UserFactCreateRequest(
    @SerialName("profile_id")
    val profileId: String,
    @SerialName("fact_text")
    val factText: String,
    @SerialName("source")
    val source: FactSource,
)

@Serializable
data class UserFactResponse(
    @SerialName("id")
    val id: String,
    @SerialName("profile_id")
    val profileId: String,
    @SerialName("fact_text")
    val factText: String,
    @SerialName("source")
    val source: String?,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String?,
)
