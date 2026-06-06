package io.cargoiq.domain.exception;

public class HsCodeNotFoundException extends DomainException {
    public HsCodeNotFoundException(String query) {
        super("No HS code matched query: " + query);
    }
}
