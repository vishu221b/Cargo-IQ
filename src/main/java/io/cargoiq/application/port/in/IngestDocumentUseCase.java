package io.cargoiq.application.port.in;

import io.cargoiq.domain.model.Document;
import io.cargoiq.domain.model.DocumentType;

/**
 * Inbound port: ingest a raw blob of text + metadata into the corpus.
 *
 * <p>Returns the persisted {@link Document} aggregate (with chunks and ID
 * assigned). Side effects (embedding, persistence) happen synchronously
 * inside the use case implementation — for production scale you'd queue this,
 * but for a portfolio app keeping it synchronous makes debugging trivial and
 * the demo flow legible.
 */
public interface IngestDocumentUseCase {

    Document ingest(IngestCommand command);

    /**
     * The data needed to perform an ingest. A command object (vs. positional
     * parameters) keeps the use-case interface stable as fields are added.
     */
    record IngestCommand(
            String title,
            DocumentType type,
            String sourceUri,
            String rawText) {}
}
