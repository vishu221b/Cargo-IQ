package io.cargoiq.domain.exception;

/** Raised when registration is attempted for a username that is already taken. */
public class UsernameAlreadyExistsException extends DomainException {
    public UsernameAlreadyExistsException(String username) {
        super("Username already taken: " + username);
    }
}
