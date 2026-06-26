-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.

CREATE TABLE public.user_profiles
(
    id              uuid                     NOT NULL DEFAULT gen_random_uuid(),
    discord_user_id text                     NOT NULL,
    guild_id        text                     NOT NULL,
    display_name    text,
    avatar_hash     text,
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    updated_at      timestamp with time zone,
    CONSTRAINT user_profiles_pkey PRIMARY KEY (id)
);
CREATE TABLE public.user_facts
(
    id         uuid                     NOT NULL DEFAULT gen_random_uuid(),
    profile_id uuid                     NOT NULL,
    fact_text  text                     NOT NULL,
    source     text,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone,
    CONSTRAINT user_facts_pkey PRIMARY KEY (id),
    CONSTRAINT user_facts_profile_id_fkey FOREIGN KEY (profile_id) REFERENCES public.user_profiles (id)
);
CREATE TABLE public.discord_accounts
(
    discord_user_id text                     NOT NULL,
    encrypted_token text,
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT discord_accounts_pkey PRIMARY KEY (discord_user_id)
);
CREATE TABLE public.roasts
(
    id                  uuid                     NOT NULL DEFAULT gen_random_uuid(),
    guild_id            character varying        NOT NULL,
    channel_id          character varying        NOT NULL,
    roast_text          text                     NOT NULL,
    persona_used        character varying,
    primary_target_id   character varying        NOT NULL,
    clapped_the_most_id character varying        NOT NULL,
    burn_accuracy       smallint                 NOT NULL CHECK (burn_accuracy >= 0 AND burn_accuracy <= 100),
    severity_score      smallint                 NOT NULL CHECK (severity_score >= 0 AND severity_score <= 100),
    created_at          timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT roasts_pkey PRIMARY KEY (id)
);
CREATE TABLE public.roast_targets
(
    id              uuid                     NOT NULL DEFAULT gen_random_uuid(),
    roast_id        uuid                     NOT NULL,
    discord_user_id character varying        NOT NULL,
    guild_id        character varying        NOT NULL,
    damage_reason   text                     NOT NULL,
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT roast_targets_pkey PRIMARY KEY (id),
    CONSTRAINT roast_targets_roast_id_fkey FOREIGN KEY (roast_id) REFERENCES public.roasts (id),
    CONSTRAINT roast_targets_profile_fkey FOREIGN KEY (discord_user_id) REFERENCES public.user_profiles (discord_user_id),
    CONSTRAINT roast_targets_profile_fkey FOREIGN KEY (guild_id) REFERENCES public.user_profiles (guild_id)
);