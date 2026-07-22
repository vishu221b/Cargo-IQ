package io.cargoiq.adapter.out.persistence.jpa;

import io.cargoiq.application.port.out.UserApiKeyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implements {@link UserApiKeyRepository} via Spring Data JPA. */
@Repository
public class UserApiKeyRepositoryAdapter implements UserApiKeyRepository {

    private final UserApiKeyJpaRepository jpa;

    public UserApiKeyRepositoryAdapter(UserApiKeyJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void upsert(UUID userId, String provider, String encryptedKey) {
        jpa.save(new UserApiKeyEntity(userId, provider, encryptedKey));
    }

    @Override
    public Optional<String> findEncrypted(UUID userId, String provider) {
        return jpa.findById(new UserApiKeyEntity.Key(userId, provider))
                .map(UserApiKeyEntity::getEncryptedKey);
    }

    @Override
    public List<String> providersFor(UUID userId) {
        return jpa.findByIdUserId(userId).stream()
                .map(UserApiKeyEntity::getProvider)
                .sorted()
                .toList();
    }

    @Override
    public void delete(UUID userId, String provider) {
        jpa.deleteById(new UserApiKeyEntity.Key(userId, provider));
    }
}
