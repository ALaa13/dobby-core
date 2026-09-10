CREATE TABLE user_profiles
(
    id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    discord_user_id TEXT        NOT NULL,
    guild_id        TEXT        NOT NULL,
    display_name    TEXT,
    avatar_hash     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ,

    CONSTRAINT user_profiles_discord_user_guild_unique
        UNIQUE (discord_user_id, guild_id)
);

CREATE TABLE user_facts
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    profile_id UUID        NOT NULL,
    fact_text  TEXT        NOT NULL,
    source     TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,

    CONSTRAINT user_facts_profile_fk
        FOREIGN KEY (profile_id)
            REFERENCES user_profiles (id)
            ON DELETE CASCADE
);

CREATE TABLE discord_accounts
(
    discord_user_id TEXT PRIMARY KEY,
    encrypted_token TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE roasts
(
    id                  UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    guild_id            TEXT        NOT NULL,
    channel_id          TEXT        NOT NULL,
    roast_text          TEXT        NOT NULL,
    persona_used        TEXT,
    primary_target_id   TEXT        NOT NULL,
    clapped_the_most_id TEXT        NOT NULL,
    burn_accuracy       SMALLINT    NOT NULL,
    severity_score      SMALLINT    NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT roasts_burn_accuracy_check
        CHECK (burn_accuracy BETWEEN 0 AND 100),

    CONSTRAINT roasts_severity_score_check
        CHECK (severity_score BETWEEN 0 AND 100)
);

CREATE TABLE roast_targets
(
    id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    roast_id        UUID        NOT NULL,
    discord_user_id TEXT        NOT NULL,
    guild_id        TEXT        NOT NULL,
    damage_reason   TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT roast_targets_roast_fk
        FOREIGN KEY (roast_id)
            REFERENCES roasts (id)
            ON DELETE CASCADE,

    CONSTRAINT roast_targets_profile_fk
        FOREIGN KEY (discord_user_id, guild_id)
            REFERENCES user_profiles (discord_user_id, guild_id)
            ON DELETE CASCADE
);

CREATE INDEX idx_user_profiles_guild_id
    ON user_profiles (guild_id);

CREATE INDEX idx_user_facts_profile_id
    ON user_facts (profile_id);

CREATE INDEX idx_roasts_guild_created_at
    ON roasts (guild_id, created_at DESC);

CREATE INDEX idx_roast_targets_roast_id
    ON roast_targets (roast_id);
