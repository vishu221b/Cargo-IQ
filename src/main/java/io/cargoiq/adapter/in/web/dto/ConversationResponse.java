package io.cargoiq.adapter.in.web.dto;

import io.cargoiq.domain.model.Conversation;

import java.time.Instant;
import java.util.UUID;

/** Summary of a chat conversation, for the history list. */
public record ConversationResponse(
        UUID id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        long messageCount) {

    public static ConversationResponse from(Conversation c) {
        return new ConversationResponse(c.id(), c.title(), c.createdAt(), c.updatedAt(), c.messageCount());
    }
}
