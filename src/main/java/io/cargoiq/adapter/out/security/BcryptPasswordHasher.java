package io.cargoiq.adapter.out.security;

import io.cargoiq.application.port.out.PasswordHasherPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * {@link PasswordHasherPort} backed by Spring Security's {@link PasswordEncoder}
 * (BCrypt by default — see {@code SecurityConfig}). This is the only place the
 * application's hashing contract touches the security framework.
 */
@Component
public class BcryptPasswordHasher implements PasswordHasherPort {

    private final PasswordEncoder encoder;

    public BcryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }
}
