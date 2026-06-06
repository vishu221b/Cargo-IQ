package io.cargoiq.adapter.out.persistence.jpa;

import io.cargoiq.domain.model.Document;
import io.cargoiq.domain.model.DocumentType;
import io.cargoiq.domain.model.Incoterm;
import io.cargoiq.domain.model.ShipmentMetadata;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA persistence model for a document.
 *
 * <p>Kept deliberately separate from {@link Document} (the domain aggregate) —
 * the entity carries JPA annotations, mutable fields for Hibernate, and
 * column metadata; the domain object stays pure. Mapping happens in
 * {@link DocumentRepositoryAdapter}.
 *
 * <p>Chunks are NOT persisted as JPA-children here — the chunk text lives in
 * the pgvector {@code vector_store} table alongside the embedding. Storing it
 * twice (in JPA + in pgvector) would burn disk for no gain. Trade-off:
 * deleting a Document means deleting from both stores explicitly.
 */
@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_type", columnList = "type"),
        @Index(name = "idx_documents_ingested_at", columnList = "ingested_at")
})
public class DocumentEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 256)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DocumentType type;

    @Column(name = "source_uri", length = 512)
    private String sourceUri;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    // --- Extracted metadata, flattened for queryability ---
    private String vesselName;
    private String blNumber;
    private String portOfLoading;
    private String portOfDischarge;

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private Incoterm incoterm;

    @Column(precision = 19, scale = 4)
    private BigDecimal invoiceValue;

    @Column(length = 3)
    private String currency;

    private LocalDate issueDate;
    private String shipper;
    private String consignee;

    protected DocumentEntity() {} // JPA

    public static DocumentEntity fromDomain(Document d) {
        var e = new DocumentEntity();
        e.id = d.id();
        e.title = d.title();
        e.type = d.type();
        e.sourceUri = d.sourceUri();
        e.chunkCount = d.chunkCount();
        e.ingestedAt = d.ingestedAt();
        var m = d.metadata();
        e.vesselName = m.vesselName().orElse(null);
        e.blNumber = m.blNumber().orElse(null);
        e.portOfLoading = m.portOfLoading().orElse(null);
        e.portOfDischarge = m.portOfDischarge().orElse(null);
        e.incoterm = m.incoterm().orElse(null);
        e.invoiceValue = m.invoiceValue().orElse(null);
        e.currency = m.currency().orElse(null);
        e.issueDate = m.issueDate().orElse(null);
        e.shipper = m.shipper().orElse(null);
        e.consignee = m.consignee().orElse(null);
        return e;
    }

    /**
     * Reconstruct a domain {@link Document}. Chunks are NOT included — the
     * repository tier never needs them after the initial ingest; the vector
     * store owns chunk text for retrieval purposes.
     */
    public Document toDomain() {
        var meta = ShipmentMetadata.builder()
                .vesselName(vesselName)
                .blNumber(blNumber)
                .portOfLoading(portOfLoading)
                .portOfDischarge(portOfDischarge)
                .incoterm(incoterm)
                .invoiceValue(invoiceValue)
                .currency(currency)
                .issueDate(issueDate)
                .shipper(shipper)
                .consignee(consignee)
                .build();
        return new Document(id, title, type, sourceUri, meta,
                java.util.List.of(), ingestedAt);
    }

    public UUID getId() { return id; }
    public DocumentType getType() { return type; }
}
