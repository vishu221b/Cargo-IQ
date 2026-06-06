package io.cargoiq.application.service;

import io.cargoiq.application.port.in.AuthenticateUserUseCase;
import io.cargoiq.application.port.out.PasswordHasherPort;
import io.cargoiq.application.port.out.TokenIssuerPort;
import io.cargoiq.application.port.out.UserRepository;
import io.cargoiq.domain.exception.InvalidCredentialsException;
import io.cargoiq.domain.model.AuthToken;
import io.cargoiq.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Use case: authenticate a username/password pair and issue a bearer token.
 *
 * <p>The {@link PasswordHasherPort#matches} check runs even when the user is
 * absent would be ideal to equalise timing; for v1 we keep it simple and rely
 * on a single {@link InvalidCredentialsException} that never reveals which half
 * failed. Token minting is delegated to {@link TokenIssuerPort}.
 *
 * @author Vishal Dogra
 */
@Service
public class AuthenticateUserService implements AuthenticateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthenticateUserService.class);

    private final UserRepository users;
    private final PasswordHasherPort passwordHasher;
    private final TokenIssuerPort tokenIssuer;

    public AuthenticateUserService(UserRepository users,
                                   PasswordHasherPort passwordHasher,
                                   TokenIssuerPort tokenIssuer) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public AuthToken authenticate(Credentials credentials) {
        User user = users.findByUsername(credentials.username())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(credentials.rawPassword(), user.passwordHash())) {
            log.debug("Failed login for '{}'", credentials.username());
            throw new InvalidCredentialsException();
        }
        log.info("Issued token for '{}'", user.username());
        return tokenIssuer.issue(user);
    }
}
