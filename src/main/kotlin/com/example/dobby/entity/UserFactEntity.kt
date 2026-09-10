package com.example.dobby.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("user_facts")
data class UserFactEntity(
    @Id
    val id: UUID? = null,

    @Column("profile_id")
    val profileId: UUID,

    @Column("fact_text")
    val factText: String,

    @Column("source")
    val source: String?,

    @Column("created_at")
    val createdAt: OffsetDateTime? = null,

    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,
)
