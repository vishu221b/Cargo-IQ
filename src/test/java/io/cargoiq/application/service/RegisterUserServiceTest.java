package io.cargoiq.application.service;

import io.cargoiq.application.port.in.RegisterUserUseCase.RegisterCommand;
import io.cargoiq.application.port.out.PasswordHasherPort;
import io.cargoiq.application.port.out.UserRepository;
import io.cargoiq.domain.exception.UsernameAlreadyExistsException;
import io.cargoiq.domain.model.Role;
import io.cargoiq.domain.model.User;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit test for {@link RegisterUserService} with fake ports.
 *
 * @author Vishal Dogra
 */
class RegisterUserServiceTest {

    @Test
    void hashesPasswordAndPersistsNewUser() {
        var repo = new InMemoryUserRepository();
        var service = new RegisterUserService(repo, reversingHasher());

        User saved = service.register(RegisterCommand.selfSignup("alice", "supersecret"));

        assertThat(saved.username()).isEqualTo("alice");
        assertThat(saved.roles()).containsExactly(Role.USER);
        // raw password is never stored verbatim
        assertThat(saved.passwordHash()).isNotEqualTo("supersecret");
        assertThat(repo.findByUsername("alice")).isPresent();
    }

    @Test
    void rejectsDuplicateUsername() {
        var repo = new InMemoryUserRepository();
        var service = new RegisterUserService(repo, reversingHasher());
        service.register(RegisterCommand.selfSignup("bob", "supersecret"));

        assertThatThrownBy(() -> service.register(RegisterCommand.selfSignup("bob", "anotherpw")))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void rejectsTooShortPassword() {
        var service = new RegisterUserService(new InMemoryUserRepository(), reversingHasher());

        assertThatThrownBy(() -> service.register(RegisterCommand.selfSignup("carol", "short")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- fakes ----

    /** A trivial deterministic "hash" — enough to prove the raw value is transformed. */
    private static PasswordHasherPort reversingHasher() {
        return new PasswordHasherPort() {
            @Override public String hash(String raw) { return new StringBuilder(raw).reverse().toString(); }
            @Override public boolean matches(String raw, String hash) { return hash.equals(hash(raw)); }
        };
    }

    private static final class InMemoryUserRepository implements UserRepository {
        private final Map<String, User> byName = new HashMap<>();
        @Override public User save(User u) { byName.put(u.username(), u); return u; }
        @Override public Optional<User> findByUsername(String name) { return Optional.ofNullable(byName.get(name)); }
        @Override public boolean existsByUsername(String name) { return byName.containsKey(name); }
    }
}
