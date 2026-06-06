package io.cargoiq.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * An authenticated principal of the system.
 *
 * <p>Pure domain — it holds the <i>hashed</i> password, never a raw one. The
 * hashing itself is an outbound concern ({@code PasswordHasherPort}); the
 * domain only ever receives an already-hashed value, which keeps the BCrypt
 * dependency out of the inner rings.
 *
 * @author Vishal Dogra
 */
public final class User {

    private final UUID id;
    private final String username;
    private final String passwordHash;
    private final Set<Role> roles;
    private final Instant createdAt;

    public User(UUID id, String username, String passwordHash, Set<Role> roles, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.username = Objects.requireNonNull(username, "username");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.roles = roles == null || roles.isEmpty() ? Set.of(Role.USER) : Set.copyOf(roles);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Factory for a brand-new account; defaults to a single {@link Role#USER}. */
    public static User newAccount(String username, String passwordHash, Set<Role> roles) {
        return new User(UUID.randomUUID(), username, passwordHash, roles, Instant.now());
    }

    public UUID id() { return id; }
    public String username() { return username; }
    public String passwordHash() { return passwordHash; }
    public Set<Role> roles() { return roles; }
    public Instant createdAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        return o instanceof User u && id.equals(u.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }
}
