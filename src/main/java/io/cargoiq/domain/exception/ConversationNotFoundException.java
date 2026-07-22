package io.cargoiq.domain.exception;

import java.util.UUID;

/**
 * Thrown when a conversation doesn't exist, or exists but isn't owned by the
 * requesting user. Deliberately the same "not found" for both cases so we don't
 * leak the existence of other users' conversations.
 */
public class ConversationNotFoundException extends DomainException {
    public ConversationNotFoundException(UUID id) {
        super("Conversation not found: " + id);
    }
}
