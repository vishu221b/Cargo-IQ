package io.cargoiq.application.port.in;

import io.cargoiq.domain.model.Role;
import io.cargoiq.domain.model.User;

import java.util.Set;

/**
 * Inbound port: create a new user account.
 *
 * <p>Self-service registration always yields a {@link Role#USER}. Granting
 * {@link Role#ADMIN} is an administrative action, not something a public
 * endpoint exposes — the seeded admin (dev) or a future admin-only endpoint
 * is the path for that.
 *
 * @author Vishal Dogra
 */
public interface RegisterUserUseCase {

    User register(RegisterCommand command);

    record RegisterCommand(String username, String rawPassword, Set<Role> roles) {

        /** Public self-registration: a plain {@link Role#USER}. */
        public static RegisterCommand selfSignup(String username, String rawPassword) {
            return new RegisterCommand(username, rawPassword, Set.of(Role.USER));
        }
    }
}
