package io.cargoiq.domain.exception;

/**
 * Raised when a login attempt fails — wrong username or password.
 *
 * <p>Deliberately does not distinguish "no such user" from "wrong password":
 * leaking which half was wrong helps an attacker enumerate valid usernames.
 */
public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
