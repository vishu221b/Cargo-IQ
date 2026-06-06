package io.cargoiq.domain.exception;

/**
 * Root of the domain's exception hierarchy.
 *
 * <p>Anything thrown from {@code io.cargoiq.domain} or {@code io.cargoiq.application}
 * extends this. The web layer's {@code GlobalExceptionHandler} maps subclasses
 * to HTTP status codes — the domain itself never imports {@code HttpStatus}.
 */
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) { super(message); }
    protected DomainException(String message, Throwable cause) { super(message, cause); }
}
