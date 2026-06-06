package io.cargoiq.adapter.out.persistence.jpa;

import io.cargoiq.application.port.out.UserRepository;
import io.cargoiq.domain.model.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Implements the {@link UserRepository} port by delegating to Spring Data JPA,
 * mapping {@link UserEntity} ↔ {@link User} at the boundary.
 */
@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User save(User user) {
        jpa.save(UserEntity.fromDomain(user));
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username).map(UserEntity::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpa.existsByUsername(username);
    }
}
