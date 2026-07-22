package io.cargoiq.adapter.out.security;

import io.cargoiq.application.port.out.SecretCipherPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

/**
 * {@link SecretCipherPort} backed by Spring Security's {@code Encryptors.delux}
 * (AES-256 GCM with a random IV per value).
 *
 * <p>The password and hex salt come from configuration and MUST be overridden in
 * any real deployment ({@code SECRETS_KEY} / {@code SECRETS_SALT}) — rotating
 * them makes previously stored keys undecryptable, which is the intended
 * "invalidate everything" behaviour.
 *
 * @author Vishal Dogra
 */
@Component
public class SpringSecuritySecretCipher implements SecretCipherPort {

    private final TextEncryptor encryptor;

    public SpringSecuritySecretCipher(
            @Value("${cargoiq.security.secrets.key:dev-secrets-encryption-password-change-me}") String password,
            @Value("${cargoiq.security.secrets.salt:0123456789abcdef}") String saltHex) {
        // delux = AES/GCM/NoPadding with a per-message random IV; salt is hex.
        this.encryptor = Encryptors.delux(password, saltHex);
    }

    @Override
    public String encrypt(String plaintext) {
        return encryptor.encrypt(plaintext);
    }

    @Override
    public String decrypt(String ciphertext) {
        return encryptor.decrypt(ciphertext);
    }
}
