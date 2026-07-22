package io.cargoiq.application.service;

import io.cargoiq.application.port.in.ManageApiKeysUseCase;
import io.cargoiq.application.port.out.SecretCipherPort;
import io.cargoiq.application.port.out.UserApiKeyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages per-user LLM API keys: validates the provider, encrypts on the way in,
 * decrypts on the way out (for internal use only).
 *
 * @author Vishal Dogra
 */
@Service
public class ApiKeyService implements ManageApiKeysUseCase {

    private final UserApiKeyRepository repository;
    private final SecretCipherPort cipher;

    public ApiKeyService(UserApiKeyRepository repository, SecretCipherPort cipher) {
        this.repository = repository;
        this.cipher = cipher;
    }

    @Override
    public void setKey(UUID userId, String provider, String plaintextKey) {
        String p = normalise(provider);
        if (plaintextKey == null || plaintextKey.isBlank()) {
            throw new IllegalArgumentException("API key must not be blank");
        }
        repository.upsert(userId, p, cipher.encrypt(plaintextKey.trim()));
    }

    @Override
    public List<String> configuredProviders(UUID userId) {
        return repository.providersFor(userId);
    }

    @Override
    public void deleteKey(UUID userId, String provider) {
        repository.delete(userId, normalise(provider));
    }

    @Override
    public Optional<String> resolveKey(UUID userId, String provider) {
        if (provider == null) return Optional.empty();
        return repository.findEncrypted(userId, provider.toLowerCase(Locale.ROOT).trim())
                .map(cipher::decrypt);
    }

    private static String normalise(String provider) {
        String p = provider == null ? "" : provider.toLowerCase(Locale.ROOT).trim();
        if (!SUPPORTED_PROVIDERS.contains(p)) {
            throw new IllegalArgumentException(
                    "Unsupported provider '" + provider + "'. Supported: " + SUPPORTED_PROVIDERS);
        }
        return p;
    }
}
