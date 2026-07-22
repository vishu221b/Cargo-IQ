package io.cargoiq.adapter.out.memory;

import io.cargoiq.application.port.out.ChatMemoryPort;
import io.cargoiq.application.port.out.ConversationRepository;
import io.cargoiq.domain.model.ChatMessage;
import io.cargoiq.domain.model.ConversationTurn;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Database-backed {@link ChatMemoryPort}: conversational memory is the persisted
 * conversation itself, so multi-turn context survives restarts and is shared
 * with the chat-history UI.
 *
 * <p>The answering service reads recent turns to build the prompt and appends
 * each turn as it goes — both keyed by {@code conversationId}. The conversation
 * row (with its owning user) is created by the web layer before the query runs;
 * appends to a missing conversation are dropped defensively.
 *
 * @author Vishal Dogra
 */
@Component
public class PersistentChatMemory implements ChatMemoryPort {

    private final ConversationRepository conversations;

    public PersistentChatMemory(ConversationRepository conversations) {
        this.conversations = conversations;
    }

    @Override
    public List<ConversationTurn> history(String conversationId, int maxTurns) {
        if (conversationId == null || conversationId.isBlank() || maxTurns <= 0) {
            return List.of();
        }
        java.util.UUID id = parse(conversationId);
        if (id == null) return List.of();

        List<ChatMessage> all = conversations.messages(id);
        int skip = Math.max(0, all.size() - maxTurns);
        return all.stream()
                .skip(skip)
                .map(m -> new ConversationTurn(m.role(), m.content()))
                .toList();
    }

    @Override
    public void append(String conversationId, ConversationTurn turn) {
        if (conversationId == null || conversationId.isBlank() || turn == null) return;
        java.util.UUID id = parse(conversationId);
        if (id == null) return;
        conversations.appendMessage(id, turn.role(), turn.text());
    }

    private static java.util.UUID parse(String id) {
        try { return java.util.UUID.fromString(id); }
        catch (IllegalArgumentException e) { return null; }
    }
}
