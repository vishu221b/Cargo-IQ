package io.cargoiq.adapter.out.security;

import io.cargoiq.application.port.out.TokenIssuerPort;
import io.cargoiq.domain.model.AuthToken;
import io.cargoiq.domain.model.Role;
import io.cargoiq.domain.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * {@link TokenIssuerPort} that mints HS256-signed JWTs via Spring Security's
 * {@link JwtEncoder}.
 *
 * <p>The {@code roles} claim carries Spring Security authority strings
 * ({@code ROLE_USER}, {@code ROLE_ADMIN}) so the resource-server side can map
 * them straight to {@code GrantedAuthority}s with no prefix translation — the
 * mirror of what {@code SecurityConfig} configures on the decode side.
 *
 * @author Vishal Dogra
 */
@Component
public class JwtTokenIssuer implements TokenIssuerPort {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration ttl;

    public JwtTokenIssuer(
            JwtEncoder jwtEncoder,
            @Value("${cargoiq.security.jwt.issuer:cargo-iq}") String issuer,
            @Value("${cargoiq.security.jwt.ttl-seconds:3600}") long ttlSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @Override
    public AuthToken issue(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(ttl);

        List<String> authorities = user.roles().stream().map(Role::authority).toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(user.username())
                .claim("uid", user.id().toString())
                .claim("roles", authorities)
                .build();

        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(() -> "HS256").build(), claims))
                .getTokenValue();

        return new AuthToken(token, AuthToken.BEARER, now, expiry, user.roles());
    }
}
