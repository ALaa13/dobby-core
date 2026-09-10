package com.example.dobby.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("discord_accounts")
data class DiscordAccountEntity(
    @Id
    @Column("discord_user_id")
    val discordUserId: String,
    @Column("encrypted_token")
    val encryptedToken: String?,
    @Column("created_at")
    val createdAt: OffsetDateTime,
)
