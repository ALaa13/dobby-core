package com.example.dobby.entity

import java.time.OffsetDateTime
import java.util.UUID

data class UserFactEntity(
    val id: UUID,
    val profileId: UUID,
    val factText: String,
    val source: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime? = null,
)
