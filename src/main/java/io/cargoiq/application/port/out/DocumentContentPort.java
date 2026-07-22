package io.cargoiq.application.port.out;

import java.util.UUID;

/**
 * Outbound port: read back the full text of a document by joining its indexed
 * chunks.
 *
 * <p>Chunk text is stored once, in the vector store — the JPA {@code documents}
 * table deliberately does not duplicate it. When a caller (the MCP
 * {@code cargo://documents/{id}} resource, say) needs the whole document text,
 * this port reassembles it from the chunk rows in sequence order.
 *
 * @author Vishal Dogra
 */
public interface DocumentContentPort {

    /**
     * The document's chunk text, concatenated in {@code chunkSequence} order.
     * Returns an empty string when the document has no indexed chunks.
     */
    String joinedText(UUID documentId);
}
