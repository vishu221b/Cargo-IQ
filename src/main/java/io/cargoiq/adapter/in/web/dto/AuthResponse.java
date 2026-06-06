package io.cargoiq.adapter.in.web.dto;

import io.cargoiq.domain.model.AuthToken;
import io.cargoiq.domain.model.Role;

import java.util.List;

/**
 * Login response. {@code accessToken} is a bearer JWT — clients send it as
 * {@code Authorization: Bearer <token>} on subsequent calls.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        List<String> roles) {

    public static AuthResponse from(AuthToken token) {
        return new AuthResponse(
                token.value(),
                token.tokenType(),
                token.expiresInSeconds(),
                token.roles().stream().map(Role::name).sorted().toList());
    }
}
