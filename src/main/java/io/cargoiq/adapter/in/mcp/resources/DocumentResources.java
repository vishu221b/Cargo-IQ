package io.cargoiq.adapter.in.mcp.resources;

import io.cargoiq.application.port.in.ListDocumentsUseCase;
import io.cargoiq.application.port.out.DocumentContentPort;
import io.cargoiq.domain.model.Document;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MCP <b>resources</b> — read-only context an MCP client can attach to a
 * conversation, complementing the action-oriented {@code @Tool}s.
 *
 * <p>Two resources, discovered automatically by Spring AI's MCP annotation
 * scanner:
 * <ul>
 *   <li><b>{@code cargo://documents}</b> — a fixed index resource listing every
 *       ingested document with its id, title and type.</li>
 *   <li><b>{@code cargo://documents/{id}}</b> — a templated resource returning a
 *       single document's full text, reassembled from its indexed chunks.</li>
 * </ul>
 *
 * <p>Because chunk text lives only in the vector store, the full-text read goes
 * through {@link DocumentContentPort} rather than the JPA aggregate.
 *
 * @author Vishal Dogra
 */
@Component
public class DocumentResources {

    private static final Logger log = LoggerFactory.getLogger(DocumentResources.class);
    private static final String MIME = "text/plain";

    private final ListDocumentsUseCase listDocuments;
    private final DocumentContentPort documentContent;

    public DocumentResources(ListDocumentsUseCase listDocuments, DocumentContentPort documentContent) {
        this.listDocuments = listDocuments;
        this.documentContent = documentContent;
    }

    @McpResource(
            uri = "cargo://documents",
            name = "cargo_documents_index",
            title = "Ingested document index",
            description = "Index of every document in the cargo-iq corpus: id, title, and type. "
                    + "Read cargo://documents/{id} for a document's full text.",
            mimeType = MIME)
    public ReadResourceResult index() {
        List<Document> docs = listDocuments.list(java.util.Optional.empty(), 500);
        String body = docs.isEmpty()
                ? "The corpus is empty. Ingest documents via the ingest_cargo_document tool or the REST API."
                : docs.stream()
                        .map(d -> "- " + d.id() + "  [" + d.type() + "]  " + d.title())
                        .collect(Collectors.joining("\n"));
        String text = "cargo-iq document index (" + docs.size() + " documents)\n\n" + body;
        return textResult("cargo://documents", text);
    }

    @McpResource(
            uri = "cargo://documents/{id}",
            name = "cargo_document",
            title = "Cargo document full text",
            description = "The full extracted text of a single ingested document, by its UUID.",
            mimeType = MIME)
    public ReadResourceResult document(ReadResourceRequest request, String id) {
        UUID documentId;
        try {
            documentId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return textResult(request.uri(), "Invalid document id: '" + id + "' (expected a UUID).");
        }

        Document doc = listDocuments.byId(documentId); // throws DocumentNotFoundException → MCP error
        String content = documentContent.joinedText(documentId);
        if (content.isBlank()) {
            content = "(No indexed chunk text found for this document.)";
        }

        String header = """
                Title: %s
                Type: %s
                Ingested: %s
                Chunks: %d
                --------------------------------------------------------------------------------

                """.formatted(doc.title(), doc.type(), doc.ingestedAt(), doc.chunkCount());

        log.debug("MCP resource read for document {}", documentId);
        return textResult(request.uri(), header + content);
    }

    private static ReadResourceResult textResult(String uri, String text) {
        return new ReadResourceResult(List.of(new TextResourceContents(uri, MIME, text)));
    }
}
