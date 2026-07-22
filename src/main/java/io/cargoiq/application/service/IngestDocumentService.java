package io.cargoiq.application.service;

import io.cargoiq.application.port.in.IngestDocumentUseCase;
import io.cargoiq.application.port.out.DocumentParserPort;
import io.cargoiq.application.port.out.DocumentRepository;
import io.cargoiq.application.port.out.FileTextExtractorPort;
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
 * back — the vector store insert is non-transactional, so at production scale
 * the adapter would own compensating writes. The {@link Transactional} on this
 * method documents that boundary.
 *
 * @author Vishal Dogra
 */
@Service
public class IngestDocumentService implements IngestDocumentUseCase {

    private static final Logger log = LoggerFactory.getLogger(IngestDocumentService.class);

    private final DocumentParserPort parser;
    private final DocumentRepository repository;
    private final VectorStorePort vectorStore;
    private final FileTextExtractorPort fileTextExtractor;

    public IngestDocumentService(
            DocumentParserPort parser,
            DocumentRepository repository,
            VectorStorePort vectorStore,
            FileTextExtractorPort fileTextExtractor) {
        this.parser = parser;
        this.repository = repository;
        this.vectorStore = vectorStore;
        this.fileTextExtractor = fileTextExtractor;
    }

    @Override
    @Transactional
    public Document ingest(IngestCommand cmd) {
        return persist(cmd.title(), cmd.type(), cmd.sourceUri(), cmd.rawText(), "pasted text");
    }

    @Override
    @Transactional
    public Document ingestFile(IngestFileCommand cmd) {
        String text = fileTextExtractor.extract(cmd.bytes(), cmd.filename(), cmd.contentType());
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "No extractable text found in '" + cmd.filename() + "'. "
                    + "Scanned/image-only PDFs need OCR, which isn't enabled.");
        }
        String sourceUri = cmd.sourceUri() != null ? cmd.sourceUri() : "file://" + cmd.filename();
        return persist(cmd.title(), cmd.type(), sourceUri, text, "file " + cmd.filename());
    }

    /** Shared ingest pipeline: parse → persist aggregate (JPA) → embed + index (vector store). */
    private Document persist(String title, io.cargoiq.domain.model.DocumentType type,
                             String sourceUri, String rawText, String origin) {
        UUID newId = UUID.randomUUID();
        log.info("Ingesting document title='{}' type={} chars={} from {}",
                title, type, rawText.length(), origin);

        var parsed = parser.parse(newId, type, rawText);

        Document doc = new Document(
                newId,
                title,
                type,
                sourceUri,
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
