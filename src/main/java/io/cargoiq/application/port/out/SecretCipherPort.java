package io.cargoiq.application.port.out;

/**
 * Outbound port: symmetric encryption for secrets at rest (user API keys).
 *
 * <p>The application deals in plaintext; the adapter turns it into ciphertext
 * for storage and back. Keeping this behind a port means the crypto choice
 * (AES-GCM today) can change without touching the services that use it.
 *
 * @author Vishal Dogra
 */
public interface SecretCipherPort {

    String encrypt(String plaintext);

    String decrypt(String ciphertext);
}
