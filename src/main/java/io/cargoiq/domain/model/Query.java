package io.cargoiq.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * A user question, plus optional retrieval filters and a chat-model choice.
 *
 * <p>{@code topK} is the number of chunks to retrieve before synthesis. Defaults
 * to 6 — high enough for context, low enough to keep prompt cost sane.
 *
 * <p>{@code chat} selects which model answers the query (per request). It is
 * nullable; the four-arg constructor and {@link #of} leave it unset, which the
 * answering service treats as {@link ModelChoice#mock()}.
 */
public record Query(
        String text,
        int topK,
        Optional<DocumentType> filterByType,
        Optional<Incoterm> filterByIncoterm,
        ModelChoice chat) {

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
    }

    /** Backward-compatible constructor for callers that don't pick a model. */
    public Query(String text, int topK,
                 Optional<DocumentType> filterByType,
                 Optional<Incoterm> filterByIncoterm) {
        this(text, topK, filterByType, filterByIncoterm, null);
    }

    public static Query of(String text) {
        return new Query(text, 6, Optional.empty(), Optional.empty(), null);
    }

    /** The model choice, defaulting to the dependency-free mock when unset. */
    public ModelChoice chatOrMock() {
        return chat != null ? chat : ModelChoice.mock();
    }
}
