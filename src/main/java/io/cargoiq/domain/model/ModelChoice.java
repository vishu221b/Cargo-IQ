package io.cargoiq.domain.model;

/**
 * Which chat model should answer a query — chosen per request so a caller can
 * pick a provider/model from the UI without restarting the server.
 *
 * <p>{@code provider} is a lower-case id ({@code mock}, {@code ollama},
 * {@code openai}, {@code anthropic}, {@code google-genai}); {@code model} is the
 * provider-specific model name (e.g. {@code gemma2:9b} for Ollama). Both are
 * optional — a null/blank provider means "use the server default", which is the
 * dependency-free {@code mock} unless the deployment configured something else.
 *
 * @author Vishal Dogra
 */
public record ModelChoice(String provider, String model) {

    public static final String MOCK = "mock";

    /** The default, dependency-free choice — no API key, no local model server. */
    public static ModelChoice mock() {
        return new ModelChoice(MOCK, MOCK);
    }

    public boolean hasProvider() {
        return provider != null && !provider.isBlank();
    }

    /** Provider id, lower-cased and trimmed; empty string when unset. */
    public String providerId() {
        return hasProvider() ? provider.trim().toLowerCase() : "";
    }

    public boolean hasModel() {
        return model != null && !model.isBlank();
    }
}
