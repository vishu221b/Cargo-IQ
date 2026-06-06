package io.cargoiq.config;

import io.cargoiq.application.port.in.RegisterUserUseCase;
import io.cargoiq.application.port.out.UserRepository;
import io.cargoiq.domain.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Set;

/**
 * Seeds a default admin account on first run so the app is usable immediately
 * for local development — self-registration only ever grants USER, so without
 * this there would be no way to exercise the ADMIN-only endpoints.
 *
 * <p>Active in the {@code dev} profile only. In any other profile the admin is
 * expected to be provisioned out-of-band, so no default credentials ship.
 */
@Configuration
@Profile("dev")
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedAdmin(
            UserRepository users,
            RegisterUserUseCase register,
            @Value("${cargoiq.security.bootstrap-admin.username:admin}") String username,
            @Value("${cargoiq.security.bootstrap-admin.password:admin12345}") String password) {
        return args -> {
            if (users.existsByUsername(username)) {
                return;
            }
            register.register(new RegisterUserUseCase.RegisterCommand(
                    username, password, Set.of(Role.ADMIN, Role.USER)));
            log.warn("Seeded default DEV admin user '{}' — change the password before exposing this app",
                    username);
        };
    }
}
