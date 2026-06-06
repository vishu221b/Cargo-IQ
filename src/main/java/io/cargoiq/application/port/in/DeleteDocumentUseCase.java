package io.cargoiq.application.port.in;

import java.util.UUID;

/**
 * Inbound port: remove a document from the corpus entirely.
 *
 * <p>A "delete" here is a two-store operation — the document aggregate lives in
 * the JPA {@code documents} table, while its embedded chunks live in the
 * pgvector {@code vector_store} table. Both must go, or the corpus is left with
 * orphaned vectors that still surface in retrieval. The use case owns that
 * invariant so neither inbound adapter (REST, MCP) has to remember it.
 *
 * @author Vishal Dogra
 */
public interface DeleteDocumentUseCase {

    /**
     * Delete the document and all of its indexed chunks.
     *
     * @param id the document id
     * @throws io.cargoiq.domain.exception.DocumentNotFoundException if no such document exists
     */
    void delete(UUID id);
}
