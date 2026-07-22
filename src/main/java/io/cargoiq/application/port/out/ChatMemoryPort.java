package io.cargoiq.application.port.out;

import io.cargoiq.domain.model.ConversationTurn;

import java.util.List;

/**
 * Outbound port: short-term conversational memory keyed by conversation id.
 *
 * <p>RAG becomes a conversation when the model can see prior turns. This port
 * stores the running transcript so {@link io.cargoiq.application.service.AnswerQueryService}
 * can prepend recent context to a follow-up question and record the answer.
 *
 * <p>The default adapter keeps a bounded, in-memory window (no external store,
 * no key); a JPA/Redis-backed adapter can replace it behind the same contract.
 *
 * @author Vishal Dogra
 */
public interface ChatMemoryPort {

    /** The most recent turns for a conversation, oldest-first, capped at {@code maxTurns}. */
    List<ConversationTurn> history(String conversationId, int maxTurns);

    /** Append a turn to a conversation's transcript. */
    void append(String conversationId, ConversationTurn turn);

    /** A no-op memory — history is always empty, appends are dropped. */
    static ChatMemoryPort noop() {
        return new ChatMemoryPort() {
            @Override public List<ConversationTurn> history(String conversationId, int maxTurns) { return List.of(); }
            @Override public void append(String conversationId, ConversationTurn turn) { }
        };
    }
}
