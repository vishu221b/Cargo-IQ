package io.cargoiq.application.port.out;

import io.cargoiq.domain.model.DocumentChunk;
import io.cargoiq.domain.model.DocumentType;
import io.cargoiq.domain.model.ShipmentMetadata;

import java.util.List;
import java.util.UUID;

/**
 * Outbound port: turn raw text into chunks + extracted structured metadata.
 *
 * <p>One port, many adapters (text, PDF, CSV, ...). A
 * {@code CompositeDocumentParser} on the adapter side picks the right one
 * based on {@link DocumentType} or content-type sniffing.
 */
public interface DocumentParserPort {

    ParseResult parse(UUID documentId, DocumentType type, String rawText);

    record ParseResult(List<DocumentChunk> chunks, ShipmentMetadata metadata) {}
}
