package com.example.dobby.repository.r2dbc.query

const val ROAST_WITH_TARGETS_SELECT = """
    SELECT
        r.id AS "id",
        r.guild_id AS "guildId",
        r.channel_id AS "channelId",
        r.roast_text AS "roastText",
        r.persona_used AS "personaUsed",
        r.primary_target_id AS "primaryTargetId",
        r.clapped_the_most_id AS "clappedTheMostId",
        r.burn_accuracy::integer AS "burnAccuracy",
        r.severity_score::integer AS "severityScore",
        r.created_at AS "createdAt",
        COALESCE(
            jsonb_agg(
                jsonb_build_object(
                    'roast_id', rt.roast_id,
                    'discord_user_id', rt.discord_user_id,
                    'guild_id', rt.guild_id,
                    'damage_reason', rt.damage_reason,
                    'user_profiles',
                        CASE
                            WHEN p.id IS NULL THEN NULL
                            ELSE jsonb_build_object(
                                'display_name', p.display_name,
                                'avatar_hash', p.avatar_hash
                            )
                        END
                ) ORDER BY rt.created_at ASC, rt.id ASC
            ) FILTER (WHERE rt.id IS NOT NULL),
            '[]'::jsonb
        )::text AS "targetsJson"
    FROM roasts r
    LEFT JOIN roast_targets rt
        ON rt.roast_id = r.id
    LEFT JOIN user_profiles p
        ON p.discord_user_id = rt.discord_user_id
       AND p.guild_id = rt.guild_id
"""
