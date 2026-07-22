package io.cargoiq.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A persisted chat thread, owned by a user.
 *
 * <p>{@code messageCount} is a read-side convenience for the conversation list;
 * it isn't a stored column (it's derived from the messages), so it's 0 on a
 * freshly created conversation.
 *
 * @author Vishal Dogra
 */
public record Conversation(
        UUID id,
        UUID userId,
        String title,
        Instant createdAt,
        Instant updatedAt,
        long messageCount) {

    public boolean isOwnedBy(UUID candidate) {
        return userId.equals(candidate);
    }
}
