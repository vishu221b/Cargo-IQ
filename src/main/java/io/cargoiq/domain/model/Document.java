package io.cargoiq.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A document ingested into the corpus — a Bill of Lading, Commercial Invoice,
 * Letter of Credit, or reference text (e.g. the INCOTERMS 2020 rule book).
 *
 * <p>The aggregate root for the ingest side. Owns its chunks. Carries
 * structured trade-finance metadata extracted at parse time (vessel, ports,
 * incoterm, value) so downstream tools can filter without re-parsing text.
 *
 * <p>Pure domain — no JPA, no Spring, no Jackson. The {@code adapter/out/persistence}
 * layer maps to/from a JPA entity; the {@code adapter/in/web/dto} layer maps to/from
 * REST DTOs. Both directions are explicit and one-way. The domain never knows
 * about either.
 */
public final class Document {

    private final UUID id;
    private final String title;
    private final DocumentType type;
    private final String sourceUri;
    private final ShipmentMetadata metadata;
    private final List<DocumentChunk> chunks;
    private final Instant ingestedAt;

    public Document(
            UUID id,
            String title,
            DocumentType type,
            String sourceUri,
            ShipmentMetadata metadata,
            List<DocumentChunk> chunks,
            Instant ingestedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        this.type = Objects.requireNonNull(type, "type");
        this.sourceUri = sourceUri; // optional
        this.metadata = metadata != null ? metadata : ShipmentMetadata.empty();
        this.chunks = chunks != null ? List.copyOf(chunks) : Collections.emptyList();
        this.ingestedAt = Objects.requireNonNull(ingestedAt, "ingestedAt");
    }

    /** Factory for newly-parsed docs that have not been persisted yet. */
    public static Document newFromIngest(
            String title, DocumentType type, String sourceUri,
            ShipmentMetadata metadata, List<DocumentChunk> chunks) {
        return new Document(UUID.randomUUID(), title, type, sourceUri,
                metadata, chunks, Instant.now());
    }

    public UUID id() { return id; }
    public String title() { return title; }
    public DocumentType type() { return type; }
    public String sourceUri() { return sourceUri; }
    public ShipmentMetadata metadata() { return metadata; }
    public List<DocumentChunk> chunks() { return chunks; }
    public Instant ingestedAt() { return ingestedAt; }

    public int chunkCount() { return chunks.size(); }

    @Override
    public boolean equals(Object o) {
        return o instanceof Document d && id.equals(d.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }
}
