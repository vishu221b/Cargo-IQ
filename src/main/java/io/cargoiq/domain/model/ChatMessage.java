package io.cargoiq.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One stored turn in a {@link Conversation}. Reuses {@link ConversationTurn.Role}
 * (USER / ASSISTANT) so the in-prompt memory and the persisted history speak the
 * same vocabulary.
 *
 * @author Vishal Dogra
 */
public record ChatMessage(
        UUID id,
        UUID conversationId,
        ConversationTurn.Role role,
        String content,
        Instant createdAt) {}
