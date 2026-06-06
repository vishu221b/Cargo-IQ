package io.cargoiq.domain.model;

import java.time.Instant;
import java.util.Set;

/**
 * The result of a successful authentication: a signed bearer token plus the
 * metadata a client needs to use and refresh it.
 *
 * <p>Domain-level on purpose — the application layer returns this from the
 * {@code AuthenticateUserUseCase}; the web layer maps it to a JSON response and
 * the {@code TokenIssuerPort} adapter is what actually mints {@link #value()}
 * (a JWT, in the shipped implementation). Nothing here knows it's a JWT.
 */
public record AuthToken(
        String value,
        String tokenType,
        Instant issuedAt,
        Instant expiresAt,
        Set<Role> roles) {

    public static final String BEARER = "Bearer";

    public long expiresInSeconds() {
        return java.time.Duration.between(issuedAt, expiresAt).toSeconds();
    }
}
