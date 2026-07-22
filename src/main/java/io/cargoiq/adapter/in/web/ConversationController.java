package io.cargoiq.adapter.in.web;

import io.cargoiq.adapter.in.web.dto.ConversationDetailResponse;
import io.cargoiq.adapter.in.web.dto.ConversationResponse;
import io.cargoiq.application.port.in.ManageConversationsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for a user's persisted chat history. Every operation is scoped to
 * the authenticated principal — the {@code uid} claim on the bearer JWT — so a
 * user only ever sees or mutates their own conversations.
 *
 * @author Vishal Dogra
 */
@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "conversations", description = "Persisted chat history (per user)")
public class ConversationController {

    private final ManageConversationsUseCase conversations;

    public ConversationController(ManageConversationsUseCase conversations) {
        this.conversations = conversations;
    }

    @Operation(summary = "Start a new (empty) conversation")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse create(@AuthenticationPrincipal Jwt jwt) {
        return ConversationResponse.from(conversations.create(userId(jwt)));
    }

    @Operation(summary = "List the caller's conversations, most recent first")
    @GetMapping
    public List<ConversationResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return conversations.list(userId(jwt)).stream().map(ConversationResponse::from).toList();
    }

    @Operation(summary = "Get a conversation and its full message history")
    @GetMapping("/{id}")
    public ConversationDetailResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ConversationDetailResponse.from(conversations.get(id, userId(jwt)));
    }

    @Operation(summary = "Delete a conversation and all of its messages")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        conversations.delete(id, userId(jwt));
    }

    /** The authenticated user's id, from the {@code uid} JWT claim. */
    static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("uid"));
    }
}
