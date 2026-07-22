-- ============================================================================
-- V4 — per-user LLM API keys.
--
-- One row per (user, provider). The key is stored ENCRYPTED (AES-GCM, see
-- SpringSecuritySecretCipher) — never in plaintext. It is used only server-side
-- to build a chat model on the user's behalf and is never returned by the API.
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_api_keys (
    user_id        uuid         NOT NULL,
    provider       varchar(32)  NOT NULL,
    encrypted_key  text         NOT NULL,
    updated_at     timestamptz  NOT NULL,
    PRIMARY KEY (user_id, provider)
);
