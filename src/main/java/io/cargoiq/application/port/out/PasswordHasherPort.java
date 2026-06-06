package io.cargoiq.application.port.out;

/**
 * Outbound port: one-way password hashing + verification.
 *
 * <p>Keeps the concrete algorithm (BCrypt, in the shipped adapter) out of the
 * application layer. Swapping to Argon2 later means rewriting one adapter, not
 * the registration/authentication use cases.
 */
public interface PasswordHasherPort {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
