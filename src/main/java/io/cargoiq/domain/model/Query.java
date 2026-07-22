package io.cargoiq.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * A user question, plus optional retrieval filters, a chat-model choice, the
 * retrieval-pipeline switches, and an optional conversation id for memory.
 *
 * <p>{@code topK} is the number of chunks to retrieve before synthesis. Defaults
 * to 6 — high enough for context, low enough to keep prompt cost sane.
 *
 * <p>{@code chat} selects which model answers the query (per request). It is
 * nullable; the shorter constructors and {@link #of} leave it unset, which the
 * answering service treats as {@link ModelChoice#mock()}.
 *
 * <p>{@code retrieval} chooses which retrieval stages run (hybrid / multi-query
 * / rerank); {@code conversationId} threads multi-turn memory. Both default to
 * "on / stateless" so every legacy caller keeps working.
 */
public record Query(
        String text,
        int topK,
        Optional<DocumentType> filterByType,
        Optional<Incoterm> filterByIncoterm,
        ModelChoice chat,
        RetrievalOptions retrieval,
        String conversationId) {

    public Query {
        Objects.requireNonNull(text, "text");
        if (text.isBlank()) {
            throw new IllegalArgumentException("query text must not be blank");
        }
        if (topK <= 0 || topK > 50) {
            throw new IllegalArgumentException("topK must be between 1 and 50");
        }
        filterByType = filterByType == null ? Optional.empty() : filterByType;
        filterByIncoterm = filterByIncoterm == null ? Optional.empty() : filterByIncoterm;
        retrieval = retrieval == null ? RetrievalOptions.defaults() : retrieval;
    }

    /** Full model-choice constructor without retrieval tuning or memory. */
    public Query(String text, int topK,
                 Optional<DocumentType> filterByType,
                 Optional<Incoterm> filterByIncoterm,
                 ModelChoice chat) {
        this(text, topK, filterByType, filterByIncoterm, chat, RetrievalOptions.defaults(), null);
    }

    /** Backward-compatible constructor for callers that don't pick a model. */
    public Query(String text, int topK,
                 Optional<DocumentType> filterByType,
                 Optional<Incoterm> filterByIncoterm) {
        this(text, topK, filterByType, filterByIncoterm, null, RetrievalOptions.defaults(), null);
    }

    public static Query of(String text) {
        return new Query(text, 6, Optional.empty(), Optional.empty(), null, RetrievalOptions.defaults(), null);
    }

    /** The model choice, defaulting to the dependency-free mock when unset. */
    public ModelChoice chatOrMock() {
        return chat != null ? chat : ModelChoice.mock();
    }

    /** True when this query is part of a threaded, multi-turn conversation. */
    public boolean hasConversation() {
        return conversationId != null && !conversationId.isBlank();
    }

    /** A copy of this query with different text (used for multi-query expansion). */
    public Query withText(String newText) {
        return new Query(newText, topK, filterByType, filterByIncoterm, chat, retrieval, conversationId);
    }
}
