package io.cargoiq.adapter.in.web.dto;

import io.cargoiq.domain.model.DocumentType;
import io.cargoiq.domain.model.Incoterm;
import io.cargoiq.domain.model.ModelChoice;
import io.cargoiq.domain.model.RetrievalOptions;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A RAG query.
 *
 * <p>{@code provider} and {@code model} let the caller choose the chat model per
 * request. {@code hybrid} / {@code multiQuery} / {@code rerank} toggle the
 * retrieval-pipeline stages (all default on). {@code conversationId} threads
 * multi-turn memory — reuse the same id across follow-up questions.
 */
public record QueryRequest(
        @NotBlank @Size(max = 2000) String query,
        @Min(1) @Max(50) Integer topK,
        DocumentType filterByType,
        Incoterm filterByIncoterm,
        @Size(max = 32) String provider,
        @Size(max = 64) String model,
        Boolean hybrid,
        Boolean multiQuery,
        Boolean rerank,
        @Size(max = 64) String conversationId) {

    public int topKOrDefault() { return topK != null ? topK : 6; }

    /** The selected model, or null when the caller didn't specify one. */
    public ModelChoice modelChoice() {
        if ((provider == null || provider.isBlank()) && (model == null || model.isBlank())) {
            return null;
        }
        return new ModelChoice(provider, model);
    }

    /** Retrieval switches; each stage defaults to on when the field is omitted. */
    public RetrievalOptions retrievalOptions() {
        return new RetrievalOptions(
                hybrid == null || hybrid,
                multiQuery == null || multiQuery,
                rerank == null || rerank);
    }
}
