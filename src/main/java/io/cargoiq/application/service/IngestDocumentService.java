package io.cargoiq.application.service;

import io.cargoiq.application.port.in.IngestDocumentUseCase;
import io.cargoiq.application.port.out.DocumentParserPort;
import io.cargoiq.application.port.out.DocumentRepository;
import io.cargoiq.application.port.out.VectorStorePort;
import io.cargoiq.domain.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case: ingest a document.
 *
 * <pre>
 *   raw text + type
 *        │
 *        ▼  DocumentParserPort
 *   chunks + extracted ShipmentMetadata
 *        │
 *        ▼  DocumentRepository      (persist aggregate)
 *        │
 *        ▼  VectorStorePort         (embed chunks, store in pgvector)
 *        │
 *        ▼  return Document
 * </pre>
 *
 * <p>Three ports, one transaction. If indexing fails, the JPA insert rolls
 * back — the vector store insert is non-transactional, so the adapter is
 * responsible for compensating writes if you go to production scale. For the
 * scaffold the happy path is what matters; the {@link Transactional} on this
 * method documents the intent.
 */
@Service
public class IngestDocumentService implements IngestDocumentUseCase {

    private static final Logger log = LoggerFactory.getLogger(IngestDocumentService.class);

    private final DocumentParserPort parser;
    private final DocumentRepository repository;
    private final VectorStorePort vectorStore;

    public IngestDocumentService(
            DocumentParserPort parser,
            DocumentRepository repository,
            VectorStorePort vectorStore) {
        this.parser = parser;
        this.repository = repository;
        this.vectorStore = vectorStore;
    }

    @Override
    @Transactional
    public Document ingest(IngestCommand cmd) {
        UUID newId = UUID.randomUUID();
        log.info("Ingesting document title='{}' type={} bytes={}",
                cmd.title(), cmd.type(), cmd.rawText().length());

        var parsed = parser.parse(newId, cmd.type(), cmd.rawText());

        Document doc = new Document(
                newId,
                cmd.title(),
                cmd.type(),
                cmd.sourceUri(),
                parsed.metadata(),
                parsed.chunks(),
                java.time.Instant.now());

        Document saved = repository.save(doc);

        vectorStore.index(saved.chunks(), new VectorStorePort.ChunkMetadata(
                saved.id(),
                saved.title(),
                saved.type(),
                saved.metadata().incoterm()));

        log.info("Ingested doc {} with {} chunks", saved.id(), saved.chunkCount());
        return saved;
    }
}
