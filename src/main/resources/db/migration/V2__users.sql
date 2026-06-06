-- ============================================================================
-- V2 — authentication & authorization schema.
--
-- Two tables back the RBAC model:
--   1. `users`       — one row per account, holding the BCrypt password hash.
--   2. `user_roles`  — the user's granted roles (USER, ADMIN). A side table
--                      rather than a delimited column so each role is its own
--                      indexable row and the set has a natural unique key.
-- ============================================================================

CREATE TABLE IF NOT EXISTS users (
    id             uuid          PRIMARY KEY,
    username       varchar(64)   NOT NULL UNIQUE,
    password_hash  varchar(100)  NOT NULL,   -- BCrypt output is 60 chars; headroom for algorithm changes
    created_at     timestamptz   NOT NULL
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id  uuid         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role     varchar(16)  NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
