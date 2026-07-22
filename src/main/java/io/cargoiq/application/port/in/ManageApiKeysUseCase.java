package io.cargoiq.application.port.in;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Inbound port: manage a user's personal LLM API keys.
 *
 * <p>Keys are stored encrypted and scoped to the user. They are set and deleted
 * via REST but <b>never returned</b> — {@link #configuredProviders} only reports
 * which providers have a key. {@link #resolveKey} (decrypted) is for internal
 * use by the query flow, not the web layer.
 *
 * @author Vishal Dogra
 */
public interface ManageApiKeysUseCase {

    /** Providers a user may store a key for (and that the chat flow can use per-user). */
    Set<String> SUPPORTED_PROVIDERS = Set.of("openai", "anthropic");

    void setKey(UUID userId, String provider, String plaintextKey);

    List<String> configuredProviders(UUID userId);

    void deleteKey(UUID userId, String provider);

    /** The user's decrypted key for a provider, if set — internal use only. */
    Optional<String> resolveKey(UUID userId, String provider);
}
