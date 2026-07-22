package io.cargoiq.adapter.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data repo for per-user API keys — internal to the persistence adapter. */
public interface UserApiKeyJpaRepository extends JpaRepository<UserApiKeyEntity, UserApiKeyEntity.Key> {

    List<UserApiKeyEntity> findByIdUserId(UUID userId);
}
