package io.cargoiq.adapter.in.web;

import io.cargoiq.adapter.in.web.dto.QueryRequest;
import io.cargoiq.adapter.in.web.dto.QueryResponse;
import io.cargoiq.application.port.in.AnswerQueryUseCase;
import io.cargoiq.application.port.in.LookupHsCodeUseCase;
import io.cargoiq.application.port.in.LookupIncotermUseCase;
import io.cargoiq.application.port.in.ManageApiKeysUseCase;
import io.cargoiq.application.port.in.ManageConversationsUseCase;
import io.cargoiq.domain.model.HsCode;
import io.cargoiq.domain.model.ModelChoice;
import io.cargoiq.domain.model.Query;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST adapter for the query side: RAG, INCOTERM lookup, HS-code lookup.
 *
 * <p>These three endpoints all also exist as MCP tools — see
 * {@code adapter/in/mcp/tools}. Same use cases, two inbound adapters. The
 * controllers and tools never call each other.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "query", description = "Ask questions, look up INCOTERMS and HS codes")
public class QueryController {

    private final AnswerQueryUseCase answerQuery;
    private final LookupIncotermUseCase lookupIncoterm;
    private final LookupHsCodeUseCase lookupHsCode;
    private final ManageConversationsUseCase conversations;
    private final ManageApiKeysUseCase apiKeys;

    public QueryController(
            AnswerQueryUseCase answerQuery,
            LookupIncotermUseCase lookupIncoterm,
            LookupHsCodeUseCase lookupHsCode,
            ManageConversationsUseCase conversations,
            ManageApiKeysUseCase apiKeys) {
        this.answerQuery = answerQuery;
        this.lookupIncoterm = lookupIncoterm;
        this.lookupHsCode = lookupHsCode;
        this.conversations = conversations;
        this.apiKeys = apiKeys;
    }

    @Operation(summary = "Ask a question grounded in the ingested corpus")
    @PostMapping("/query")
    public QueryResponse ask(@Valid @RequestBody QueryRequest req, @AuthenticationPrincipal Jwt jwt) {
        java.util.UUID userId = ConversationController.userId(jwt);

        // When the query threads a conversation, make sure it exists and belongs
        // to the caller before answering — the answering service then persists
        // this turn (and reads prior turns) via the DB-backed chat memory.
        if (req.conversationId() != null && !req.conversationId().isBlank()) {
            conversations.ensureOwned(
                    java.util.UUID.fromString(req.conversationId()), userId, req.query());
        }

        var query = new Query(
                req.query(),
                req.topKOrDefault(),
                Optional.ofNullable(req.filterByType()),
                Optional.ofNullable(req.filterByIncoterm()),
                resolveModelChoice(req.modelChoice(), userId),
                req.retrievalOptions(),
                req.conversationId());
        return QueryResponse.from(answerQuery.answer(query), query);
    }

    /**
     * Attach the caller's personal API key for the chosen provider (if they have
     * stored one), so the answer runs on their own OpenAI/Anthropic account
     * rather than the server default.
     */
    private ModelChoice resolveModelChoice(ModelChoice choice, java.util.UUID userId) {
        if (choice == null || !choice.hasProvider()) return choice;
        return apiKeys.resolveKey(userId, choice.providerId())
                .map(choice::withApiKey)
                .orElse(choice);
    }

    @Operation(summary = "Look up an INCOTERM 2020 rule by code (e.g. CIF, FOB, DDP)")
    @GetMapping("/incoterms/{code}")
    public LookupIncotermUseCase.IncotermDetail incoterm(@PathVariable String code) {
        return lookupIncoterm.lookup(code);
    }

    @Operation(summary = "Look up an HS code by exact code")
    @GetMapping("/hs-codes/{code}")
    public HsCode hsCodeByCode(@PathVariable String code) {
        return lookupHsCode.byCode(code);
    }

    @Operation(summary = "Search HS codes by description text")
    @GetMapping("/hs-codes/search")
    public List<HsCode> hsCodeSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        return lookupHsCode.search(q, limit);
    }
}
