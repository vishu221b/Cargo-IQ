package io.cargoiq.application.service;

import io.cargoiq.application.port.in.AuthenticateUserUseCase.Credentials;
import io.cargoiq.application.port.out.PasswordHasherPort;
import io.cargoiq.application.port.out.TokenIssuerPort;
import io.cargoiq.application.port.out.UserRepository;
import io.cargoiq.domain.exception.InvalidCredentialsException;
import io.cargoiq.domain.model.AuthToken;
import io.cargoiq.domain.model.Role;
import io.cargoiq.domain.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit test for {@link AuthenticateUserService} with fake ports.
 *
 * @author Vishal Dogra
 */
class AuthenticateUserServiceTest {

    private final User stored = new User(
            java.util.UUID.randomUUID(), "alice", "HASH(secret)",
            Set.of(Role.USER), Instant.now());

    @Test
    void issuesTokenForValidCredentials() {
        var service = new AuthenticateUserService(
                repoReturning(stored), prefixHasher(), stubIssuer());

        AuthToken token = service.authenticate(new Credentials("alice", "secret"));

        assertThat(token.value()).isEqualTo("token-for-alice");
        assertThat(token.tokenType()).isEqualTo(AuthToken.BEARER);
        assertThat(token.roles()).containsExactly(Role.USER);
    }

    @Test
    void rejectsWrongPassword() {
        var service = new AuthenticateUserService(
                repoReturning(stored), prefixHasher(), stubIssuer());

        assertThatThrownBy(() -> service.authenticate(new Credentials("alice", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsUnknownUser() {
        var service = new AuthenticateUserService(
                repoReturning(null), prefixHasher(), stubIssuer());

        assertThatThrownBy(() -> service.authenticate(new Credentials("nobody", "secret")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // ---- fakes ----

    private static UserRepository repoReturning(User user) {
        return new UserRepository() {
            @Override public User save(User u) { return u; }
            @Override public Optional<User> findByUsername(String name) { return Optional.ofNullable(user); }
            @Override public boolean existsByUsername(String name) { return user != null; }
        };
    }

    /** matches(raw, hash) iff hash == "HASH(" + raw + ")". */
    private static PasswordHasherPort prefixHasher() {
        return new PasswordHasherPort() {
            @Override public String hash(String raw) { return "HASH(" + raw + ")"; }
            @Override public boolean matches(String raw, String hash) { return hash.equals(hash(raw)); }
        };
    }

    private static TokenIssuerPort stubIssuer() {
        return user -> new AuthToken(
                "token-for-" + user.username(), AuthToken.BEARER,
                Instant.now(), Instant.now().plusSeconds(3600), user.roles());
    }
}
