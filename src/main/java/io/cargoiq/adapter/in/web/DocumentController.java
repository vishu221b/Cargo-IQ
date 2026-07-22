package io.cargoiq.adapter.in.web;

import io.cargoiq.adapter.in.web.dto.DocumentContentResponse;
import io.cargoiq.adapter.in.web.dto.DocumentResponse;
import io.cargoiq.adapter.in.web.dto.IngestRequest;
import io.cargoiq.application.port.in.DeleteDocumentUseCase;
import io.cargoiq.application.port.in.IngestDocumentUseCase;
import io.cargoiq.application.port.in.ListDocumentsUseCase;
import io.cargoiq.domain.model.DocumentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
 *
 * @author Vishal Dogra
 */
@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "documents", description = "Ingest and browse the cargo-document corpus")
public class DocumentController {

    private final IngestDocumentUseCase ingest;
    private final ListDocumentsUseCase listing;
    private final DeleteDocumentUseCase deletion;

    public DocumentController(IngestDocumentUseCase ingest,
                              ListDocumentsUseCase listing,
                              DeleteDocumentUseCase deletion) {
        this.ingest = ingest;
        this.listing = listing;
        this.deletion = deletion;
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

    @Operation(summary = "Ingest an uploaded file — PDF, DOCX, HTML or TXT (chunks, embeds, indexes)")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") DocumentType type,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "sourceUri", required = false) String sourceUri) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String effectiveTitle = (title != null && !title.isBlank())
                ? title.trim()
                : (file.getOriginalFilename() != null ? file.getOriginalFilename() : "Uploaded document");
        var saved = ingest.ingestFile(new IngestDocumentUseCase.IngestFileCommand(
                effectiveTitle, type, sourceUri,
                file.getOriginalFilename(), file.getContentType(), file.getBytes()));
        return ResponseEntity
                .created(URI.create("/api/v1/documents/" + saved.id()))
                .body(DocumentResponse.from(saved));
    }

    @Operation(summary = "List documents (optionally filtered by type, paged)")
    @GetMapping
    public List<DocumentResponse> list(
            @RequestParam(required = false) DocumentType type,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return listing.list(Optional.ofNullable(type), limit, offset).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Operation(summary = "Get a single document by ID")
    @GetMapping("/{id}")
    public DocumentResponse byId(@PathVariable UUID id) {
        return DocumentResponse.from(listing.byId(id));
    }

    @Operation(summary = "Get a document's full extracted text (for the in-app viewer)")
    @GetMapping("/{id}/content")
    public DocumentContentResponse content(@PathVariable UUID id) {
        var doc = listing.byId(id);
        return new DocumentContentResponse(doc.id(), doc.title(), doc.type(), listing.content(id));
    }

    @Operation(summary = "Delete a document and all of its indexed chunks (ADMIN only)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deletion.delete(id);
    }
}
