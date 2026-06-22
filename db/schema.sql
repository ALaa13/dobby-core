-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.

CREATE TABLE public.user_profiles
(
    id              uuid                     NOT NULL DEFAULT gen_random_uuid(),
    discord_user_id text                     NOT NULL,
    guild_id        text                     NOT NULL,
    display_name    text,
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    updated_at      timestamp with time zone,
    CONSTRAINT user_profiles_pkey PRIMARY KEY (id)
);
CREATE TABLE public.user_facts
(
    id                 uuid                     NOT NULL DEFAULT gen_random_uuid(),
    profile_id         uuid                     NOT NULL,
    fact_text          text                     NOT NULL,
    source             text,
    confidence_score   smallint,
    roastability_score smallint,
    created_at         timestamp with time zone NOT NULL DEFAULT now(),
    updated_at         timestamp with time zone,
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