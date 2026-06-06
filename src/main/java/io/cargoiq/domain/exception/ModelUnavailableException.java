package io.cargoiq.domain.exception;

/**
 * Raised when the chat model a request asked for can't answer — the provider
 * isn't configured, or the local model server/model isn't reachable. Surfaced
 * to the client as 503 so the UI can suggest switching providers.
 */
public class ModelUnavailableException extends DomainException {
    public ModelUnavailableException(String provider, String reason) {
        super("Model provider '" + provider + "' is unavailable: " + reason);
    }
}
