package io.cargoiq.adapter.in.web;

import io.cargoiq.application.port.in.ManageApiKeysUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST adapter for per-user settings — currently personal LLM API keys.
 *
 * <p>Keys are scoped to the authenticated user and stored encrypted. They are
 * set and deleted here but <b>never returned</b>; the GET reports only which
 * providers are configured.
 *
 * @author Vishal Dogra
 */
@RestController
@RequestMapping("/api/v1/settings/api-keys")
@Tag(name = "settings", description = "Per-user settings — personal LLM API keys")
public class SettingsController {

    private final ManageApiKeysUseCase apiKeys;

    public SettingsController(ManageApiKeysUseCase apiKeys) {
        this.apiKeys = apiKeys;
    }

    @Operation(summary = "Which providers the caller has a key configured for (keys are never returned)")
    @GetMapping
    public ApiKeysResponse list(@AuthenticationPrincipal Jwt jwt) {
        return new ApiKeysResponse(
                ManageApiKeysUseCase.SUPPORTED_PROVIDERS.stream().sorted().toList(),
                apiKeys.configuredProviders(ConversationController.userId(jwt)));
    }

    @Operation(summary = "Store (or replace) the caller's API key for a provider")
    @PutMapping("/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void set(@PathVariable String provider,
                    @Valid @RequestBody SetApiKeyRequest req,
                    @AuthenticationPrincipal Jwt jwt) {
        apiKeys.setKey(ConversationController.userId(jwt), provider, req.apiKey());
    }

    @Operation(summary = "Remove the caller's API key for a provider")
    @DeleteMapping("/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String provider, @AuthenticationPrincipal Jwt jwt) {
        apiKeys.deleteKey(ConversationController.userId(jwt), provider);
    }

    public record SetApiKeyRequest(@NotBlank @Size(max = 512) String apiKey) {}

    public record ApiKeysResponse(List<String> supported, List<String> configured) {}
}
