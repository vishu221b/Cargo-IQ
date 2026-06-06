package io.cargoiq.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * A user question, plus optional retrieval filters.
 *
 * <p>{@code topK} is the number of chunks to retrieve before LLM synthesis.
 * Defaults to 6 — high enough for context, low enough to keep prompt cost
 * sane. Tune per use case.
 */
public record Query(
        String text,
        int topK,
        Optional<DocumentType> filterByType,
        Optional<Incoterm> filterByIncoterm) {

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

    public static Query of(String text) {
        return new Query(text, 6, Optional.empty(), Optional.empty());
    }
}
