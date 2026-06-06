package io.cargoiq.application.port.in;

import io.cargoiq.domain.model.Document;
import io.cargoiq.domain.model.DocumentType;

/**
 * Inbound port: ingest a raw blob of text + metadata into the corpus.
 *
 * <p>Returns the persisted {@link Document} aggregate (with chunks and ID
 * assigned). Side effects (embedding, persistence) happen synchronously
 * inside the use case implementation — at production scale this would be
 * queued; keeping it synchronous here makes the flow legible and failures
 * easy to trace.
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
