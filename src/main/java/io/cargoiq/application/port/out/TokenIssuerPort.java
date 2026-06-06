package io.cargoiq.application.port.out;

import io.cargoiq.domain.model.AuthToken;
import io.cargoiq.domain.model.User;

/**
 * Outbound port: mint a signed bearer token for an authenticated user.
 *
 * <p>The application asks for an {@link AuthToken} in domain terms; the adapter
 * decides it's a JWT signed with HS256. If we later move to opaque tokens
 * backed by a store, or to asymmetric RS256 keys, only the adapter changes.
 */
public interface TokenIssuerPort {

    AuthToken issue(User user);
}
