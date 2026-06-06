package io.cargoiq.application.port.in;

import io.cargoiq.domain.model.AuthToken;

/**
 * Inbound port: exchange username + password for a signed bearer token.
 *
 * @author Vishal Dogra
 */
public interface AuthenticateUserUseCase {

    AuthToken authenticate(Credentials credentials);

    record Credentials(String username, String rawPassword) {}
}
