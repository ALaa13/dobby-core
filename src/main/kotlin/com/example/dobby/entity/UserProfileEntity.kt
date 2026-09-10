package com.example.dobby.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("user_profiles")
data class UserProfileEntity(
    @Id
    val id: UUID? = null,
    @Column("discord_user_id")
    val discordUserId: String,
    @Column("guild_id")
    val guildId: String,
    @Column("display_name")
    val displayName: String? = null,
    @Column("avatar_hash")
    val avatarHash: String? = null,
    @Column("created_at")
    val createdAt: OffsetDateTime? = null,
    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,
)
