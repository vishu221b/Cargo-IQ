-- ============================================================================
-- V3 — persistent chat conversations, scoped per user.
--
-- `conversations`  — one row per chat thread, owned by a user.
-- `chat_messages`  — the turns of a conversation (USER / ASSISTANT), in order.
--                    ON DELETE CASCADE so deleting a conversation removes its
--                    messages in a single statement.
-- ============================================================================

CREATE TABLE IF NOT EXISTS conversations (
    id          uuid         PRIMARY KEY,
    user_id     uuid         NOT NULL,
    title       varchar(200),
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_conversations_user
    ON conversations(user_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS chat_messages (
    id               uuid         PRIMARY KEY,
    conversation_id  uuid         NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role             varchar(16)  NOT NULL,
    content          text         NOT NULL,
    created_at       timestamptz  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_conversation
    ON chat_messages(conversation_id, created_at);
