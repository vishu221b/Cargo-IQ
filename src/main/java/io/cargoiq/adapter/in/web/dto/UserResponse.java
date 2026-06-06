package io.cargoiq.adapter.in.web.dto;

import io.cargoiq.domain.model.Role;
import io.cargoiq.domain.model.User;

import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        List<String> roles) {

    public static UserResponse from(User u) {
        return new UserResponse(
                u.id(),
                u.username(),
                u.roles().stream().map(Role::name).sorted().toList());
    }
}
