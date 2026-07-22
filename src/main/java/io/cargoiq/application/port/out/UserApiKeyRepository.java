package io.cargoiq.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port: persistence for per-user LLM API keys (stored encrypted).
 *
 * @author Vishal Dogra
 */
public interface UserApiKeyRepository {

    /** Insert or replace the encrypted key for a (user, provider) pair. */
    void upsert(UUID userId, String provider, String encryptedKey);

    Optional<String> findEncrypted(UUID userId, String provider);

    /** The providers this user has a key configured for. */
    List<String> providersFor(UUID userId);

    void delete(UUID userId, String provider);
}
