package io.cargoiq.domain.model;

import java.util.UUID;

/**
 * A pointer back into the corpus for a single piece of evidence used in an
 * {@link Answer}. The RAG contract: <i>every claim must be traceable</i>.
 *
 * <p>Score is the similarity / relevance the retriever reported (cosine in
 * pgvector's default config). Don't show it to end users — keep it for
 * debugging and eval. Snippet is short enough to inline in a response.
 */
public record Citation(
        UUID documentId,
        UUID chunkId,
        String documentTitle,
        int chunkSequence,
        String snippet,
        double score) {}
