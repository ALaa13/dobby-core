package com.example.dobby.repository.r2dbc.query

const val USER_PROFILE_WITH_FACTS_SELECT = """
    SELECT
        p.id AS "id",
        p.discord_user_id AS "discordUserId",
        p.guild_id AS "guildId",
        p.display_name AS "displayName",
        p.avatar_hash AS "avatarHash",
        p.created_at AS "createdAt",
        p.updated_at AS "updatedAt",
        COALESCE(
            jsonb_agg(
                jsonb_build_object(
                    'id', f.id,
                    'profile_id', f.profile_id,
                    'fact_text', f.fact_text,
                    'source', f.source,
                    'created_at', f.created_at,
                    'updated_at', f.updated_at
                ) ORDER BY f.created_at ASC, f.id ASC
            ) FILTER (WHERE f.id IS NOT NULL),
            '[]'::jsonb
        )::text AS "factsJson"
    FROM user_profiles p
    LEFT JOIN user_facts f
        ON f.profile_id = p.id
"""
