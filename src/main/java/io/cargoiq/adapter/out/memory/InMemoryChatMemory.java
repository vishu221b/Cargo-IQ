package io.cargoiq.adapter.out.memory;

import io.cargoiq.application.port.out.ChatMemoryPort;
import io.cargoiq.domain.model.ConversationTurn;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounded, in-process conversational memory.
 *
 * <p>Keeps a rolling window of the most recent turns per conversation id — no
 * external store. Superseded in the running app by {@link PersistentChatMemory}
 * (DB-backed, so history survives restarts and feeds the chat-history UI); kept
 * as a dependency-free implementation used by unit tests and as a fallback.
 *
 * @author Vishal Dogra
 */
public class InMemoryChatMemory implements ChatMemoryPort {

    /** Hard cap on retained turns per conversation to bound memory use. */
    private static final int MAX_RETAINED = 24;

    private final Map<String, Deque<ConversationTurn>> store = new ConcurrentHashMap<>();

    @Override
    public List<ConversationTurn> history(String conversationId, int maxTurns) {
        if (conversationId == null || conversationId.isBlank() || maxTurns <= 0) {
            return List.of();
        }
        Deque<ConversationTurn> turns = store.get(conversationId);
        if (turns == null) return List.of();
        synchronized (turns) {
            int size = turns.size();
            int skip = Math.max(0, size - maxTurns);
            return turns.stream().skip(skip).toList();
        }
    }

    @Override
    public void append(String conversationId, ConversationTurn turn) {
        if (conversationId == null || conversationId.isBlank() || turn == null) {
            return;
        }
        Deque<ConversationTurn> turns = store.computeIfAbsent(conversationId, k -> new ArrayDeque<>());
        synchronized (turns) {
            turns.addLast(turn);
            while (turns.size() > MAX_RETAINED) {
                turns.removeFirst();
            }
        }
    }
}
