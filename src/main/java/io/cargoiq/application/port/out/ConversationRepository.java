package io.cargoiq.application.port.out;

import io.cargoiq.domain.model.ChatMessage;
import io.cargoiq.domain.model.Conversation;
import io.cargoiq.domain.model.ConversationTurn;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port: persistence for chat {@link Conversation}s and their messages.
 *
 * @author Vishal Dogra
 */
public interface ConversationRepository {

    /** Insert a new (empty) conversation. */
    Conversation create(UUID id, UUID userId, String title);

    Optional<Conversation> findById(UUID id);

    /** A user's conversations, most recently updated first, with message counts. */
    List<Conversation> listByUser(UUID userId);

    List<ChatMessage> messages(UUID conversationId);

    /** Append a message and bump the conversation's {@code updated_at}. No-op if the conversation is gone. */
    void appendMessage(UUID conversationId, ConversationTurn.Role role, String content);

    /** Set the title only if it is currently null/blank (first message names the thread). */
    void setTitleIfAbsent(UUID conversationId, String title);

    void deleteById(UUID id);
}
