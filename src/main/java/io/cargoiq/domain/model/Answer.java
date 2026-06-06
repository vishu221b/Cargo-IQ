package io.cargoiq.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * The output of a RAG query: the synthesised text plus the citations that
 * justify it.
 *
 * <p>An Answer with empty citations is suspicious and should be surfaced as
 * such to the caller — it means the LLM responded without grounding.
 */
public record Answer(String text, List<Citation> citations) {

    public Answer {
        Objects.requireNonNull(text, "text");
        citations = citations == null ? List.of() : List.copyOf(citations);
    }

    public boolean isGrounded() { return !citations.isEmpty(); }
}
