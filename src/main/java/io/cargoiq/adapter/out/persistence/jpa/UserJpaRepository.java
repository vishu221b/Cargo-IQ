package io.cargoiq.adapter.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link UserEntity} — framework concern, confined
 * to this package. The application talks to
 * {@link io.cargoiq.application.port.out.UserRepository}; the
 * {@link UserRepositoryAdapter} bridges the two.
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
