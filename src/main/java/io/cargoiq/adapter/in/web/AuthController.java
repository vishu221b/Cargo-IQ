package io.cargoiq.adapter.in.web;

import io.cargoiq.adapter.in.web.dto.AuthResponse;
import io.cargoiq.adapter.in.web.dto.LoginRequest;
import io.cargoiq.adapter.in.web.dto.RegisterRequest;
import io.cargoiq.adapter.in.web.dto.UserResponse;
import io.cargoiq.application.port.in.AuthenticateUserUseCase;
import io.cargoiq.application.port.in.RegisterUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST adapter for authentication: self-service registration, login, and a
 * "who am I" endpoint. Thin by design — it maps DTOs to use-case commands and
 * delegates; the credential checking and token minting live in the application
 * and security-adapter layers.
 *
 * @author Vishal Dogra
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "auth", description = "Registration, login (JWT issuance), and current-user lookup")
public class AuthController {

    private final RegisterUserUseCase register;
    private final AuthenticateUserUseCase authenticate;

    public AuthController(RegisterUserUseCase register, AuthenticateUserUseCase authenticate) {
        this.register = register;
        this.authenticate = authenticate;
    }

    @Operation(summary = "Register a new account (always granted the USER role)")
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        var user = register.register(
                RegisterUserUseCase.RegisterCommand.selfSignup(req.username(), req.password()));
        return ResponseEntity
                .created(URI.create("/api/v1/auth/users/" + user.id()))
                .body(UserResponse.from(user));
    }

    @Operation(summary = "Exchange username + password for a bearer JWT")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        var token = authenticate.authenticate(
                new AuthenticateUserUseCase.Credentials(req.username(), req.password()));
        return AuthResponse.from(token);
    }

    @Operation(summary = "Return the identity and roles encoded in the presented JWT")
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) jwt.getClaims().getOrDefault("roles", List.of());
        return new MeResponse(jwt.getSubject(), jwt.getClaimAsString("uid"), roles);
    }

    /** Echoes the authenticated principal back from the validated token. */
    public record MeResponse(String username, String userId, List<String> roles) {}
}
