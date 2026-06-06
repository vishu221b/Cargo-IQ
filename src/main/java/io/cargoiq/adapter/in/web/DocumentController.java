package io.cargoiq.adapter.in.web;

import io.cargoiq.adapter.in.web.dto.DocumentResponse;
import io.cargoiq.adapter.in.web.dto.IngestRequest;
import io.cargoiq.application.port.in.IngestDocumentUseCase;
import io.cargoiq.application.port.in.ListDocumentsUseCase;
import io.cargoiq.domain.model.DocumentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST adapter for the document corpus.
 *
 * <p>This controller is intentionally thin — it does DTO ↔ domain conversion
 * and HTTP status mapping, nothing else. Business decisions belong in
 * {@code IngestDocumentService} et al., not here.
 */
@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "documents", description = "Ingest and browse the cargo-document corpus")
public class DocumentController {

    private final IngestDocumentUseCase ingest;
    private final ListDocumentsUseCase listing;

    public DocumentController(IngestDocumentUseCase ingest, ListDocumentsUseCase listing) {
        this.ingest = ingest;
        this.listing = listing;
    }

    @Operation(summary = "Ingest a document into the corpus (chunks, embeds, indexes)")
    @PostMapping
    public ResponseEntity<DocumentResponse> ingest(@Valid @RequestBody IngestRequest req) {
        var saved = ingest.ingest(new IngestDocumentUseCase.IngestCommand(
                req.title(), req.type(), req.sourceUri(), req.text()));
        var body = DocumentResponse.from(saved);
        return ResponseEntity
                .created(URI.create("/api/v1/documents/" + saved.id()))
                .body(body);
    }

    @Operation(summary = "List documents (optionally filtered by type)")
    @GetMapping
    public List<DocumentResponse> list(
            @RequestParam(required = false) DocumentType type,
            @RequestParam(defaultValue = "50") int limit) {
        return listing.list(Optional.ofNullable(type), limit).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Operation(summary = "Get a single document by ID")
    @GetMapping("/{id}")
    public DocumentResponse byId(@PathVariable UUID id) {
        return DocumentResponse.from(listing.byId(id));
    }
}
