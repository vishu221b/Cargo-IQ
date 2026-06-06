package io.cargoiq.adapter.out.persistence.jpa;

import io.cargoiq.domain.model.Role;
import io.cargoiq.domain.model.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JPA persistence model for a {@link User}. Roles are stored in a side table
 * ({@code user_roles}) via {@link ElementCollection} — a small, bounded set, so
 * a join table is cheaper to reason about than a delimited string column and
 * keeps each role individually indexable.
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", length = 16)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = EnumSet.noneOf(Role.class);

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserEntity() {} // JPA

    public static UserEntity fromDomain(User u) {
        var e = new UserEntity();
        e.id = u.id();
        e.username = u.username();
        e.passwordHash = u.passwordHash();
        e.roles = u.roles().isEmpty() ? EnumSet.of(Role.USER) : EnumSet.copyOf(u.roles());
        e.createdAt = u.createdAt();
        return e;
    }

    public User toDomain() {
        return new User(id, username, passwordHash,
                roles.stream().collect(Collectors.toUnmodifiableSet()), createdAt);
    }
}
