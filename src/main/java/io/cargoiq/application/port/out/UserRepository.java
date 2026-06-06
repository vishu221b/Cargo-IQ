package io.cargoiq.application.port.out;

import io.cargoiq.domain.model.User;

import java.util.Optional;

/**
 * Outbound port (SPI): persistence for {@link User} accounts.
 *
 * <p>As with {@code DocumentRepository}, the application owns this contract in
 * domain terms; the JPA adapter implements it. Spring Security's
 * {@code UserDetailsService} is deliberately <i>not</i> used here — we
 * authenticate inside a use case against this port, keeping the security
 * framework at the adapter edge rather than letting it reach into the core.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
