package io.cargoiq.adapter.in.web.dto;

import io.cargoiq.domain.model.DocumentType;
import io.cargoiq.domain.model.Incoterm;
import io.cargoiq.domain.model.ModelChoice;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A RAG query. {@code provider} and {@code model} let the caller choose the chat
 * model per request — e.g. {@code "ollama"} + {@code "gemma2:9b"}, or the default
 * {@code "mock"} which needs no API key or model server.
 */
public record QueryRequest(
        @NotBlank @Size(max = 2000) String query,
        @Min(1) @Max(50) Integer topK,
        DocumentType filterByType,
        Incoterm filterByIncoterm,
        @Size(max = 32) String provider,
        @Size(max = 64) String model) {

    public int topKOrDefault() { return topK != null ? topK : 6; }

    /** The selected model, or null when the caller didn't specify one. */
    public ModelChoice modelChoice() {
        if ((provider == null || provider.isBlank()) && (model == null || model.isBlank())) {
            return null;
        }
        return new ModelChoice(provider, model);
    }
}
