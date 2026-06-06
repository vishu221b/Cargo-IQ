package io.cargoiq.domain.model;

import java.util.UUID;

/**
 * A chunk of text from a parent {@link Document}, sized for embedding.
 *
 * <p>Sequence index lets us reconstruct order and provide stable per-chunk
 * citations ("section 3, chunk 12"). The vector embedding itself is held by
 * the vector store, not on the domain object — the domain only cares that a
 * chunk is identifiable and contains text.
 */
public record DocumentChunk(
        UUID id,
        UUID documentId,
        int sequence,
        String text) {

    public DocumentChunk {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("chunk text must not be blank");
        }
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must be >= 0");
        }
    }

    public static DocumentChunk of(UUID documentId, int sequence, String text) {
        return new DocumentChunk(UUID.randomUUID(), documentId, sequence, text);
    }
}
