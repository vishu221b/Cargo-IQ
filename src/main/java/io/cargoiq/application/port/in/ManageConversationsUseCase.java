package io.cargoiq.application.port.in;

import io.cargoiq.domain.model.ChatMessage;
import io.cargoiq.domain.model.Conversation;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port: manage a user's persisted chat conversations.
 *
 * <p>Every operation is scoped to a {@code userId} (taken from the JWT by the
 * controller) so a user can only ever see or mutate their own threads.
 *
 * @author Vishal Dogra
 */
public interface ManageConversationsUseCase {

    /** Create a new, empty conversation for the user. */
    Conversation create(UUID userId);

    /** The user's conversations, most-recently-updated first. */
    List<Conversation> list(UUID userId);

    /** A conversation (owned by the user) together with its messages, oldest-first. */
    ConversationDetail get(UUID conversationId, UUID userId);

    /** Delete a conversation (and its messages) owned by the user. */
    void delete(UUID conversationId, UUID userId);

    /**
     * Ensure a conversation with this id exists and is owned by the user — used
     * by the query flow, which threads memory by a client-supplied id. Creates
     * it (titled from the first message) when absent; rejects it when it exists
     * but belongs to someone else.
     */
    void ensureOwned(UUID conversationId, UUID userId, String firstMessageTitle);

    record ConversationDetail(Conversation conversation, List<ChatMessage> messages) {}
}
