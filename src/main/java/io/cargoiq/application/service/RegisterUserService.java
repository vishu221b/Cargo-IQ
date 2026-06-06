package io.cargoiq.application.service;

import io.cargoiq.application.port.in.RegisterUserUseCase;
import io.cargoiq.application.port.out.PasswordHasherPort;
import io.cargoiq.application.port.out.UserRepository;
import io.cargoiq.domain.exception.UsernameAlreadyExistsException;
import io.cargoiq.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: register a new user.
 *
 * <p>Hashes the raw password via the {@link PasswordHasherPort} before the
 * {@link User} aggregate is ever constructed, so a raw password never reaches
 * persistence. Uniqueness is enforced both here (friendly 409) and by a DB
 * constraint (the real guard against a race).
 *
 * @author Vishal Dogra
 */
@Service
public class RegisterUserService implements RegisterUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterUserService.class);

    private final UserRepository users;
    private final PasswordHasherPort passwordHasher;

    public RegisterUserService(UserRepository users, PasswordHasherPort passwordHasher) {
        this.users = users;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public User register(RegisterCommand cmd) {
        if (cmd.rawPassword() == null || cmd.rawPassword().length() < 8) {
            throw new IllegalArgumentException("password must be at least 8 characters");
        }
        if (users.existsByUsername(cmd.username())) {
            throw new UsernameAlreadyExistsException(cmd.username());
        }
        var user = User.newAccount(
                cmd.username(),
                passwordHasher.hash(cmd.rawPassword()),
                cmd.roles());
        User saved = users.save(user);
        log.info("Registered user '{}' with roles {}", saved.username(), saved.roles());
        return saved;
    }
}
